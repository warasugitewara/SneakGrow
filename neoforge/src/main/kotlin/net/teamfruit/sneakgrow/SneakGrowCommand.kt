package net.teamfruit.sneakgrow

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.neoforge.event.RegisterCommandsEvent

/**
 * `/sneakgrow` コマンド。
 *
 * 変更は即座に設定ファイル (`config/sneakgrow-server.toml`、ワールド個別に
 * 上書きしている場合は `world/serverconfig/sneakgrow-server.toml`) へ
 * 書き戻されるため、サーバーを再起動しても設定が残る。
 */
object SneakGrowCommand {
    /** OP レベル 2 以上を要求する。 */
    private const val PERMISSION_LEVEL = 2

    /** `filter addlook` で視線判定を行う距離 (ブロック)。 */
    private const val LOOK_DISTANCE = 16.0

    fun onRegisterCommands(event: RegisterCommandsEvent) {
        event.dispatcher.register(root())
    }

    private fun root(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal("sneakgrow")
            .requires { it.hasPermission(PERMISSION_LEVEL) }
            .executes { ctx -> showStatus(ctx.source) }
            .then(
                Commands.literal("reload").executes { ctx ->
                    Config.refresh()
                    ctx.source.sendSuccess({ label("設定を再読み込みしました") }, true)
                    1
                }
            )
            .then(setNode())
            .then(filterNode())

    // ------------------------------------------------------------------ set

    private fun setNode(): LiteralArgumentBuilder<CommandSourceStack> {
        val set = Commands.literal("set")

        for (setting in Config.intSettings) {
            set.then(
                settingNode(setting).then(
                    Commands.argument("value", IntegerArgumentType.integer(setting.min, setting.max))
                        .executes { ctx -> assign(ctx.source, setting, IntegerArgumentType.getInteger(ctx, "value")) }
                )
            )
        }
        for (setting in Config.doubleSettings) {
            set.then(
                settingNode(setting).then(
                    Commands.argument("value", DoubleArgumentType.doubleArg(setting.min, setting.max))
                        .executes { ctx -> assign(ctx.source, setting, DoubleArgumentType.getDouble(ctx, "value")) }
                )
            )
        }
        for (setting in Config.booleanSettings) {
            set.then(
                settingNode(setting).then(
                    Commands.argument("value", BoolArgumentType.bool())
                        .executes { ctx -> assign(ctx.source, setting, BoolArgumentType.getBool(ctx, "value")) }
                )
            )
        }
        return set
    }

    /** 値を省略した場合は現在値を表示するノード。 */
    private fun settingNode(setting: Setting<*>): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(setting.key).executes { ctx ->
            ctx.source.sendSuccess({ entry(setting.key, setting.get().toString()) }, false)
            1
        }

    private fun <T : Any> assign(source: CommandSourceStack, setting: Setting<T>, value: T): Int {
        setting.set(value)
        Config.save()
        source.sendSuccess({ entry(setting.key, value.toString()) }, true)
        return 1
    }

    // --------------------------------------------------------------- filter

    private fun filterNode(): LiteralArgumentBuilder<CommandSourceStack> {
        val filter = Commands.literal("filter")
            .executes { ctx -> showFilter(ctx.source) }
            .then(Commands.literal("list").executes { ctx -> showFilter(ctx.source) })
            .then(modeNode())
            .then(
                Commands.literal("add").then(
                    blockArgument().executes { ctx ->
                        editList(ctx.source, Config.filterBlocks, blockId(ctx), true, "ブロック")
                    }
                )
            )
            .then(
                Commands.literal("remove").then(
                    blockArgument().executes { ctx ->
                        editList(ctx.source, Config.filterBlocks, blockId(ctx), false, "ブロック")
                    }
                )
            )
            .then(Commands.literal("addlook").executes { ctx -> addLookedAtBlock(ctx.source) })
            .then(namespaceNode("addmod", true))
            .then(namespaceNode("removemod", false))
            .then(
                Commands.literal("clear").executes { ctx ->
                    Config.filterBlocks.set(mutableListOf())
                    Config.filterNamespaces.set(mutableListOf())
                    Config.save()
                    ctx.source.sendSuccess({ label("フィルタを空にしました") }, true)
                    1
                }
            )
        return filter
    }

    private fun modeNode(): LiteralArgumentBuilder<CommandSourceStack> {
        val mode = Commands.literal("mode")
        for (value in FilterMode.entries) {
            mode.then(
                Commands.literal(value.name.lowercase()).executes { ctx ->
                    Config.filterMode.set(value)
                    Config.save()
                    ctx.source.sendSuccess({ entry("mode", value.name) }, true)
                    1
                }
            )
        }
        return mode
    }

    private fun blockArgument(): RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> =
        Commands.argument("block", ResourceLocationArgument.id())
            .suggests { _, builder ->
                SharedSuggestionProvider.suggestResource(BuiltInRegistries.BLOCK.keySet(), builder)
            }

    /**
     * 未導入MODのブロックIDも指定できるよう、レジストリに存在するかは検証しない。
     * 設定を先に用意しておく運用ができるようにするための意図的な仕様。
     */
    private fun blockId(ctx: CommandContext<CommandSourceStack>): String =
        ResourceLocationArgument.getId(ctx, "block").toString()

    private fun namespaceNode(literal: String, add: Boolean): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(literal).then(
            Commands.argument("namespace", StringArgumentType.word())
                .suggests { _, builder ->
                    SharedSuggestionProvider.suggest(
                        BuiltInRegistries.BLOCK.keySet().mapTo(sortedSetOf<String>()) { it.namespace },
                        builder,
                    )
                }
                .executes { ctx ->
                    editList(
                        ctx.source,
                        Config.filterNamespaces,
                        StringArgumentType.getString(ctx, "namespace"),
                        add,
                        "名前空間",
                    )
                }
        )

    private fun addLookedAtBlock(source: CommandSourceStack): Int {
        val player = source.playerOrException
        val hit = player.pick(LOOK_DISTANCE, 0.0f, false)
        if (hit.type != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("ブロックを見ている状態で実行してください"))
            return 0
        }

        val pos = (hit as BlockHitResult).blockPos
        val block = player.level().getBlockState(pos).block
        // BLOCK は DefaultedRegistry なので、未登録でも既定キーが返り null にはならない
        val id = BuiltInRegistries.BLOCK.getKey(block)
        return editList(source, Config.filterBlocks, id.toString(), true, "ブロック")
    }

