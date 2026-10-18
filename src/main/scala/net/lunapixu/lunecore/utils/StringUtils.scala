package net.lunapixu.lunecore.utils

import java.util.regex.Pattern
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.Component

object StringUtils:
  def stripTokens(string: String): String =
    val tokenRegex = "[&§][\\da-fk-or]" // hehe da fkor :)
    val stripPattern = Pattern.compile(tokenRegex, Pattern.CASE_INSENSITIVE)
    stripPattern.matcher(string).replaceAll("")

  def componentToPlainString(component: Component): String =
    stripTokens(PlainTextComponentSerializer.plainText.serialize(component))
