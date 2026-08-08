package net.teamfruit.sneakgrow

import net.minecraft.resources.ResourceLocation
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.common.ModConfigSpec
import java.util.function.Predicate
import java.util.function.Supplier

/** フィルタの解釈方法。 */
enum class FilterMode {
    /** 一覧に載っているブロックだけを成長対象から外す。 */
    BLACKLIST,

    /** 一覧に載っているブロックだけを成長対象にする (空なら何も成長しない)。 */
    WHITELIST,
}

/**
 * 毎tick参照される設定のスナップショット。
 *
 * 設定の読み込み・再読み込み・コマンドによる変更のたびに丸ごと差し替えるため、
 * ゲームスレッドから読んでいる最中に個々の値が食い違うことがない。
 */
data class Settings(
    val cooldownTicks: Int,
    val growSaplings: Boolean,
    val growCrops: Boolean,
    val growMobs: Boolean,
    val mobInstantGrow: Boolean,
    val mobAgeStep: Int,
    val showParticles: Boolean,
    val blockRadius: Int,
    val blockHeight: Int,
    val mobRadius: Int,
    val blockPercentage: Double,
    val mobPercentage: Double,
    val maxBlocksPerTrigger: Int,
    val fireBonemealEvent: Boolean,
    val requireGrowerType: Boolean,
    val filterMode: FilterMode,
    val filterBlocks: Set<ResourceLocation>,
    val filterNamespaces: Set<String>,
) {
    /** [id] のブロックがフィルタを通過するか。 */
    fun isAllowed(id: ResourceLocation): Boolean {
        val listed = id in filterBlocks || id.namespace in filterNamespaces
        return when (filterMode) {
            FilterMode.BLACKLIST -> !listed
            FilterMode.WHITELIST -> listed
        }
    }
}

/** `/sneakgrow set` から扱える設定項目。定義と同時に登録され、コマンド側で列挙される。 */
sealed interface Setting<T : Any> {
    val key: String

    fun get(): T
    fun set(value: T)
}

class IntSetting(
    override val key: String,
    private val value: ModConfigSpec.IntValue,
    val min: Int,
    val max: Int,
) : Setting<Int> {
    override fun get(): Int = value.get()
    override fun set(value: Int) = this.value.set(value)
}

class DoubleSetting(
    override val key: String,
    private val value: ModConfigSpec.DoubleValue,
    val min: Double,
    val max: Double,
) : Setting<Double> {
    override fun get(): Double = value.get()
    override fun set(value: Double) = this.value.set(value)
}

class BooleanSetting(
    override val key: String,
    private val value: ModConfigSpec.BooleanValue,
) : Setting<Boolean> {
    override fun get(): Boolean = value.get()
    override fun set(value: Boolean) = this.value.set(value)
}

/**
 * `world/serverconfig/sneakgrow-server.toml` に対応する設定。
 *
 * 既定値は [Defaults] に集約し、スペック定義と [settings] の初期値で共有する。
 * 数値・真偽値の項目は [intSettings] / [doubleSettings] / [booleanSettings] にも
 * 登録され、`/sneakgrow set` はこの一覧からコマンドツリーを組み立てる。
 */
object Config {
    private object Defaults {
        const val COOLDOWN_TICKS = 5
        const val GROW_SAPLINGS = true
        const val GROW_CROPS = true
        const val GROW_MOBS = true
        const val MOB_INSTANT_GROW = true
        const val MOB_AGE_STEP = 1200
        const val SHOW_PARTICLES = true
        const val BLOCK_RADIUS = 5
        const val BLOCK_HEIGHT = 2
        const val MOB_RADIUS = 5
        const val BLOCK_PERCENTAGE = 0.1
        const val MOB_PERCENTAGE = 0.05
        const val MAX_BLOCKS_PER_TRIGGER = 1024
        const val FIRE_BONEMEAL_EVENT = true
        const val REQUIRE_GROWER_TYPE = true
        val FILTER_MODE = FilterMode.BLACKLIST
        val FILTER_BLOCKS = emptyList<String>()
        val FILTER_NAMESPACES = emptyList<String>()
    }

    private val cooldownTicks: ModConfigSpec.IntValue
    private val growSaplings: ModConfigSpec.BooleanValue
    private val growCrops: ModConfigSpec.BooleanValue
    private val growMobs: ModConfigSpec.BooleanValue
    private val mobInstantGrow: ModConfigSpec.BooleanValue
    private val mobAgeStep: ModConfigSpec.IntValue
    private val showParticles: ModConfigSpec.BooleanValue
    private val blockRadius: ModConfigSpec.IntValue
    private val blockHeight: ModConfigSpec.IntValue
    private val mobRadius: ModConfigSpec.IntValue
    private val blockPercentage: ModConfigSpec.DoubleValue
    private val mobPercentage: ModConfigSpec.DoubleValue
    private val maxBlocksPerTrigger: ModConfigSpec.IntValue
    private val fireBonemealEvent: ModConfigSpec.BooleanValue
    private val requireGrowerType: ModConfigSpec.BooleanValue

