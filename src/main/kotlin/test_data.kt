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
- fruits and vegetables
- cereals
- Legumes, and beans
.correct:1,3
.explanation: This is some explanation
.control: false
    """.trimIndent(),

    /* 4 */ """
        What are the various kinds of greetings?
        - Hello again
        - Say your - name.
        - another th's to pay
        .correct: 0,2
        .explanation: This is some explanation on why the answer is the answer
        .color : green
    """.trimIndent(),

    /* 5 */ """
Lnk http://www.example.com/idontevenknow/sk hello world 
.color: green

What are the various kinds of greetings?
- Hello again
- another th's to pay
.fontSize: lg

Lnk "https://example.com/hello" 
    """.trimIndent(),

    /* 6 */ """Aud "src.mp3"
Img "burning_bush.svg"


Hello, it is I
Img "burning_bush.svg"
This is some random text
Img "boy-in-the-yard.png"  this is some text
Img "the-0clayface.jpg"  this is some text
    """.trimMargin(),

    /* 7 */ """
What are the various kinds of greetings?
- Hello again
- another th's to pay
.correct : 0
    """.trimIndent(),

   /* 8 */  """
Some text that needs reading
// this is a comment
Even some more text
.fontSize : lg
   """.trimIndent(),

   /* 9 */  """
This is a test string for text alignment
.textAlign: end
   """.trimIndent()

)
