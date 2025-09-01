package pamflet.tokenizer

data class Token(
    val type: TokenType,
    var value: String = ""
)

enum class TokenType(val label: String) {
    Text("text"),
    Null("null"),
//    NewLine,
    PropertyName("propertyName"),
//    Equals,
    PropertyValue("propertyValue"),
    Comment("comment"),
    ListItem("listItem"),
    Keyword("keyword"),
    KeywordValue("keywordValue");
}

enum class TokenizerState {
    Data,
    StartOfLine,
    Text,
    PropertyName,
    PropertyValue,
    Comment,
    ListItem,
    Keyword,
    KeywordValue
}

val nullToken = Token(type = TokenType.Null)

val keywords = listOf("Aud", "Lnk", "Img");

class Tokenizer(val inputchars: String) {
    var state: TokenizerState = TokenizerState.StartOfLine
    var cursor: Int = 0
    var reconsume = false
    var currToken = nullToken
    private val tokens: MutableList<Token> = mutableListOf()

    fun tokenize(): List<Token> {
        while (cursor < inputchars.length) {
            when (this.state) {
                TokenizerState.StartOfLine -> {
                    // read consumeNextChar
                    when (val currChar = this.consumeNextChar()) {
                        '\\' -> {  // start of escape sequence
                            handleEscapeSequence(currChar)
                        }

                        in firstCharOfKeywords() -> {   // is start of a keyword
                            this.switchState(TokenizerState.Keyword)
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
                                    this.switchToTextState()
                                }
                            }
                        }

                        '-' -> {  // possible start of a list item
                            // peek to see if the next char is a space
                            val nextChar = peekNextChar()
                            if (nextChar == ' ') {
                                this.currToken = Token(type = TokenType.ListItem)
                                this.consumeForwardBy(1)  // advance over the ' ' character
                                this.switchState(TokenizerState.ListItem)
                            } else {
                                this.switchToTextState()
                            }
                        }

                        '/' -> {   // possible start of a comment
                            // check if the next char is a /
                            val nextChar = peekNextChar()
                            if (nextChar == '/') {
                                this.consumeForwardBy(1) // advance the cursor my 1 index forward
                                this.currToken = Token(type = TokenType.Comment)
                                this.switchState(TokenizerState.Comment)
                            } else {
                                this.switchToTextState()
                            }
                        }

                        '[' -> {   // start of a code bloc
                            TODO()
                        }

                        else -> {   // start of regular text
                            this.switchToTextState()
                        }
                    }
                }

                TokenizerState.Keyword -> {
                    // read the next 3 chars starting with the current char and see if it matches any keyword
                    // peek by 4 chars because even though keywords are 3 chars long, I should make sure the next char after the keyword is a space.
                    // basically I want to test against 'Lnk ' not 'Lnk'
                    val fourChars = peekForwardBy(4, startFromCurrCharPosition = true).first
                    if (fourChars in keywords.map{ "$it " }) {
                        this.consumeForwardBy(3)   // consume the next 2 chars of the keyword + 1 (which is the space that follows)
                        this.currToken = Token(type = TokenType.Keyword)
                        this.currToken.value = fourChars.trim()
                        this.flushCurrToken()
                        this.currToken = Token(type = TokenType.KeywordValue)
                        this.switchState(TokenizerState.KeywordValue)
                    } else {
                        // not a keyword, so tokenize as Text
                        this.switchToTextState()
                    }
                }

                TokenizerState.KeywordValue -> {
                    when (val currChar = this.consumeNextChar()) {
                        '\n' -> {
                            println("currToken before flush:: ${this.currToken}")
                            handleNewLine()
                        }

                        '\\' -> {
                            handleEscapeSequence(currChar)
                        }

                        else -> {
                            this.currToken.value += currChar;
                        }
                    }
                }

                TokenizerState.ListItem -> {
                    when (val currChar = this.consumeNextChar()) {
                        '\n' -> {
                            this.handleNewLine()
                        }

                        else -> {
                            this.currToken.value += currChar
                        }
                    }
                }

                TokenizerState.Data -> {
                    TODO()
                }

                TokenizerState.Comment -> {
                    when (val currChar = this.consumeNextChar()) {
                        '\n' -> {
                            this.handleNewLine()
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
//                            this.currToken = Token(type = TokenType.Equals)
//                            this.flushCurrToken()
                            this.currToken = Token(type = TokenType.PropertyValue)
                            this.switchState(TokenizerState.PropertyValue)
                        }

                        '\n' -> {
                            this.handleNewLine()
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
                            this.handleNewLine()
                        }

                        '\\' -> {
                            handleEscapeSequence(currChar)
                        }

                        else -> {
                            this.currToken.value += currChar
                        }
                    }
                }

                TokenizerState.Text -> {
                    when (val currChar = this.consumeNextChar()) {
                        '\n' -> {
                            this.handleNewLine()
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
                this.consumeForwardBy(1)    // advance cursor forward to consume the 'n'
                this.handleNewLine()
            }

            else -> {
                this.currToken.value += currChar
            }
        }
    }

    fun handleNewLine() {
        this.flushCurrToken()
//        this.currToken = Token(type = TokenType.NewLine)
//        this.flushCurrToken()
        this.switchState(TokenizerState.StartOfLine)
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

    /**
     * read the next x number of chars given by count and return them, while updating the cursor position
     * */
    fun consumeForwardBy(count: Int): List<Char> {
        val vals = mutableListOf<Char>()
        repeat(count) {
            vals.add(this.consumeNextChar())
        }
        return vals
    }

    /** read the next char without updating the cursor position */
    fun peekNextChar(): Char? {
        val nextCharIndex = this.cursor
        if (nextCharIndex < this.inputchars.length) {
            return this.inputchars[nextCharIndex]
        }
        return null;
    }

    fun peekForwardBy(count: Int, startFromCurrCharPosition: Boolean = false): Pair<String, Boolean> {
        var vals = ""
        var isOutOfChars = false;
        for (i in 0 until count) {
            val char = run {
                val index = run {
                    if (startFromCurrCharPosition) {
                        (this.cursor - 1) + i
                    } else {
                        this.cursor + i
                    }
                }
                this.inputchars.getOrNull(index)
            }

            if (char == null) {
                isOutOfChars = true
                break
            }
            vals += char
        }
        return vals to isOutOfChars
    }

    fun switchState(newState: TokenizerState) {
        this.state = newState
    }

    fun switchToTextState() {
        this.reconsume = true
        // create empty text token
        this.currToken = Token(type = TokenType.Text)
        this.switchState(TokenizerState.Text);
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