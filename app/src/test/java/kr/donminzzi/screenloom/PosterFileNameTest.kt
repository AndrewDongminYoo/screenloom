package kr.donminzzi.screenloom

import org.junit.Assert.assertEquals
import org.junit.Test

class PosterFileNameTest {
    @Test
    fun titleBecomesPortablePngFileName() {
        assertEquals(
            "launch-day-poster.png",
            posterFileName(" Launch day / poster! "),
        )
    }

    @Test
    fun blankTitleUsesFallback() {
        assertEquals("screenloom-poster.png", posterFileName("  "))
    }
}
