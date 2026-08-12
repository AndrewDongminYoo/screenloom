package kr.donminzzi.screenloom.media

import android.graphics.Bitmap
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
}
