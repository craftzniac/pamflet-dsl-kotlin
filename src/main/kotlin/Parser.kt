package pamflet.parser

import pamflet.parser.generateId
import pamflet.tokenizer.Keyword
import pamflet.tokenizer.Token
import pamflet.tokenizer.TokenType
import pamflet.tokenizer.Tokenizer
import java.net.URI
import kotlin.String
import kotlin.math.floor

enum class TextAlign(val value: String) {
    Center("center"),
    Left("left"),
    Right("right"),
}

data class ElementProp(
    var name: String,
    var value: String = ""
) {
    companion object {
        val Null = ElementProp(name = "null")
    }
}

fun generateId(): String {
    val length = 20
    var id: String = ""
    val chars: String = "abcdefghijklmnopqrstuvwxyz0123456789"
    for (i in 0 until length) {
        val index = floor(Math.random() * chars.length).toInt()
        id += chars[index]
    }
    return id
}

sealed class Element {
    abstract val id: String
    abstract var color: String
    abstract var fontSize: String

    data class Text(
        override val id: String = generateId(),
        override var color: String = "",
        override var fontSize: String = "1.2rem",
        var content: String = "",
        var textAlign: TextAlign = TextAlign.Center,
    ) : Element()

    sealed class Multichoice : Element() {
        abstract var options: MutableList<String>
        abstract var explanation: String

        data class SingleSelect(
            override val id: String = generateId(),
            override var color: String = "",
            override var fontSize: String = "1.2rem",
            override var options: MutableList<String> = mutableListOf(),
            override var explanation: String = "",
            var correct: UInt? = null,
        ) : Multichoice()

        data class MultiSelect(
            override val id: String = generateId(),
            override var color: String = "",
            override var fontSize: String = "1.2rem",
            override var options: MutableList<String> = mutableListOf(),
            override var explanation: String = "",
            var correct: kotlin.collections.List<UInt> = listOf(),
        ) : Multichoice()
    }

    data class List(
        override val id: String = generateId(),
        override var color: String = "",
        override var fontSize: String = "1.2rem",     // there probably has to be conversions?? cos it will be rendered on android
        var items: MutableList<String> = mutableListOf()
    ) : Element()

    data class Link(
        override val id: String = generateId(),
        override var color: String = "",
        override var fontSize: String = "1.2rem",
        var href: String = "",
        var linkText: String = ""
    ) : Element()

    object NullElement : Element() {
        override val id: String
            get() = "null"
        override var color: String = "null"
        override var fontSize: String = "null"
    }

    override fun toString(): String {
        return when (this) {
            is NullElement -> "Null"
            is Link -> "Link"
            is List -> "List"
            is Text -> "Text"
            is Multichoice.SingleSelect -> "Multichoice.SingleS"
            is Multichoice.MultiSelect -> "Multichoice.MultiS"
        }
    }
}

val nullElementProp = ElementProp.Null

enum class ParserState {
    Data,   // just read data
    TextElement,
    ElementProp,
    PropName,
    PropValue,
    InitializeListElement,
    ListElementOptions,
    LinkContent
}

class Parser(val inputchars: String) {
    private val elements: MutableList<Element> = mutableListOf()
    var cursor: Int = 0
    var state: ParserState = ParserState.Data
    var currElementProp: ElementProp = nullElementProp
    var currElement: Element = Element.NullElement

    val tokens = run {
        val tok = Tokenizer(inputchars)
        tok.tokenize()
    }

    val reconsume = {
        this.cursor--
    }

    fun switchState(newState: ParserState) {
        this.state = newState
    }

