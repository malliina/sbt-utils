package com.malliina.sbt.filetree

object ScalaIdentifiers extends ScalaIdentifiers

trait ScalaIdentifiers {
  // TODO get a better reference
  val illegalChars = ".-åäö".toCharArray.toList
  // SLS 1.1
  val reservedWords: List[String] =
    ("abstract case catch class def do else extends " +
      "false final finally for forSome if implicit import lazy match new" +
      " null object override package private protected return sealed super" +
      " this throw trait try true type val var while with yield _ = # @").split(' ').toList

  def legalName(base: String): String = sanitize(replaced(base, illegalChars, '_'))

  def sanitize(word: String) =
    if (reservedWords contains word) s"`$word`"
    else word

  def camelCase(in: String): String = camelCase(in, List('-', '_'))

  def camelCase(in: String, triggers: List[Char]): String = {
    def toCamelCase(cs: List[Char]): List[Char] = cs match {
      case head :: next :: tail =>
        if (triggers contains head) next.toUpper :: toCamelCase(tail)
        else head :: toCamelCase(next :: tail)
      case other =>
        other
    }

    new String(toCamelCase(in.toList).toArray)
  }

  def replaced(in: String, illegal: List[Char], replacement: Char): String =
    illegal.foldLeft(in)((acc, illegal) => acc.replace(illegal, replacement))
}
