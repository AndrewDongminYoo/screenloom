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
        file.outputStream().use { output ->
            assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, output))
        }

        val result = ImageDecoder(context.contentResolver).decode(Uri.fromFile(file), 2048)

        assertTrue(result.exceptionOrNull()?.stackTraceToString(), result.isSuccess)
        result.getOrThrow().let { decoded ->
            assertEquals(320, decoded.width)
            assertEquals(640, decoded.height)
            decoded.recycle()
        }
        source.recycle()
        file.delete()
        Unit
    }

    @Test
    fun appliesExifRotationToPortraitPhotos() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(640, 320, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, "image-decoder-rotated.jpg")
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
        result.getOrThrow().let { decoded ->
            // The stored frame is 640x320; honouring ORIENTATION_ROTATE_90 swaps the axes.
            assertEquals(320, decoded.width)
            assertEquals(640, decoded.height)
            decoded.recycle()
        }
        source.recycle()
        file.delete()
        Unit
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
        result.getOrThrow().let { decoded ->
            // ORIENTATION_FLIP_HORIZONTAL carries no rotation, so only the mirror can move the
            // red half from the left edge to the right one.
            assertEquals(400, decoded.width)
            assertEquals(200, decoded.height)
            val left = decoded.getPixel(20, 100)
            val right = decoded.getPixel(380, 100)
            assertTrue("left edge should be blue but was $left", Color.blue(left) > Color.red(left))
            assertTrue("right edge should be red but was $right", Color.red(right) > Color.blue(right))
            decoded.recycle()
        }
        source.recycle()
        file.delete()
        Unit
    }
}
