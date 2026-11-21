package com.kid.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BookParserTest {
    private val json = """
        {
          "title": "  Beyond the Sky  ",
          "picture": "book.jpg",
          "units": [{
            "title": "Unit 1",
            "audio": "Unit 1.mp3",
            "time": [{"start": 0, "end": 34.94}, {"start": 34.94, "end": 86.54}],
            "picture": ["unit1/1.jpg", "unit1/2.jpg"]
          }]
        }
    """.trimIndent()

    @Test
    fun parsesTrimmedTitleAndAdjacentMilliseconds() {
        val (title, _, units) = BookParser.parse(json)
        assertEquals("Beyond the Sky", title)
        assertEquals(listOf(0L, 34_940L), units.single().pages.map { it.startMs })
    }

    @Test
    fun rejectsMissingPairsAndParentTraversal() {
        assertThrows(IllegalArgumentException::class.java) {
            BookParser.parse(json.replace(", \"unit1/2.jpg\"", ""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BookParser.parse(json.replace("unit1/2.jpg", "../2.jpg"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BookParser.parse("$json {}")
        }
    }
}