    val filterMode: ModConfigSpec.EnumValue<FilterMode>
    val filterBlocks: ModConfigSpec.ConfigValue<MutableList<out String>>
    val filterNamespaces: ModConfigSpec.ConfigValue<MutableList<out String>>

    val intSettings: List<IntSetting>
    val doubleSettings: List<DoubleSetting>
    val booleanSettings: List<BooleanSetting>

    /** 定義順に並んだ全項目。`/sneakgrow` の一覧表示に使う。 */
    val allSettings: List<Setting<*>>

    val SPEC: ModConfigSpec

    /** 現在有効な設定。設定ファイルが読み込まれるまでは既定値を返す。 */
    @Volatile
    var settings: Settings = Settings(
        cooldownTicks = Defaults.COOLDOWN_TICKS,
        growSaplings = Defaults.GROW_SAPLINGS,
        growCrops = Defaults.GROW_CROPS,
        growMobs = Defaults.GROW_MOBS,
        mobInstantGrow = Defaults.MOB_INSTANT_GROW,
        mobAgeStep = Defaults.MOB_AGE_STEP,
        showParticles = Defaults.SHOW_PARTICLES,
        blockRadius = Defaults.BLOCK_RADIUS,
        blockHeight = Defaults.BLOCK_HEIGHT,
        mobRadius = Defaults.MOB_RADIUS,
        blockPercentage = Defaults.BLOCK_PERCENTAGE,
        mobPercentage = Defaults.MOB_PERCENTAGE,
        maxBlocksPerTrigger = Defaults.MAX_BLOCKS_PER_TRIGGER,
        fireBonemealEvent = Defaults.FIRE_BONEMEAL_EVENT,
        requireGrowerType = Defaults.REQUIRE_GROWER_TYPE,
        filterMode = Defaults.FILTER_MODE,
        filterBlocks = emptySet(),
        filterNamespaces = emptySet(),
    )
        private set

