package pamflet

val testStrings = listOf(
/* 0 */    """But yet $23 thing to note.\nThis is something""".trimIndent(),

/* 1 */    """But yet $23 things\ to note.
This is something
    """.trimMargin(),

    /* 2 */   """But yet $23 thing to note.
This is something
.color:green
.block-size:3px
.fontSize : 23px""".trimIndent(),

    /* 3 */ """
- Hello again
- Say your - name.
- another th's to pay
.answer : 0,2
    """.trimIndent(),

    /* 4 */ """
        What are the various kinds of greetings?
        - Hello again
        - Say your - name.
        - another th's to pay
        .answer : 0,2
        .explanation: This is some explanation on why the answer is the answer
        .color : green
    """.trimIndent(),

    /* 5 */ """
Lnk http://www.example.com/idontevenknow/sk hello world 

What are the various kinds of greetings?
- Hello again
- another th's to pay

Lnk "https://example.com/hello" 
    """.trimIndent(),

    /* 6 */ """Aud "src.mp3""",

    /* 7 */ """
What are the various kinds of greetings?
- Hello again
- another th's to pay
.answer : 0
    """.trimIndent(),

   /* 8 */  """
Some text that needs reading
// this is a comment
Even some more text
.fontSize : lg
   """.trimIndent()

)
