package pamflet.parser

import pamflet.tokenizer.Keyword
import pamflet.tokenizer.Token
import pamflet.tokenizer.TokenType
import pamflet.tokenizer.Tokenizer
import java.net.URI
import java.net.URL
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
                                is Keyword.Img -> { }
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
                            (this.currElement as Element.Link).href = href
                            (this.currElement as Element.Link).linkText = linkText
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

                is Element.List -> {
                    this.setPropertyOnListElement(this.currElementProp)
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
        var href = ""
        val linkText = mutableListOf<String>()

        val isValidUrl: (String) -> Boolean = { text ->
            try {
                URI(text).toURL()
                true
            } catch (e: Exception) {
                false
            }
        }

        str.split(" ").forEach { word ->
            if (word.isNotEmpty()) {
                if (isValidUrl(word)) {
                    href = word
                } else {
                    linkText.add(word)
                }
            }
        }

        return linkText.joinToString(" ") to href
    }
}