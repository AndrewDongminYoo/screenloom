package kr.donminzzi.screenloom.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageDecoderInstrumentedTest {
    @Test
    fun decodesPngOpenedByContentResolver() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, "image-decoder-source.png")
        var decoded: Bitmap? = null
        try {
            file.outputStream().use { output ->
                assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, output))
            }

            val result = ImageDecoder(context.contentResolver).decode(Uri.fromFile(file), 2048)

            assertTrue(result.exceptionOrNull()?.stackTraceToString(), result.isSuccess)
            decoded = result.getOrThrow()
            assertEquals(320, decoded.width)
            assertEquals(640, decoded.height)
        } finally {
            runCatching { decoded?.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { source.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { file.delete() }
        }
    }

    @Test
    fun appliesExifRotationToPortraitPhotos() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(640, 320, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, "image-decoder-rotated.jpg")
        var decoded: Bitmap? = null
        try {
            file.outputStream().use { output ->
                assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 100, output))
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_90.toString(),
                )
                saveAttributes()
            }

            val result = ImageDecoder(context.contentResolver).decode(Uri.fromFile(file), 2048)

            assertTrue(result.exceptionOrNull()?.stackTraceToString(), result.isSuccess)
            decoded = result.getOrThrow()
            // The stored frame is 640x320; honouring ORIENTATION_ROTATE_90 swaps the axes.
            assertEquals(320, decoded.width)
            assertEquals(640, decoded.height)
        } finally {
            runCatching { decoded?.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { source.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { file.delete() }
        }
    }

    @Test
    fun appliesExifMirrorToFlippedPhotos() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        Canvas(source).apply {
            drawRect(0f, 0f, 200f, 200f, Paint().apply { color = Color.RED })
            drawRect(200f, 0f, 400f, 200f, Paint().apply { color = Color.BLUE })
        }
        val file = File(context.cacheDir, "image-decoder-flipped.jpg")
        var decoded: Bitmap? = null
        try {
            file.outputStream().use { output ->
                assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 100, output))
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL.toString(),
                )
                saveAttributes()
            }

            val result = ImageDecoder(context.contentResolver).decode(Uri.fromFile(file), 2048)

            assertTrue(result.exceptionOrNull()?.stackTraceToString(), result.isSuccess)
            decoded = result.getOrThrow()
            // ORIENTATION_FLIP_HORIZONTAL carries no rotation, so only the mirror can move the
            // red half from the left edge to the right one.
            assertEquals(400, decoded.width)
            assertEquals(200, decoded.height)
            val left = decoded.getPixel(20, 100)
            val right = decoded.getPixel(380, 100)
            assertTrue("left edge should be blue but was $left", Color.blue(left) > Color.red(left))
            assertTrue("right edge should be red but was $right", Color.red(right) > Color.blue(right))
        } finally {
            runCatching { decoded?.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { source.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { file.delete() }
        }
    }

    @Test
    fun oversizedImageDecodesWithinTheRequestedLongestEdge() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(4096, 1024, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, "image-decoder-oversized.png")
        var decoded: Bitmap? = null
        try {
            file.outputStream().use { output ->
                assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, output))
            }

            decoded = ImageDecoder(context.contentResolver)
                .decode(Uri.fromFile(file), 2048)
                .getOrThrow()

            assertTrue(maxOf(decoded.width, decoded.height) <= 2048)
        } finally {
            runCatching { decoded?.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { source.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { file.delete() }
        }
    }

    @Test
    fun appliesExifTransposeAsMirrorThenRotation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        Canvas(source).apply {
            drawRect(0f, 0f, 200f, 200f, Paint().apply { color = Color.RED })
            drawRect(200f, 0f, 400f, 200f, Paint().apply { color = Color.BLUE })
        }
        val file = File(context.cacheDir, "image-decoder-transposed.jpg")
        var decoded: Bitmap? = null
        try {
            file.outputStream().use { output ->
                assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 100, output))
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_TRANSPOSE.toString(),
                )
                saveAttributes()
            }

            decoded = ImageDecoder(context.contentResolver)
                .decode(Uri.fromFile(file), 2048)
                .getOrThrow()

            assertEquals(200, decoded.width)
            assertEquals(400, decoded.height)
            val top = decoded.getPixel(100, 20)
            val bottom = decoded.getPixel(100, 380)
            assertTrue("top should be red but was $top", Color.red(top) > Color.blue(top))
            assertTrue("bottom should be blue but was $bottom", Color.blue(bottom) > Color.red(bottom))
        } finally {
            runCatching { decoded?.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { source.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { file.delete() }
        }
    }

    @Test
    fun appliesExifTransverseAsMirrorThenRotation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        Canvas(source).apply {
            drawRect(0f, 0f, 200f, 200f, Paint().apply { color = Color.RED })
            drawRect(200f, 0f, 400f, 200f, Paint().apply { color = Color.BLUE })
        }
        val file = File(context.cacheDir, "image-decoder-transversed.jpg")
        var decoded: Bitmap? = null
        try {
            file.outputStream().use { output ->
                assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 100, output))
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_TRANSVERSE.toString(),
                )
                saveAttributes()
            }

            decoded = ImageDecoder(context.contentResolver)
                .decode(Uri.fromFile(file), 2048)
                .getOrThrow()

            assertEquals(200, decoded.width)
            assertEquals(400, decoded.height)
            val top = decoded.getPixel(100, 20)
            val bottom = decoded.getPixel(100, 380)
            assertTrue("top should be blue but was $top", Color.blue(top) > Color.red(top))
            assertTrue("bottom should be red but was $bottom", Color.red(bottom) > Color.blue(bottom))
        } finally {
            runCatching { decoded?.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { source.takeUnless(Bitmap::isRecycled)?.recycle() }
            runCatching { file.delete() }
        }
    }
}
