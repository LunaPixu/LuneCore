package net.lunapixu.lunecore.luckperms

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.{LiteralArgumentBuilder, RequiredArgumentBuilder}
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.{CommandSourceStack, Commands}
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import java.util.concurrent.TimeUnit
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.Component
import net.luckperms.api.model.user.UserManager
import net.luckperms.api.node.types.SuffixNode
import net.luckperms.api.LuckPermsProvider
import net.lunapixu.lunecore.{LuneCommandContext, LuneCore}
import net.lunapixu.lunecore.utils.{ArgUtils, StringUtils}
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import scala.util.Try

class SuffixCommand(plugin: LuneCore) extends ModifiesMeta("suffix"):
  def createCommand: LiteralArgumentBuilder[CommandSourceStack] = Commands
    .literal(s"set${getMetaType}")
    .requires { _.getSender.hasPermission(s"lunecore.set${getMetaType}") }.`then`(suffixSubCommand)
    .`then`(
      Commands.argument("player", ArgumentTypes.player).requires {
        _.getSender.hasPermission(s"lunecore.set${getMetaType}.other")
      }.`then`(suffixSubCommand)
    )

  private def suffixSubCommand: RequiredArgumentBuilder[CommandSourceStack, String] = Commands
    .argument(getMetaType, StringArgumentType.string).executes { ctx =>
      setSuffix(ctx, ArgUtils.tryGetPlayerArg(ctx))
    }

  private def setSuffix(ctx: CommandContext[CommandSourceStack], target: Option[Player]): Int =
    given config: FileConfiguration = plugin.getConfig()
    val context = LuneCommandContext(ctx)

    val commandOutput: Either[String, Option[MetaModifiedEvent]] =
      for
        targetPlayer <- target.fold(context.executorAsPlayer) { p => Some(p) }.filter { _.isOnline }
          .toRight("Target is not an online player.")
        suffixStr <- Option(ctx.getArgument(getMetaType, classOf[String])).filter { _.length > 0 }
          .map { s => guardFromFormatting(s, getMetaType, context) }
          .toRight(s"${getMetaType.capitalize} cannot be empty.")
      yield
        given userManager: UserManager = LuckPermsProvider.get.getUserManager
        Try(
          updateUserMeta(
            userManager.loadUser(targetPlayer.getUniqueId),
            suffixStr,
            SuffixNode.builder
          ).orTimeout(timeoutSecs, TimeUnit.SECONDS).get
        ).toOption

    commandOutput match
      case Left(error) =>
        context.sender.sendMessage(Component.text(error, NamedTextColor.RED))
        0
      case Right(eventOption) => eventOption.fold {
          val message = context.sender.sendMessage(Component.text(
            s"A major error occurred when trying to edit ${getMetaType}. Please contact an admin.",
            NamedTextColor.RED
          ))
          0
        } { event =>
          event.callEvent
          val message = makeUpdatedMetaComponent(event)
          context.sender.sendMessage(message)
          if context.senderAsPlayer.isDefined then
            plugin.getLogger.info(StringUtils.componentToPlainString(message))
          1
        }
