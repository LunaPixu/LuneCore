package net.lunapixu.lunecore

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.{CommandSourceStack, Commands}
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.Component
import net.lunapixu.lunecore.luckperms.{PrefixCommand, SuffixCommand}
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.plugin.java.JavaPlugin

class LuneCore extends JavaPlugin:
  override def onEnable(): Unit =
    saveDefaultConfig()

    getLifecycleManager.registerEventHandler(
      LifecycleEvents.COMMANDS,
      commands =>
        commands.registrar.register(baseCommand("lunecore").build)

        if getServer.getPluginManager.isPluginEnabled("LuckPerms") then
          commands.registrar.register(PrefixCommand(this).createCommand.build)
          commands.registrar.register(SuffixCommand(this).createCommand.build)
          getLogger.info("LuckPerms detected. LP functions enabled!")
    )

  private def baseCommand(commandName: String): LiteralArgumentBuilder[CommandSourceStack] =
    Commands.literal(commandName).`then`(reloadCommand("reload"))

  private def reloadCommand(commandName: String) = Commands.literal(commandName)
    .requires { source => source.getSender.hasPermission("lunecore.reload") }.executes { ctx =>
      reloadConfig()

      val reloadMessage = "LuneCore config reloaded!"

      if ctx.getSource.getSender.isInstanceOf[Player] then
        ctx.getSource.getSender.sendMessage(Component.text(reloadMessage, NamedTextColor.GOLD))
      getLogger.info(reloadMessage)

      1
    }
