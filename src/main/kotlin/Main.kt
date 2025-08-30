package pamflet

import pamflet.tokenizer.Tokenizer

fun main() {
    val testString = testStrings[8]
    val tokenizer = Tokenizer(inputchars = testString);
    val tokens = tokenizer.tokenize()
    println(String.format("\ntokens::\n%s\n", tokens.joinToString(",\n")))
}