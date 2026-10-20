package net.lunapixu.lunecore.utils

import java.util.regex.{MatchResult, Pattern}
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

object StringUtils:
  private val legacyTokenRegex = "[&§]([\\da-fk-or])" // hehe da fkor :)
  val legacyMiniMessageMap = Map[String, String](
    "0" -> "black",
    "1" -> "dark_blue",
    "2" -> "dark_green",
    "3" -> "dark_aqua",
    "4" -> "dark_red",
    "5" -> "dark_purple",
    "6" -> "gold",
    "7" -> "gray",
    "8" -> "dark_gray",
    "9" -> "blue",
    "a" -> "green",
    "b" -> "aqua",
    "c" -> "red",
    "d" -> "light_purple",
    "e" -> "yellow",
    "f" -> "white",
    "k" -> "obf",
    "l" -> "b",
    "m" -> "st",
    "n" -> "u",
    "o" -> "i",
    "r" -> "reset"
  )

  def stripLegacyTokens(string: String): String =
    val legacyPattern = Pattern.compile(legacyTokenRegex, Pattern.CASE_INSENSITIVE)
    legacyPattern.matcher(string).replaceAll("")

  def componentToPlainString(component: Component): String =
    stripLegacyTokens(PlainTextComponentSerializer.plainText.serialize(component))

  private val supportedChatPluginNames = "Chatty" :: "EssentialsChat" :: Nil

  def stringToMiniMessage(string: String): String =
    val enabledChatPlugins = Bukkit.getPluginManager.getPlugins.filter { p =>
      supportedChatPluginNames.contains(p.getName) && p.isEnabled
    }
    // This is ugly as sin.
    val results = supportedChatPluginNames
      .foldLeft[(applied: Boolean, string: String)]((false, string)) { (acc, name) =>
        (acc.applied, name) match
          case (true, _)     => acc
          case (_, "Chatty") =>
            if enabledChatPlugins.exists(p => p.getName == name) then
              (true, chattyToMiniMessage(acc.string))
            else acc
          case (_, "EssentialsChat") =>
            if enabledChatPlugins.exists(p => p.getName == name) then
              (true, extendedLegacyToMiniMessage(acc.string))
            else acc
          case _ => acc
      }

    results.applied match
      case true  => results.string
      case false => legacyToMiniMessage(string)

  // TODO: Implement this!
  def extendedLegacyToMiniMessage(string: String): String =
    legacyToMiniMessage(string)

  // TODO: Implement this!
  def chattyToMiniMessage(string: String): String =
    legacyToMiniMessage(extendedLegacyToMiniMessage(string))

  def legacyToMiniMessage(string: String): String =
    val legacyPattern = Pattern.compile(legacyTokenRegex, Pattern.CASE_INSENSITIVE)
    legacyPattern.matcher(string).replaceAll { result =>
      legacyMiniMessageMap.get(result.group(1)).fold(result.group(0)) { str => s"<${str}>" }
    }
