package kr.donminzzi.screenloom.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.donminzzi.screenloom.editor.EditorDocument
import kr.donminzzi.screenloom.media.ExportResult
import kr.donminzzi.screenloom.media.OutputStreamProvider
import kr.donminzzi.screenloom.media.PosterExporter
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PosterRendererTest {
    @Test
    fun rendererCreatesExactPosterDimensions() {
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(60, 90, 220))
        }

        val rendered = PosterRenderer().render(
            document = EditorDocument(imageCount = 1),
            images = listOf(source),
            width = 1080,
            height = 1920,
        )

        assertEquals(1080, rendered.width)
        assertEquals(1920, rendered.height)
    }

    @Test
    fun exporterWritesDecodablePng() = runBlocking {
        val bytes = ByteArrayOutputStream()
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
        val exporter = PosterExporter(PosterRenderer(), OutputStreamProvider { bytes })

        val result = exporter.export(
            uri = Uri.parse("content://screenloom/test"),
            document = EditorDocument(imageCount = 1),
            images = listOf(source),
        )

        assertEquals(ExportResult.Success, result)
        val encoded = bytes.toByteArray()
        val decoded = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)
        assertEquals(1080, decoded.width)
        assertEquals(1920, decoded.height)
    }

    @Test
    fun exporterReportsUnavailableOutput() = runBlocking {
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
        val exporter = PosterExporter(PosterRenderer(), OutputStreamProvider { null })

        val result = exporter.export(
            uri = Uri.EMPTY,
            document = EditorDocument(imageCount = 1),
            images = listOf(source),
        )

        assertEquals(ExportResult.Failure("Unable to save PNG"), result)
    }
}
