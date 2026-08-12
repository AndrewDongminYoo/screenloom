package kr.donminzzi.screenloom.render

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.editor.EditorDocument
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PosterPreviewTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun previewExposesOnlyItsConciseDescription() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            PosterPreview(
                document = EditorDocument(
                    title = "Ship beautifully",
                    subtitle = "Store-ready visuals in seconds.",
                ),
                images = emptyList(),
            )
        }

        val previewDescription = context.getString(R.string.poster_preview_description)
        compose.onNodeWithContentDescription(previewDescription).assertIsDisplayed()
        compose.onNodeWithText("Ship beautifully").assertDoesNotExist()
        compose.onNodeWithText("Store-ready visuals in seconds.").assertDoesNotExist()
    }

    @Test
    fun subtitleOnlyStartsAtTheSameVerticalPositionAsTheExport() {
        var document by mutableStateOf(EditorDocument())
        compose.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale = 1f),
            ) {
                Box(Modifier.width(270.dp).testTag("preview-capture")) {
                    PosterPreview(document = document, images = emptyList())
                }
            }
        }

        val previewBaseline = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle {
            document = EditorDocument(subtitle = "Store-ready visuals in seconds.")
        }
        val previewWithSubtitle = compose.onNodeWithTag("preview-capture").captureToImage()
        val previewTop = firstDifferentRow(previewBaseline, previewWithSubtitle) *
            1080f / previewWithSubtitle.width

        val renderer = PosterRenderer()
        val exportBaseline = renderer.render(EditorDocument(), emptyList(), 1080, 1920)
        val exportWithSubtitle = renderer.render(document, emptyList(), 1080, 1920)
        val exportTop = firstDifferentRow(exportBaseline, exportWithSubtitle).toFloat()

        assertTrue(
            "Preview subtitle starts at $previewTop but export starts at $exportTop",
            abs(previewTop - exportTop) <= 8f,
        )
    }

    private fun firstDifferentRow(baseline: ImageBitmap, changed: ImageBitmap): Int {
        val baselinePixels = baseline.toPixelMap()
        val changedPixels = changed.toPixelMap()
        for (y in 0 until changed.height) {
            for (x in 0 until changed.width) {
                if (baselinePixels[x, y] != changedPixels[x, y]) return y
            }
        }
        error("Images are identical")
    }

    private fun firstDifferentRow(baseline: Bitmap, changed: Bitmap): Int {
        val baselinePixels = IntArray(baseline.width * baseline.height)
        val changedPixels = IntArray(changed.width * changed.height)
        baseline.getPixels(baselinePixels, 0, baseline.width, 0, 0, baseline.width, baseline.height)
        changed.getPixels(changedPixels, 0, changed.width, 0, 0, changed.width, changed.height)
        for (index in changedPixels.indices) {
            if (baselinePixels[index] != changedPixels[index]) return index / changed.width
        }
        error("Bitmaps are identical")
    }
}
