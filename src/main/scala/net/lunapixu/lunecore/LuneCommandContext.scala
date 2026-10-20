package net.lunapixu.lunecore

import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.{Entity, Player}
import net.lunapixu.lunecore.utils.StringUtils

class LuneCommandContext(ctx: CommandContext[CommandSourceStack]):
  val sender: CommandSender = ctx.getSource.getSender
  val senderAsPlayer: Option[Player] = sender match
    case player: Player => Some(player)
    case _              => None
  lazy val senderName: Component = getSenderName(sender)
  lazy val plainSenderName: String = getPlainName(senderName)

  val executor: Entity = ctx.getSource.getExecutor
  val executorAsPlayer: Option[Player] = executor match
    case player: Player => Some(player)
    case _              => None
  lazy val executorName: Component = getSenderName(executor)
  lazy val plainExecutorName: String = getPlainName(executorName)

  lazy val isSelfSent: Boolean = (executorAsPlayer, senderAsPlayer) match
    case (Some(executorPlayer), Some(senderPlayer)) => senderPlayer.getUniqueId ==
        executorPlayer.getUniqueId
    case _ => false

  protected def getSenderName(sender: CommandSender): Component = sender match
    case p: Player => p.displayName.hoverEvent(p.asHoverEvent)
        .clickEvent(ClickEvent.suggestCommand(s"/tell ${p.getName} "))
    case e: Entity => Option(e.customName).getOrElse(e.name).hoverEvent(e.asHoverEvent)
    case s: Any    => s.name

  protected def getPlainName(name: Component): String =
    val plainText = PlainTextComponentSerializer.plainText
    StringUtils.stripLegacyTokens(plainText.serialize(name))
