package pamflet

import pamflet.parser.Parser

fun main() {
    val testString = testStrings[3]
    val parser = Parser(inputchars = testString);
    val tokens = parser.tokens
    println(String.format("\ntokens::\n%s\n", tokens.joinToString(", \n")))
    val elements = parser.parse()
    println(String.format("\nelements::\n%s\n", elements.joinToString(", \n")))
}