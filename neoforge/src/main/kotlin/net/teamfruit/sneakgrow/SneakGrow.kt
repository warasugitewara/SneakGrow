package net.teamfruit.sneakgrow

import com.mojang.logging.LogUtils
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/**
 * SneakGrow (NeoForge / Minecraft 1.21.1) のエントリポイント。
 *
 * ブロック・アイテム・エンティティ・カスタムパケットを一切登録しないため、
 * サーバーにのみ導入すれば動作し、クライアント側への追加は不要。
 */
@Mod(SneakGrow.ID)
object SneakGrow {
    const val ID = "sneakgrow"

    val LOGGER: Logger = LogUtils.getLogger()

    init {
        ModLoadingContext.get().activeContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC)

        // Kotlin のメソッド参照は SAM 変換でジェネリクスが失われるため、
        // イベント型を明示する addListener(Class, Consumer) を使う。
        MOD_BUS.addListener(ModConfigEvent.Loading::class.java, Config::onLoad)
        MOD_BUS.addListener(ModConfigEvent.Reloading::class.java, Config::onReload)
        MOD_BUS.addListener(FMLCommonSetupEvent::class.java, ::onCommonSetup)

        FORGE_BUS.addListener(PlayerTickEvent.Post::class.java, SneakHandler::onPlayerTick)
        FORGE_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent::class.java, SneakHandler::onPlayerLoggedOut)
        FORGE_BUS.addListener(ServerStoppedEvent::class.java, SneakHandler::onServerStopped)
        FORGE_BUS.addListener(RegisterCommandsEvent::class.java, SneakGrowCommand::onRegisterCommands)
    }

    /** 全MODのロード後に、任意連携が使えるかを確認する。 */
    private fun onCommonSetup(@Suppress("UNUSED_PARAMETER") event: FMLCommonSetupEvent) {
        BotanyPotsIntegration.probe()
    }
}
