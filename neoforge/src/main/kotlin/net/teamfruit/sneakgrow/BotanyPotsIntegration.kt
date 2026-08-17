package net.teamfruit.sneakgrow

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.neoforged.fml.ModList
import java.lang.reflect.Method

/**
 * Botany Pots のポットを成長させるための連携。
 *
 * Botany Pots のポットは [net.minecraft.world.level.block.BonemealableBlock] ではなく、
 * BlockEntity 側で成長 tick を管理している。さらに NeoForge の `BonemealEvent` も購読していないため、
 * 通常の骨粉処理では一切反応しない。
 *
 * ここでは Botany Pots 本体の `BasicFertilizer#apply` と同じ計算を、その公開 API 経由で再現する。
 * ただし本家は「手に持っている骨粉を 1 個消費する」実装なので、その部分だけは行わない。
 *
 * ビルド時に Botany Pots へ依存しないよう、呼び出しはリフレクションで行う。
 * ATM10 が同梱するバージョンは変動しうるため、[probe] でメソッドの存在を先に確認し、
 * 1 つでも欠けていれば警告を出して連携ごと無効化する (成長処理そのものは止めない)。
 */
object BotanyPotsIntegration {
    const val MOD_ID = "botanypots"

    private const val POT_BLOCK_ENTITY_CLASS =
        "net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity"

    /**
     * 本家 `BasicFertilizer#apply` と同じく、収穫までの最後の 20 tick は骨粉で埋められない。
     * 「骨粉だけでは収穫状態にできない」という Botany Pots の設計を維持する。
     */
    private const val RESERVED_TICKS = 20

    /** ワックスがけのポットは本家でもインタラクトを受け付けない。 */
    private const val WAXED_POT_TYPE = "WAXED"

    /** 解決済みのリフレクション情報。全て揃ったときだけ生成される。 */
    private class Api(
        val potClass: Class<*>,
        val getPotType: Method,
        val canBonemeal: Method,
        val getRequiredGrowthTicks: Method,
        val growthTime: Method,
        val updateGrowthTime: Method,
        val setBonemealCooldown: Method,
        val markUpdated: Method,
    )

    @Volatile
    private var api: Api? = null

    /** 連携が有効か。Botany Pots 未導入、または API が想定と異なる場合は false。 */
    val isAvailable: Boolean
        get() = api != null

    /**
     * Botany Pots の API を解決する。サーバー起動時に 1 度だけ呼ぶ。
     */
    fun probe() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            SneakGrow.LOGGER.info("Botany Pots が見つからないため、連携を無効のままにします")
            return
        }

        api = try {
            val potClass = Class.forName(POT_BLOCK_ENTITY_CLASS)
            Api(
                potClass = potClass,
                getPotType = potClass.getMethod("getPotType"),
                canBonemeal = potClass.getMethod("canBonemeal"),
                getRequiredGrowthTicks = potClass.getMethod("getRequiredGrowthTicks"),
                growthTime = potClass.getMethod("growthTime"),
                updateGrowthTime = potClass.getMethod("updateGrowthTime", Float::class.javaPrimitiveType),
                setBonemealCooldown = potClass.getMethod("setBonemealCooldown", Int::class.javaPrimitiveType),
                markUpdated = potClass.getMethod("markUpdated"),
            )
        } catch (e: ReflectiveOperationException) {
            warnUnavailable(e)
            null
        } catch (e: LinkageError) {
            warnUnavailable(e)
            null
        }

        if (api != null) {
            SneakGrow.LOGGER.info("Botany Pots を検出したため、ポットの成長に対応します")
        }
    }

    private fun warnUnavailable(cause: Throwable) {
        SneakGrow.LOGGER.warn(
            "Botany Pots は導入されていますが API が想定と異なるため、連携を無効化します: {}",
            cause.toString(),
        )
    }

    /**
     * [pos] のブロックが Botany Pots のポットであれば成長を進める。
     *
     * @return 成長させた場合のみ true
     */
    fun tryGrow(level: ServerLevel, pos: BlockPos, settings: Settings): Boolean {
        val api = this.api ?: return false
        val pot = level.getBlockEntity(pos) ?: return false
        if (!api.potClass.isInstance(pot)) return false

        return try {
            val potType = api.getPotType.invoke(pot)
            if (potType is Enum<*> && potType.name == WAXED_POT_TYPE) return false

            if (api.canBonemeal.invoke(pot) != true) return false

            val requiredTicks = api.getRequiredGrowthTicks.invoke(pot) as Int
            // 作物が入っていないポットは -1 を返す
            if (requiredTicks <= RESERVED_TICKS) return false

            val maxTicks = (requiredTicks - RESERVED_TICKS).toFloat()
            val currentTicks = api.growthTime.invoke(pot) as Float
            if (currentTicks >= maxTicks) return false

            val minGrowth = settings.botanyPotsMinGrowthTicks
            val maxGrowth = maxOf(minGrowth, settings.botanyPotsMaxGrowthTicks)
            val growth = minGrowth + level.random.nextInt(maxGrowth - minGrowth + 1)

            api.updateGrowthTime.invoke(pot, minOf(currentTicks + growth, maxTicks))
            api.setBonemealCooldown.invoke(pot, settings.botanyPotsCooldownTicks)
            api.markUpdated.invoke(pot)
            true
        } catch (e: ReflectiveOperationException) {
            disableAfterFailure(e)
            false
        } catch (e: LinkageError) {
            disableAfterFailure(e)
            false
        }
    }

    private fun disableAfterFailure(cause: Throwable) {
        api = null
        SneakGrow.LOGGER.error("Botany Pots の成長処理に失敗したため、連携を無効化します", cause)
    }
}
