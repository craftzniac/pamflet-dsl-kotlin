package pamflet

import pamflet.tokenizer.Tokenizer

fun main() {
    val testString = testStrings[7]
    val tokenizer = Tokenizer(inputchars = testString);
    val tokens = tokenizer.tokenize()
    println(String.format("\ninputchars::\n%s\n", testString))
    println(String.format("\ntokens::\n%s\n", tokens.joinToString(",\n")))
}