    init {
        val builder = ModConfigSpec.Builder()
        val ints = mutableListOf<IntSetting>()
        val doubles = mutableListOf<DoubleSetting>()
        val booleans = mutableListOf<BooleanSetting>()
        val all = mutableListOf<Setting<*>>()

        fun int(key: String, default: Int, min: Int, max: Int, vararg comment: String): ModConfigSpec.IntValue {
            val value = builder.comment(*comment).defineInRange(key, default, min, max)
            IntSetting(key, value, min, max).also { ints += it; all += it }
            return value
        }

        fun double(
            key: String,
            default: Double,
            min: Double,
            max: Double,
            vararg comment: String,
        ): ModConfigSpec.DoubleValue {
            val value = builder.comment(*comment).defineInRange(key, default, min, max)
            DoubleSetting(key, value, min, max).also { doubles += it; all += it }
            return value
        }

        fun boolean(key: String, default: Boolean, vararg comment: String): ModConfigSpec.BooleanValue {
            val value = builder.comment(*comment).define(key, default)
            BooleanSetting(key, value).also { booleans += it; all += it }
            return value
        }

        builder.comment("成長処理の挙動").push("tweaks")

        cooldownTicks = int(
            "cooldownTicks", Defaults.COOLDOWN_TICKS, 0, 20 * 60,
            "成長判定を行う最短間隔 (tick)",
        )
        growSaplings = boolean(
            "growSaplings", Defaults.GROW_SAPLINGS,
            "スニーク時に苗木 (SaplingBlock) を成長させるか",
        )
        growCrops = boolean(
            "growCrops", Defaults.GROW_CROPS,
            "スニーク時に苗木以外の骨粉対象ブロック (農作物など) を成長させるか",
        )
        growMobs = boolean(
            "growMobs", Defaults.GROW_MOBS,
            "スニーク時に子供Mob・繁殖クールダウン中のMobを成長させるか",
        )
        mobInstantGrow = boolean(
            "mobInstantGrow", Defaults.MOB_INSTANT_GROW,
            "true: 対象になったMobを即座に成体化し、繁殖クールダウンも解除する (Paper版と同じ挙動)",
            "false: mobAgeStep tick 分だけ成体・繁殖可能に近づける",
        )
        mobAgeStep = int(
            "mobAgeStep", Defaults.MOB_AGE_STEP, 1, 24000,
            "mobInstantGrow=false のときに1回で進める成長量 (tick)",
        )
        showParticles = boolean(
            "showParticles", Defaults.SHOW_PARTICLES,
            "成長時にパーティクルを表示するか",
            "サーバーから送出するためクライアント側の導入は不要",
        )
        blockRadius = int(
            "blockRadius", Defaults.BLOCK_RADIUS, 0, 16,
            "ブロックを判定する水平方向の範囲 (ブロック)",
        )
        blockHeight = int(
            "blockHeight", Defaults.BLOCK_HEIGHT, 0, 16,
            "ブロックを判定する垂直方向の範囲 (ブロック)",
        )
        mobRadius = int(
            "mobRadius", Defaults.MOB_RADIUS, 0, 32,
            "Mobを判定する範囲 (ブロック)",
        )
        blockPercentage = double(
            "blockPercentage", Defaults.BLOCK_PERCENTAGE, 0.0, 1.0,
            "ブロック1つあたりの成長確率",
        )
        mobPercentage = double(
            "mobPercentage", Defaults.MOB_PERCENTAGE, 0.0, 1.0,
            "Mob1体あたりの成長確率",
        )
        maxBlocksPerTrigger = int(
            "maxBlocksPerTrigger", Defaults.MAX_BLOCKS_PER_TRIGGER, 1, 8192,
            "1回のスニークで骨粉を適用する最大ブロック数 (負荷の上限)",
        )
        fireBonemealEvent = boolean(
            "fireBonemealEvent", Defaults.FIRE_BONEMEAL_EVENT,
            "骨粉適用前に NeoForge の BonemealEvent を発火するか",
            "true にすると土地保護MOD (FTB Chunks 等) が成長を拒否できる",
        )
        requireGrowerType = boolean(
            "requireGrowerType", Defaults.REQUIRE_GROWER_TYPE,
            "BonemealableBlock.Type が GROWER のブロックだけを対象にするか",
            "true にすると草ブロックのような NEIGHBOR_SPREADER (周囲に花や草を生やす類) を除外する",
        )

        builder.pop()

        builder.comment("成長対象ブロックの絞り込み (/sneakgrow filter で編集可能)").push("filter")

        filterMode = builder
            .comment(
                "BLACKLIST: 一覧のブロックを除外する",
                "WHITELIST: 一覧のブロックだけを対象にする (空の場合は何も成長しない)",
            )
            .defineEnum("mode", Defaults.FILTER_MODE)
        filterBlocks = builder
            .comment("対象のブロックID (例: \"minecraft:wheat\")")
            .defineListAllowEmpty(
                "blocks",
                Defaults.FILTER_BLOCKS,
                Supplier { "minecraft:wheat" },
                Predicate { it is String && ResourceLocation.tryParse(it) != null },
            )
        filterNamespaces = builder
            .comment("MOD単位で指定する名前空間 (例: \"mysticalagriculture\")")
            .defineListAllowEmpty(
                "namespaces",
                Defaults.FILTER_NAMESPACES,
                Supplier { "minecraft" },
                Predicate { it is String && it.isNotEmpty() },
            )

        builder.pop()

        SPEC = builder.build()
        intSettings = ints
        doubleSettings = doubles
        booleanSettings = booleans
        allSettings = all
    }

    /** 設定ファイルの内容を [settings] に取り込む。 */
    fun refresh() {
        if (!SPEC.isLoaded) return
        settings = Settings(
            cooldownTicks = cooldownTicks.get(),
            growSaplings = growSaplings.get(),
            growCrops = growCrops.get(),
            growMobs = growMobs.get(),
            mobInstantGrow = mobInstantGrow.get(),
            mobAgeStep = mobAgeStep.get(),
            showParticles = showParticles.get(),
            blockRadius = blockRadius.get(),
            blockHeight = blockHeight.get(),
            mobRadius = mobRadius.get(),
            blockPercentage = blockPercentage.get(),
            mobPercentage = mobPercentage.get(),
            maxBlocksPerTrigger = maxBlocksPerTrigger.get(),
            fireBonemealEvent = fireBonemealEvent.get(),
            requireGrowerType = requireGrowerType.get(),
            filterMode = filterMode.get(),
            filterBlocks = filterBlocks.get().mapNotNullTo(LinkedHashSet()) { ResourceLocation.tryParse(it) },
            filterNamespaces = filterNamespaces.get().toCollection(LinkedHashSet()),
        )
    }

    /** コマンドによる変更をファイルへ書き戻し、[settings] を更新する。 */
    fun save() {
        if (SPEC.isLoaded) {
            SPEC.save()
        }
        refresh()
    }

    fun onLoad(@Suppress("UNUSED_PARAMETER") event: ModConfigEvent.Loading) = refresh()

    fun onReload(@Suppress("UNUSED_PARAMETER") event: ModConfigEvent.Reloading) = refresh()
}
