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
import androidx.compose.ui.graphics.asImageBitmap
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
import kr.donminzzi.screenloom.editor.ShadowLevel
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    @Test
    fun twoLineCopyMatchesTheExportVerticalPositions() {
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

        val previewWithoutCopy = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle {
            document = EditorDocument(title = "Launch better")
        }
        val previewWithOneLine = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle {
            document = EditorDocument(title = "Launch better\nShare faster")
        }
        val previewWithTwoLines = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle {
            document = document.copy(subtitle = "Store-ready visuals")
        }
        val previewWithSubtitle = compose.onNodeWithTag("preview-capture").captureToImage()
        val previewScale = 1080f / previewWithTwoLines.width
        val previewFirstLineTop = firstDifferentRow(previewWithoutCopy, previewWithOneLine) * previewScale
        val previewSecondLineTop = firstDifferentRow(previewWithOneLine, previewWithTwoLines) * previewScale
        val previewSubtitleTop = firstDifferentRow(previewWithTwoLines, previewWithSubtitle) * previewScale

        val renderer = PosterRenderer()
        val exportWithoutCopy = renderer.render(EditorDocument(), emptyList(), 1080, 1920)
        val exportWithOneLine = renderer.render(
            EditorDocument(title = "Launch better"),
            emptyList(),
            1080,
            1920,
        )
        val exportWithTwoLines = renderer.render(
            EditorDocument(title = "Launch better\nShare faster"),
            emptyList(),
            1080,
            1920,
        )
        val exportWithSubtitle = renderer.render(document, emptyList(), 1080, 1920)
        val exportFirstLineTop = firstDifferentRow(exportWithoutCopy, exportWithOneLine).toFloat()
        val exportSecondLineTop = firstDifferentRow(exportWithOneLine, exportWithTwoLines).toFloat()
        val exportSubtitleTop = firstDifferentRow(exportWithTwoLines, exportWithSubtitle).toFloat()
        val previewLineSpacing = previewSecondLineTop - previewFirstLineTop
        val exportLineSpacing = exportSecondLineTop - exportFirstLineTop
        val previewSubtitleOffset = previewSubtitleTop - previewFirstLineTop
        val exportSubtitleOffset = exportSubtitleTop - exportFirstLineTop

        assertTrue(
            "Preview line spacing is $previewLineSpacing but export spacing is $exportLineSpacing",
            abs(previewLineSpacing - exportLineSpacing) <= 2f,
        )
        assertTrue(
            "Preview rows are $previewFirstLineTop, $previewSecondLineTop, $previewSubtitleTop " +
                "but export rows are $exportFirstLineTop, $exportSecondLineTop, $exportSubtitleTop",
            abs(previewSubtitleOffset - exportSubtitleOffset) <= 3f,
        )
    }

    @Test
    fun twoLineSubtitleMatchesTheExportLineSpacing() {
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

        val previewWithoutSubtitle = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle {
            document = EditorDocument(subtitle = "Store-ready visuals")
        }
        val previewWithOneLine = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle {
            document = EditorDocument(subtitle = "Store-ready visuals\nWithout the wait")
        }
        val previewWithTwoLines = compose.onNodeWithTag("preview-capture").captureToImage()
        val previewScale = 1080f / previewWithTwoLines.width
        val previewFirstLineTop = firstDifferentRow(previewWithoutSubtitle, previewWithOneLine) * previewScale
        val previewSecondLineTop = firstDifferentRow(previewWithOneLine, previewWithTwoLines) * previewScale

        val renderer = PosterRenderer()
        val exportWithoutSubtitle = renderer.render(EditorDocument(), emptyList(), 1080, 1920)
        val exportWithOneLine = renderer.render(
            EditorDocument(subtitle = "Store-ready visuals"),
            emptyList(),
            1080,
            1920,
        )
        val exportWithTwoLines = renderer.render(document, emptyList(), 1080, 1920)
        val exportFirstLineTop = firstDifferentRow(exportWithoutSubtitle, exportWithOneLine).toFloat()
        val exportSecondLineTop = firstDifferentRow(exportWithOneLine, exportWithTwoLines).toFloat()
        val previewLineSpacing = previewSecondLineTop - previewFirstLineTop
        val exportLineSpacing = exportSecondLineTop - exportFirstLineTop

        assertTrue(
            "Preview subtitle spacing is $previewLineSpacing but export spacing is $exportLineSpacing",
            abs(previewLineSpacing - exportLineSpacing) <= 2f,
        )
    }

    @Test
    fun previewCopyIgnoresSystemFontScale() {
        var fontScale by mutableStateOf(1f)
        compose.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale),
            ) {
                Box(Modifier.width(270.dp).testTag("preview-capture")) {
                    PosterPreview(
                        document = EditorDocument(
                            title = "Ship beautifully",
                            subtitle = "Store-ready visuals in seconds.",
                        ),
                        images = emptyList(),
                    )
                }
            }
        }

        val defaultScale = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle { fontScale = 2f }
        val enlargedScale = compose.onNodeWithTag("preview-capture").captureToImage()

        assertTrue("Preview copy changed with system font scale", imagesAreEqual(defaultScale, enlargedScale))
    }

    @Test
    fun strongPreviewShadowExtendsBeyondScreenshotBounds() {
        var images by mutableStateOf(emptyList<ImageBitmap>())
        var shadow by mutableStateOf(ShadowLevel.Soft)
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
        compose.setContent {
            Box(Modifier.width(270.dp).testTag("preview-capture")) {
                PosterPreview(
                    document = EditorDocument(shadow = shadow),
                    images = images,
                )
            }
        }

        val background = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle {
            images = listOf(source.asImageBitmap())
        }
        val softShadow = compose.onNodeWithTag("preview-capture").captureToImage()
        compose.runOnIdle { shadow = ShadowLevel.Strong }
        val strongShadow = compose.onNodeWithTag("preview-capture").captureToImage()
        val scale = strongShadow.width / 1080f
        val sampleX = (150f * scale).roundToInt()
        val sampleY = (1000f * scale).roundToInt()

        assertEquals(
            background.toPixelMap()[sampleX, sampleY],
            softShadow.toPixelMap()[sampleX, sampleY],
        )
        assertNotEquals(
            background.toPixelMap()[sampleX, sampleY],
            strongShadow.toPixelMap()[sampleX, sampleY],
        )

        val renderer = PosterRenderer()
        val exportBackground = renderer.render(
            EditorDocument(shadow = ShadowLevel.Strong),
            emptyList(),
            1080,
            1920,
        )
        val exportSoftShadow = renderer.render(
            EditorDocument(shadow = ShadowLevel.Soft),
            listOf(source),
            1080,
            1920,
        )
        val exportStrongShadow = renderer.render(
            EditorDocument(shadow = ShadowLevel.Strong),
            listOf(source),
            1080,
            1920,
        )
        assertEquals(
            exportBackground.getPixel(150, 1000),
            exportSoftShadow.getPixel(150, 1000),
        )
        assertNotEquals(
            exportBackground.getPixel(150, 1000),
            exportStrongShadow.getPixel(150, 1000),
        )
    }

    private fun imagesAreEqual(first: ImageBitmap, second: ImageBitmap): Boolean {
        if (first.width != second.width || first.height != second.height) return false
        val firstPixels = first.toPixelMap()
        val secondPixels = second.toPixelMap()
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                if (firstPixels[x, y] != secondPixels[x, y]) return false
            }
        }
        return true
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
