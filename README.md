# Pamflet DSL
Pamflet DSL (Domain Specific Language) is a very tiny markup language used in the pamflet flashcard app for creating the content of flashcards
### Elements
Pamflet DSL is primarily made up of 5 elements from which the content of cards are composed; text, list, multichoice, link and comment

Each element is declared differently but each have a few property/value pair that can be applied to them, usually to add basic styling but can also be used for more specific things depending on the element.
##### 1. Text
```
Something lives in the water
But yet again, it is not here
.color: green
.fontSize: lg
.textAlign: center
```

**`fontSize`**
Possible values incldue `2xs`, `xs`, `sm` ,`base`, `lg`, `xl`, `2xl`, `3xl`. it can also take a px value e.g `20px`

**textAlign**
Possible values include `center`, `start`, `end`

**color**
possible values include any css named color (e.g blue, green, rebeccapurple) or 6digit hex color value (e.g `#FFAABB`)

##### 2. List
```
- this is the first note
- this is another note
.color: gray
.fontSize: lg
```

##### 3. Multichoice
There are 2 variants to the multichoice element; single select and multi select
###### Single select
```
- Hello again
- Say your name -or say nothing
- Don’t stop now, you’ll get better
.color: gray
.fontSize: lg
.correct: 0
.explanation: This is some explanation as to why the answer is the answer
.colorCorrect: #234900
.colorWrong: red
```

###### Multiselect
```
- Hello again
- Say your name -or say nothing
- Don’t stop now, you’ll get better
.color: gray
.fontSize: lg
.correct: 0, 1
.explanation: This is some explanation as to why the answer is the answer
.colorCorrect: #234900
.colorWrong: red
```

##### 4. Link
```
Lnk "https://example.com/helloworld" Hello world 
.color: blue
.fontSize: 20px
```

##### 5. comment
```
// this is a comment
```

