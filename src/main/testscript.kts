import java.net.URI

val testString = """This is a test tring "https://url.com/man" """
val testString2 = """This is a test tring  "anotherthing3" "uwu.svg" """

fun splitQuotedFromText(str: String) {
    val regex = "\"([])\"".toRegex()
    val res = regex.findAll(str)
    res.forEachIndexed { index, it ->
        val text = it.groups[1]?.value
        println("text: $text ,isValidUrl: ${isValidUrl(text ?: "")}")
    }
    println("\n")
}

splitQuotedFromText(testString)
splitQuotedFromText(testString2)


fun isValidUrl(txt: String): Boolean {
    try {
        val uri = URI.create(txt)
        uri.toURL()
        return true
    } catch (ex: Exception) {
        return false
    }
}