    fun parse(): List<Element> {
        while (this.cursor < this.tokens.size) {
            when (this.state) {
                ParserState.Data -> {
                    val token = this.consumeNextToken()
                    when (token.type) {
                        TokenType.Text -> {
                            this.reconsume()
                            this.switchState(ParserState.TextElement)
                        }

                        TokenType.PropertyName -> {}
                        TokenType.PropertyValue -> {}
                        TokenType.ListItem -> {
                            this.reconsume()
                            this.switchState(ParserState.InitializeListElement)
                        }

                        TokenType.Keyword -> {
                            when (Keyword.from(token.value)) {
                                is Keyword.Aud -> {}
                                is Keyword.Img -> {}
                                is Keyword.Lnk -> {
                                    this.currElement = Element.Link()
                                    this.switchState(ParserState.LinkContent)
                                }

                                is Keyword.Invalid -> {}
                            }
                        }

                        TokenType.KeywordValue -> {}
                        TokenType.Comment, TokenType.Null -> {
                            /* ignore it */
                        }
                    }
                }

                ParserState.LinkContent -> {
                    if (this.currElement !is Element.Link) {
                        throw Exception("Expected a link element but got a ${this.currElement} instead")
                    }

                    val token = this.consumeNextToken()
                    when (token.type) {
                        TokenType.KeywordValue -> {
                            val text = token.value
                            val (linkText, href) = this.getLinkTextAndHref(token.value)
                            println("linkText::$linkText, href::$href")
                            (this.currElement as Element.Link).href = href
                            (this.currElement as Element.Link).linkText = linkText.ifEmpty { href }
                            this.switchState(ParserState.ElementProp)
                        }

                        else -> {
                            this.flushCurrElement()
                            this.reconsume()
                            this.switchState(ParserState.Data)
                        }
                    }
                }

                ParserState.TextElement -> {
                    val token = this.consumeNextToken()
                    if (token.type != TokenType.Text) {   // ideally, this block never executes
                        throw Exception("Expected a text token here but got a ${token.type.label} instead")
                    }

                    this.currElement = Element.Text(content = token.value)
                    this.switchState(ParserState.ElementProp)
                }

                ParserState.ElementProp -> {
                    val token = this.consumeNextToken()
                    if (token.type == TokenType.PropertyName) {
                        this.reconsume()
                        this.switchState(ParserState.PropName)
                    } else {
                        this.flushCurrElement()
                        this.reconsume()
                        this.switchState(ParserState.Data)
                    }
                }

                ParserState.PropName -> {
                    val token = this.consumeNextToken()
                    if (token.type == TokenType.PropertyName) {
                        this.currElementProp.name = token.value
                        this.switchState(ParserState.PropValue)
                    } else {
                        this.reconsume()
                        this.switchState(ParserState.Data)
                    }
                }

                ParserState.PropValue -> {
                    val token = this.consumeNextToken()
                    when (token.type) {
                        TokenType.PropertyValue -> {
                            this.currElementProp.value = token.value
                            this.flushCurrElementProp()
                            this.switchState(ParserState.ElementProp)
                        }

                        TokenType.PropertyName -> {
                            this.flushCurrElementProp()
                            this.reconsume()
                            this.switchState(ParserState.PropName)
                        }

                        else -> {
                            this.reconsume()
                            this.flushCurrElement()
                            this.switchState(ParserState.Data)
                        }
                    }
                }

                ParserState.InitializeListElement -> {
                    this.currElement = Element.List()
                    this.switchState(ParserState.ListElementOptions)
                }

                ParserState.ListElementOptions -> {
                    val token = this.consumeNextToken()
                    when (token.type) {
                        TokenType.ListItem -> {
                            if (this.currElement !is Element.List) {
                                throw Exception("List element was expected  here but found ${this.currElement} instead")
                            }

                            (this.currElement as Element.List).items.add(token.value)
                        }

                        TokenType.PropertyName -> {
                            this.reconsume()
                            this.switchState(ParserState.ElementProp)
                        }

                        else -> {
                            this.flushCurrElement()
                            this.reconsume()
                            this.switchState(ParserState.Data)
                        }
                    }
                }
            }
        }
        this.flushCurrElement()
        return this.elements
    }

    fun flushCurrElement() {
        this.flushCurrElementProp()
        if (this.currElement !is Element.NullElement) {
            this.elements.add(this.currElement)
            this.currElement = Element.NullElement
        }
    }

    fun flushCurrElementProp() {
        if (this.currElement !is Element.NullElement) {
            // figure out what the element is exactly and
            when (this.currElement) {
                // figure out if the property is right for that element
                is Element.Text -> {
                    this.setPropertyOnTextElement(this.currElementProp)
                }

                is Element.Multichoice -> {
                    this.setPropertyOnMultichoiceElement(this.currElementProp)
                }

                is Element.List -> {
                    if (this.currElementProp.name == "correct") {
                        this.transformListToMultichoiceAndFlushCurrElementProp()
                    } else {
                        this.setPropertyOnListElement(this.currElementProp)
                    }
                }

                is Element.Link -> {
                    this.setPropertyOnLinkElement(this.currElementProp)
                }

                Element.NullElement -> {
                    // do nothing
                }
            }
        }
    }

