package kr.donminzzi.screenloom.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageDecoderTest {
    @Test
    fun oversizedImageUsesPowerOfTwoSampling() {
        val result = ImageDecoder.calculateInSampleSize(
            width = 8000,
            height = 4000,
            maxDimension = 2048,
        )

        assertEquals(4, result)
    }

    @Test
    fun imageWithinLimitIsNotDownsampled() {
        val result = ImageDecoder.calculateInSampleSize(
            width = 1080,
            height = 1920,
            maxDimension = 2048,
        )

        assertEquals(1, result)
    }
}
