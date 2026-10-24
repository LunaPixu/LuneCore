package net.lunapixu.lunecore.utils

import java.util.regex.{MatchResult, Pattern}
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.Component
import org.bukkit.plugin.Plugin
import org.bukkit.Bukkit

object StringUtils:
  extension [T](value: T)
    private def mapIf(b: Boolean)(f: T => T): T = if b then f(value) else value
    private def mapUnless(b: Boolean)(f: T => T) = mapIf(!b)(f)

  def componentToPlainString(component: Component): String =
    stripLegacyTokens(PlainTextComponentSerializer.plainText.serialize(component))

  private val supportedChatPluginNames = "Chatty" :: "EssentialsChat" :: Nil

  private def applyFormatByPluginName(
    acc: (applied: Boolean, string: String),
    name: String,
    plugins: Array[Plugin]
  ): (applied: Boolean, string: String) = (acc.applied, name) match
    case (true, _)              => acc
    case (_, chatty @ "Chatty") =>
      acc.mapIf(plugins.exists(_.getName == chatty)) { a => (true, chattyToMiniMessage(a.string)) }
    case (_, essentials @ "EssentialsChat") => acc.mapIf(plugins.exists(_.getName == essentials)) {
        a => (true, extendedLegacyToMiniMessage(a.string))
      }
    case _ => acc

  def stringToMiniMessage(string: String): String =
    val enabledChatPlugins = Bukkit.getPluginManager.getPlugins.filter { p =>
      supportedChatPluginNames.contains(p.getName) && p.isEnabled
    }

    val results = supportedChatPluginNames
      .foldLeft[(applied: Boolean, string: String)]((false, string)) { (acc, name) =>
        applyFormatByPluginName(acc, name, enabledChatPlugins)
      }
    results.string.mapUnless(results.applied) { _ => legacyToMiniMessage(string) }

  private def escapeFormatByPluginName(
    acc: (applied: Boolean, string: String),
    name: String,
    plugins: Array[Plugin]
  ): (applied: Boolean, string: String) = (acc.applied, name) match
    case (true, _)              => acc
    case (_, chatty @ "Chatty") =>
      acc.mapIf(plugins.exists(_.getName == chatty)) { a => (true, escapeChattyFormat(a.string)) }
    case (_, essentials @ "EssentialsChat") => acc.mapIf(plugins.exists(_.getName == essentials)) {
        a => (true, escapeExtendedLegacyTokens(a.string))
      }
    case _ => acc

  def escapeExtraFormats(string: String): String =
    val enabledChatPlugins = Bukkit.getPluginManager.getPlugins.filter { p =>
      supportedChatPluginNames.contains(p.getName) && p.isEnabled
    }

    val results = supportedChatPluginNames
      .foldLeft[(applied: Boolean, string: String)]((false, string)) { (acc, name) =>
        escapeFormatByPluginName(acc, name, enabledChatPlugins)
      }
    results.string.mapUnless(results.applied) { _ => escapeLegacyTokens(string) }

  def escapeToken(token: String): String = s"${token.apply(0)}\\\\${token.drop(1)}"

  private val basicColorPattern = Pattern.compile("\\{(#[\\da-f]{6})\\}", Pattern.CASE_INSENSITIVE)
  private val simpleGradientPattern = Pattern
    .compile("\\{((?:#[\\da-f]{6})(?::#[\\da-f]{6})+)\\}", Pattern.CASE_INSENSITIVE)
  private val encloseGradientPattern = Pattern
    .compile("\\{((?:#[\\da-f]{6})(?::#[\\da-f]{6})+) (.+?)\\}", Pattern.CASE_INSENSITIVE)

  def escapeChattyFormat(string: String): String =
    val basicEscaped = basicColorPattern.matcher(string).replaceAll { r => escapeToken(r.group) }
    val simpleGradientEscaped = simpleGradientPattern.matcher(basicEscaped).replaceAll { r =>
      escapeToken(r.group)
    }
    val enclosedGradientEscaped = encloseGradientPattern.matcher(simpleGradientEscaped)
      .replaceAll { r => escapeToken(r.group) }

    escapeExtendedLegacyTokens(enclosedGradientEscaped)

  def chattyToMiniMessage(string: String): String =
    val basicApplied = basicColorPattern.matcher(string).replaceAll("<$1>")
    val simpleGradientApplied = simpleGradientPattern.matcher(basicApplied)
      .replaceAll("<gradient:$1>")
    val fullGradientApplied = encloseGradientPattern.matcher(simpleGradientApplied)
      .replaceAll("<gradient:$1>$2</gradient>")

    extendedLegacyToMiniMessage(fullGradientApplied)

  private val rgbPattern = Pattern.compile("[&§](#[\\da-f]{6})", Pattern.CASE_INSENSITIVE)
  private def xPattern(formatChar: Char) = Pattern
    .compile(String.format("%1$cx((?:%1$c[\\da-f]){6})", formatChar), Pattern.CASE_INSENSITIVE)

  def extendedLegacyToMiniMessage(string: String): String =
    val rgbApplied = rgbPattern.matcher(string).replaceAll("<$1>")
    val xApplied = ('&' :: '§' :: Nil).foldLeft(rgbApplied) { (str, char) =>
      xPattern(char).matcher(str).replaceAll { result =>
        s"<#${result.group(1).replace(char.toString, "")}>"
      }
    }
    legacyToMiniMessage(xApplied)

  def escapeExtendedLegacyTokens(string: String): String =
    val rgbEscaped = rgbPattern.matcher(string).replaceAll { r => escapeToken(r.group) }
    val xEscaped = ('&' :: '§' :: Nil).foldLeft(rgbEscaped) { (str, char) =>
      xPattern(char).matcher(str).replaceAll { r => escapeToken(r.group) }
    }
    escapeLegacyTokens(xEscaped)

  private val legacyPattern = Pattern.compile("[&§]([\\da-fk-or])", Pattern.CASE_INSENSITIVE) // hehe da fkor :)
  def stripLegacyTokens(string: String): String = legacyPattern.matcher(string).replaceAll("")

  def escapeLegacyTokens(string: String): String = legacyPattern.matcher(string).replaceAll { r =>
    escapeToken(r.group)
  }

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

  def legacyToMiniMessage(string: String): String = legacyPattern.matcher(string).replaceAll {
    result => legacyMiniMessageMap.get(result.group(1)).fold(result.group(0)) { tag => s"<${tag}>" }
  }
