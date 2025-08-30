package pamflet.tokenizer

data class Token(
    val type: TokenType,
    var value: String = ""
)

enum class TokenType {
    Text,
    Null,
    NewLine,
    PropertyName,
    Equals,
    PropertyValue,
    Comment
}

enum class TokenizerState {
    Data,
    StartOfLine,
    Text,
    PropertyName,
    PropertyValue,
    Comment
}

val nullToken = Token(type = TokenType.Null)

val keywords = listOf("Aud", "Lnk", "Img");

class Tokenizer(val inputchars: String) {
    var state: TokenizerState = TokenizerState.StartOfLine
    var cursor: Int = 0
    var reconsume = false
    var currToken = nullToken
    val tokens: MutableList<Token> = mutableListOf()

    fun tokenize(): List<Token> {
        println(String.format("\ninputchars::\n%s", this.inputchars))
        while (cursor < inputchars.length) {
            when (this.state) {
                TokenizerState.StartOfLine -> {
                    // read consumeNextChar
                    val currChar = this.consumeNextChar()
                    when (currChar) {
                        '\\' -> {  // start of escape sequence
                            handleEscapeSequence(currChar)
                        }

                        in firstCharOfKeywords() -> {   // is start of a keyword

                        }

                        '.' -> {  // possibly the start of property name
                            // peek foward to see if nextchar is a letter
                            val nextChar = this.peekNextChar()
                            when (nextChar) {
                                in 'a'..'z', in 'A'..'Z' -> {
                                    this.currToken = Token(type = TokenType.PropertyName)
                                    this.switchState(TokenizerState.PropertyName)
                                }

                                else -> {
                                    this.currToken = Token(type = TokenType.Text, ".")
                                    this.switchState(TokenizerState.Text)
                                }
                            }
                        }

                        '-' -> {  // start of a list item

                        }

                        '/' -> {   //start of a comment
                            // check if the next char is a /
                            val nextChar = peekNextChar()
                            if (nextChar == '/') {
                                this.consumeForwardBy(1) // advance the cursor my 1 index forward
                                this.currToken = Token(type = TokenType.Comment)
                                this.switchState(TokenizerState.Comment)
                            } else {
                                this.reconsume = true
                                // create empty text token
                                this.currToken = Token(type = TokenType.Text)
                                this.switchState(TokenizerState.Text);
                            }
                        }

                        '[' -> {   // start of a code bloc
                        }

                        else -> {   // start of regular text
                            this.reconsume = true
                            // create empty text token
                            this.currToken = Token(type = TokenType.Text)
                            this.switchState(TokenizerState.Text);
                        }
                    }
                }

                TokenizerState.Data -> {
                    todo()
                }

                TokenizerState.Comment -> {
                    when (val currChar = this.consumeNextChar()) {
                        '\n' -> {
                            this.flushCurrToken()
                            this.switchState(TokenizerState.StartOfLine)
                        }

                        '\\' -> {
                            handleEscapeSequence(currChar)
                        }

                        else -> {
                            this.currToken.value += currChar
                        }

                    }
                }

                TokenizerState.PropertyName -> {
                    when (val currChar = this.consumeNextChar()) {
                        '=' -> {
                            this.flushCurrToken()
                            this.currToken = Token(type = TokenType.Equals)
                            this.flushCurrToken()
                            this.currToken = Token(type = TokenType.PropertyValue)
                            this.switchState(TokenizerState.PropertyValue)
                        }

                        '\n' -> {
                            this.flushCurrToken()
                            this.switchState(TokenizerState.StartOfLine)
                        }

                        '\\' -> {  // possible \n character
                            handleEscapeSequence(currChar)
                        }

                        else -> {
                            this.currToken.value += currChar
                        }
                    }
                }

                TokenizerState.PropertyValue -> {
                    when (val currChar = this.consumeNextChar()) {
                        '\n' -> {
                            this.flushCurrToken()
                            this.currToken = Token(type = TokenType.NewLine)
                            this.flushCurrToken()
                            this.switchState(TokenizerState.StartOfLine)
                        }

                        '\\' -> {
                            handleEscapeSequence(currChar)
                        }

                        in 'a'..'z',
                        in 'A'..'Z',
                        '#',
                        in '0'..'9',
                        ' ', '-', '_'
                            -> {
                            this.currToken.value += currChar
                        }

                        else -> {
                            todo()
                        }
                    }
                }

                TokenizerState.Text -> {
                    when (val currChar = this.consumeNextChar()) {
                        '\n' -> {
                            this.flushCurrToken()
                            this.currToken = Token(type = TokenType.NewLine)
                            this.flushCurrToken()
                            this.switchState(TokenizerState.StartOfLine)
                        }

                        '\\' -> {
                            this.handleEscapeSequence(currChar)
                        }

                        else -> {
                            this.currToken.value += currChar;
                        }
                    }
                }
            }
        }
        this.flushCurrToken()
        return this.tokens
    }

    fun handleEscapeSequence(currChar: Char) {
        // peek to see if next char makes for a valid escape sequence
        when (val nextChar = this.peekNextChar()) {
            'n' -> {
                this.flushCurrToken()
                this.currToken = Token(type = TokenType.NewLine)
                this.flushCurrToken()
                this.consumeForwardBy(1)    // advance cursor forward to consume the 'n'
                this.switchState(TokenizerState.StartOfLine)
            }

            else -> {
                this.currToken.value += currChar
            }
        }
    }

    fun flushCurrToken() {
        if (this.currToken.type != TokenType.Null) {
            if (this.currToken.type == TokenType.PropertyName || this.currToken.type == TokenType.PropertyValue) {
                this.currToken.value = this.currToken.value.trim()
            }
            this.tokens.add(this.currToken)
            this.currToken = nullToken
        }
    }

    fun consumeForwardBy(count: Int): List<Char> {
        val vals = mutableListOf<Char>()
        repeat(count) {
            vals.add(this.consumeNextChar())
        }
        return vals
    }

    /** return the next char without moving the cursor forward */
    fun peekNextChar(): Char? {
        val nextCharIndex = this.cursor
        if (nextCharIndex < this.inputchars.length) {
            return this.inputchars[nextCharIndex]
        }
        return null;
    }

    fun switchState(newState: TokenizerState) {
        this.state = newState
    }

    fun firstCharOfKeywords(): List<Char> {
        return keywords.map { it[0] }
    }

    fun consumeNextChar(): Char {
        if (this.reconsume) {
            this.cursor -= 1
            this.reconsume = false
        }
        val char = this.inputchars[this.cursor]
        this.cursor++
        return char;
    }
}

fun todo(msg: String = "This branch has not yet been implemented") {
    throw Exception(msg)
}