    fun transformListToMultichoiceAndFlushCurrElementProp() {
        if (this.currElement !is Element.List) {
            throw Exception("Expected List element here found ${this.currElement}")
        }
        val element = this.currElement as Element.List
        val prop = this.currElementProp;
        if (prop.name != "correct") {
            throw Exception("Property name 'correct' was expected but found ${prop.name}")
        }
        val indexes = prop.value.split(",").map { it.trim() }
        val correct = mutableListOf<UInt>()
        for (i in indexes) {
            val index = i.toUIntOrNull()
            if (index != null && index < element.items.size.toUInt() && index >= 0u) { // bounds checking
                correct.add(index);
            } else {  // clear list if there's even 1 invalid index
                correct.clear()
                break;
            }
        }

        val multichoice: Element.Multichoice = if (correct.isEmpty() || correct.size == 1) {
            val ss = Element.Multichoice.SingleSelect(
                color = element.color,
                fontSize = element.fontSize,
                options = element.items,
            )
            if (correct.isEmpty()) {
                ss.correct = null
            } else {   // correct.size == 1
                ss.correct = correct[0]
            }

            ss
        } else {
            // multiple indexes
            val ms = Element.Multichoice.MultiSelect(
                correct = correct,
                color = element.color,
                fontSize = element.fontSize,
                options = element.items,
            )

            ms
        }

        this.currElement = multichoice
    }

    fun setPropertyOnTextElement(prop: ElementProp) {
        if (handleCommonProperty(prop)) return
        when (prop.name) {
            "textAlign" -> {
                when (prop.value) {
                    TextAlign.Center.value -> {
                        (this.currElement as Element.Text).textAlign = TextAlign.Center
                    }

                    TextAlign.Left.value -> {
                        (this.currElement as Element.Text).textAlign = TextAlign.Left
                    }

                    TextAlign.Right.value -> {
                        (this.currElement as Element.Text).textAlign = TextAlign.Right
                    }
                }
            }
        }
    }

    fun setPropertyOnLinkElement(prop: ElementProp) {
        if (handleCommonProperty(prop)) return
    }

    fun setPropertyOnMultichoiceElement(prop: ElementProp) {
        if (handleCommonProperty(prop)) return
        when(prop.name){
            "explanation" -> (this.currElement as Element.Multichoice).explanation = prop.value
        }
    }

    fun setPropertyOnListElement(prop: ElementProp) {
        if (handleCommonProperty(prop)) return
    }

    fun handleCommonProperty(prop: ElementProp): Boolean {
        var isPropHandled = true
        when (prop.name) {
            "color" -> {
                this.currElement.color = prop.value
            }

            "fontSize" -> {
                this.currElement.fontSize = prop.value
            }

            else -> isPropHandled = false
        }
        return isPropHandled
    }

    fun consumeNextToken(): Token {
        val token = this.tokens[this.cursor]
        this.cursor++
        return token
    }

    /**
     * parses the link content into link text and href
     * */
    fun getLinkTextAndHref(str: String): Pair<String, String> {
        val str = str.trim()
        var quoteContent = ""
        var text = ""
        var betweenQuotes = false;

        for (i in 0 until str.length) {
            if (str[i] == '"') {
                betweenQuotes = !betweenQuotes
                continue
            }
            if (betweenQuotes) {
                quoteContent += str[i]
            } else {
                text += str[i]
            }
        }

        text = text.trim()
        var href = ""

        if (quoteContent.isNotEmpty()) {
            href = if (isValidUrl(quoteContent)) quoteContent else ""
            return text.trim() to href.trim()
        } else {
            // look at the text to see if there's any url
            for (i in 0 until text.length) {
                if (text[i] == 'h') {
                    val opts = listOf("http://", "https://")
                    // peek forward starting from current index to see if next char sequence is https:// or http://
                    if ((text.length - i >= opts[0].length  // bounds checking
                                && text.substring(i, i + opts[0].length) == opts[0])
                        ||
                        (text.length - i >= opts[1].length  // bounds checking
                                && text.substring(i, i + opts[1].length) == opts[1])
                    ) {
                        var tempText = text.take(i)
                        // beginning at the h until a white space char or end of string
                        var whiteSpaceIndex = -1;
                        for (j in i until text.length) {
                            if (text[j] == ' ') { // once you encounter a space, stop reading url
                                whiteSpaceIndex = j
                                break;
                            } else {
                                href += text[j]
                            }
                        }

                        if (whiteSpaceIndex > 0) {  // whitespace can never be at index 0 since text is trimmed beforehand
                            tempText += text.substring(whiteSpaceIndex, text.length)
                            text = tempText
                        }
                        break
                    }
                    // peek forward to see if next char sequence is ttp://
                }
            }
            return text.trim() to href.trim()
        }
    }
}

val isValidUrl: (String) -> Boolean = { text ->
    try {
        URI(text).toURL()
        true
    } catch (e: Exception) {
        false
    }
}
