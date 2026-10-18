package net.lunapixu.lunecore.utils

import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import scala.util.Try

object ArgUtils:
  def tryGetPlayerArg(ctx: CommandContext[CommandSourceStack]): Option[Player] = Try(
    ctx.getArgument("player", classOf[PlayerSelectorArgumentResolver]).resolve(ctx.getSource).get(0)
  ).toOption
