package kr.donminzzi.screenloom.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.editor.EditorDocument
import kr.donminzzi.screenloom.media.ExportResult
import kr.donminzzi.screenloom.media.OutputStreamProvider
import kr.donminzzi.screenloom.media.PosterExporter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun exporterWritesOpaqueTruecolorPngAtExactDimensions() = runBlocking {
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
        assertTrue("Expected a complete PNG IHDR header", encoded.size >= 26)
        assertArrayEquals(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            encoded.copyOfRange(0, 8),
        )
        assertEquals("IHDR", String(encoded, 12, 4, Charsets.US_ASCII))
        assertEquals(8, encoded[24].toInt() and 0xFF)
        assertEquals(2, encoded[25].toInt() and 0xFF)
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

        assertEquals(ExportResult.Failure(R.string.export_failure), result)
    }

    @Test
    fun exporterReportsWriteFailure() = runBlocking {
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
        try {
            val exporter = PosterExporter(
                PosterRenderer(),
                OutputStreamProvider {
                    object : OutputStream() {
                        override fun write(value: Int) = throw IOException("disk full")
                    }
                },
            )

            val result = exporter.export(Uri.EMPTY, EditorDocument(imageCount = 1), listOf(source))

            assertEquals(ExportResult.Failure(R.string.export_failure), result)
        } finally {
            source.recycle()
        }
    }

    @Test
    fun exporterPropagatesCancellationInsteadOfReportingFailure() = runBlocking {
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
        val exporter = PosterExporter(
            PosterRenderer(),
            OutputStreamProvider { throw CancellationException("scope cleared") },
        )

        val thrown = runCatching {
            exporter.export(
                uri = Uri.parse("content://screenloom/test"),
                document = EditorDocument(imageCount = 1),
                images = listOf(source),
            )
        }.exceptionOrNull()

        assertTrue("expected CancellationException, got $thrown", thrown is CancellationException)
        source.recycle()
    }

    @Test
    fun rendererEllipsizesLongTitlesLikeThePreview() {
        val longTitle = "W".repeat(60)
        val visibleTitle = ellipsizedText(longTitle, textSize = 78f)
        val renderer = PosterRenderer()

        val rendered = renderer.render(EditorDocument(title = longTitle), emptyList(), 1080, 1920)
        val expected = renderer.render(EditorDocument(title = visibleTitle), emptyList(), 1080, 1920)

        assertTrue(rendered.sameAs(expected))
    }

    @Test
    fun rendererEllipsizesLongSubtitlesLikeThePreview() {
        val longSubtitle = "W".repeat(100)
        val visibleSubtitle = ellipsizedText(longSubtitle, textSize = 32f)
        val renderer = PosterRenderer()

        val rendered = renderer.render(EditorDocument(subtitle = longSubtitle), emptyList(), 1080, 1920)
        val expected = renderer.render(EditorDocument(subtitle = visibleSubtitle), emptyList(), 1080, 1920)

        assertTrue(rendered.sameAs(expected))
    }

    private fun ellipsizedText(text: String, textSize: Float): String {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = Typeface.create(
                if (textSize == 78f) Typeface.SERIF else Typeface.DEFAULT,
                if (textSize == 78f) Typeface.BOLD else Typeface.NORMAL,
            )
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, 900)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        val lastLine = layout.lineCount - 1
        val visibleEnd = layout.getLineStart(lastLine) + layout.getEllipsisStart(lastLine)
        return text.take(visibleEnd) + "…"
    }
}