    private fun editList(
        source: CommandSourceStack,
        value: ModConfigSpec.ConfigValue<MutableList<out String>>,
        entry: String,
        add: Boolean,
        label: String,
    ): Int {
        val current = value.get().toMutableList()
        val changed = if (add) {
            if (entry in current) false else current.add(entry)
        } else {
            current.remove(entry)
        }

        if (!changed) {
            source.sendFailure(
                Component.literal(
                    if (add) "$label $entry は既に一覧にあります" else "$label $entry は一覧にありません"
                )
            )
            return 0
        }

        value.set(current)
        Config.save()
        source.sendSuccess(
            { label(if (add) "$label $entry を一覧に追加しました" else "$label $entry を一覧から削除しました") },
            true,
        )
        return 1
    }

    // --------------------------------------------------------------- output

    private fun showStatus(source: CommandSourceStack): Int {
        source.sendSuccess({ header("SneakGrow (tweaks)") }, false)
        for (setting in Config.allSettings) {
            source.sendSuccess({ entry(setting.key, setting.get().toString()) }, false)
        }
        return showFilter(source)
    }

    private fun showFilter(source: CommandSourceStack): Int {
        val settings = Config.settings
        source.sendSuccess({ header("SneakGrow (filter)") }, false)
        source.sendSuccess({ entry("mode", settings.filterMode.name) }, false)
        source.sendSuccess({ entry("blocks", joinOrEmpty(settings.filterBlocks.map(ResourceLocation::toString))) }, false)
        source.sendSuccess({ entry("namespaces", joinOrEmpty(settings.filterNamespaces)) }, false)
        return 1
    }

    private fun joinOrEmpty(values: Collection<String>): String =
        if (values.isEmpty()) "(なし)" else values.joinToString(", ")

    private fun header(text: String): Component =
        Component.literal("== $text ==").withStyle(ChatFormatting.GOLD)

    private fun label(text: String): Component =
        Component.literal(text).withStyle(ChatFormatting.GREEN)

    private fun entry(key: String, value: String): Component =
        Component.literal("$key = ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE))
}
