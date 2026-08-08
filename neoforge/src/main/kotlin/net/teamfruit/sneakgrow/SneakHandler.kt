package net.teamfruit.sneakgrow

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.SaplingBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.BonemealEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import java.util.UUID

/**
 * スニークを検知して周囲の成長を進める本体。
 *
 * Paper 版は `PlayerToggleSneakEvent` を起点にしていたが、NeoForge に同等のイベントが
 * ないため [PlayerTickEvent.Post] でスニーク状態をポーリングし、状態の変化をトグルとして扱う。
 * クールダウンとトグル消費の構造は Paper 版と同じなので、
 * 「スニークの切り替え1回につき最大1回、クールダウン間隔で成長が起きる」挙動が保たれる。
 */
object SneakHandler {
    private class PlayerState {
        var isSneaking = false
        var isSneaked = false
        var ticksLastCheck = 0L
    }

    private val states = HashMap<UUID, PlayerState>()

    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity as? ServerPlayer ?: return
        onAction(player)
    }

    fun onPlayerLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        states.remove(event.entity.uuid)
    }

    fun onServerStopped(@Suppress("UNUSED_PARAMETER") event: ServerStoppedEvent) {
        states.clear()
    }

    private fun onAction(player: ServerPlayer) {
        val level = player.level() as? ServerLevel ?: return
        val settings = Config.settings
        val state = states.getOrPut(player.uuid) { PlayerState() }

        val ticksNow = level.gameTime
        if (ticksNow - state.ticksLastCheck >= settings.cooldownTicks) {
            state.ticksLastCheck = ticksNow

            if (state.isSneaked) {
                grow(player, level, settings)
                state.isSneaked = false
            }
        }

        val isSneaking = player.isShiftKeyDown
        if (state.isSneaking != isSneaking) {
            state.isSneaking = isSneaking
            state.isSneaked = true
        }
    }

    private fun grow(player: ServerPlayer, level: ServerLevel, settings: Settings) {
        if (settings.growSaplings || settings.growCrops) {
            growBlocks(player, level, settings)
        }
        if (settings.growMobs) {
            growMobs(player, level, settings)
        }
    }

    private fun growBlocks(player: ServerPlayer, level: ServerLevel, settings: Settings) {
        if (settings.blockPercentage <= 0.0) return

        val random = level.random
        val origin = player.blockPosition()
        val cursor = BlockPos.MutableBlockPos()
        var grown = 0

        for (dx in -settings.blockRadius..settings.blockRadius) {
            for (dy in -settings.blockHeight..settings.blockHeight) {
                for (dz in -settings.blockRadius..settings.blockRadius) {
                    // Paper 版は候補ブロックを列挙してから確率を引いていたが、抽選と
                    // 判定は互いに独立なので順序を入れ替えても分布は変わらない。
                    // 先に引くことで大半の座標で BlockState の取得自体を省ける。
                    if (random.nextFloat() >= settings.blockPercentage) continue

                    cursor.set(origin.x + dx, origin.y + dy, origin.z + dz)
                    // 未ロードチャンクへ触れてチャンク生成を誘発しないようにする
                    if (!level.isLoaded(cursor)) continue

                    if (!tryGrowBlock(player, level, cursor.immutable(), level.getBlockState(cursor), settings)) {
                        continue
                    }

                    if (++grown >= settings.maxBlocksPerTrigger) return
                }
            }
        }
    }

    /** @return 骨粉が適用された (あるいは他MODが適用済みとして処理した) 場合 true */
    private fun tryGrowBlock(
        player: ServerPlayer,
        level: ServerLevel,
        pos: BlockPos,
        blockState: BlockState,
        settings: Settings,
    ): Boolean {
        val block = blockState.block
        val bonemealable = block as? BonemealableBlock ?: return false

        val isSapling = block is SaplingBlock
        if (isSapling && !settings.growSaplings) return false
        if (!isSapling && !settings.growCrops) return false

        // 草ブロックのように「周囲に生やす」タイプを除外する
        if (settings.requireGrowerType && bonemealable.type != BonemealableBlock.Type.GROWER) return false

        // BLOCK は DefaultedRegistry なので、未登録でも既定キーが返り null にはならない
        if (!settings.isAllowed(BuiltInRegistries.BLOCK.getKey(block))) return false

        if (settings.fireBonemealEvent) {
            val event = BonemealEvent(player, level, pos, blockState, ItemStack.EMPTY)
            NeoForge.EVENT_BUS.post(event)
            if (event.isCanceled) return event.isSuccessful
        }

        // Paper 版の Block#applyBoneMeal と同じ手順
        if (!bonemealable.isValidBonemealTarget(level, pos, blockState)) return false
        if (!bonemealable.isBonemealSuccess(level, level.random, pos, blockState)) return false
        bonemealable.performBonemeal(level, level.random, pos, blockState)

        if (settings.showParticles) {
            level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5,
                5,
                0.3,
                0.3,
                0.3,
                0.0,
            )
        }
        return true
    }

    private fun growMobs(player: ServerPlayer, level: ServerLevel, settings: Settings) {
        if (settings.mobPercentage <= 0.0) return

        val diameter = settings.mobRadius.toDouble() * 2.0
        val area = AABB.ofSize(player.position(), diameter, diameter, diameter)
        // Paper 版の「!isAdult() || !canBreed()」は CraftBukkit 実装上「age != 0」と等価
        val mobs = level.getEntitiesOfClass(AgeableMob::class.java, area) { it.age != 0 }

        for (mob in mobs) {
            if (level.random.nextFloat() >= settings.mobPercentage) continue

            val age = mob.age
            mob.age = when {
                // CraftBukkit の setBreed(true) は setAge(0) なので、
                // Paper 版は成功時に即座に成体化 + 繁殖可能化していた
                settings.mobInstantGrow -> 0
                age < 0 -> minOf(0, age + settings.mobAgeStep)
                else -> maxOf(0, age - settings.mobAgeStep)
            }

            if (settings.showParticles) {
                level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    mob.x,
                    mob.y + mob.bbHeight * 0.5,
                    mob.z,
                    5,
                    0.2,
                    0.2,
                    0.2,
                    0.0,
                )
            }
        }
    }
}
