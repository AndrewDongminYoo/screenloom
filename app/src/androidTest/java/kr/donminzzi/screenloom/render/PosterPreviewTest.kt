package kr.donminzzi.screenloom.render

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import kr.donminzzi.screenloom.editor.LayoutMode
import kr.donminzzi.screenloom.editor.PaletteId
import kr.donminzzi.screenloom.editor.ShadowLevel
import androidx.compose.ui.unit.IntSize
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
        val sources = List(2) { Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888) }
        try {
            compose.setContent {
                PosterPreview(
                    document = EditorDocument(
                        imageCount = 2,
                        layout = LayoutMode.Split,
                        title = "Ship beautifully",
                        subtitle = "Store-ready visuals in seconds.",
                        palette = PaletteId.Cobalt,
                    ),
                    images = sources.map(Bitmap::asImageBitmap),
                )
            }

            val previewDescription = context.getString(
                R.string.poster_preview_description,
                context.getString(R.string.layout_split),
                context.resources.getQuantityString(R.plurals.poster_preview_screenshot_count, 2, 2),
                context.getString(R.string.palette_cobalt),
            )
            compose.onNodeWithContentDescription(previewDescription).assertIsDisplayed()
            compose.onNodeWithText("Ship beautifully").assertDoesNotExist()
            compose.onNodeWithText("Store-ready visuals in seconds.").assertDoesNotExist()
        } finally {
            sources.forEach(Bitmap::recycle)
        }
    }

    @Test
    fun focusWithTwoSelectedImagesDescribesOneVisibleScreenshot() {
        assertFocusDescription(Locale.US)
    }

    @Test
    fun koreanFocusWithTwoSelectedImagesDescribesOneVisibleScreenshot() {
        assertFocusDescription(Locale.forLanguageTag("ko-KR"))
    }

    @Test
    fun everySunlitPaletteMatchesTheExportAtDecorationSamples() {
        var document by mutableStateOf(EditorDocument())
        compose.setContent {
            Box(Modifier.width(270.dp).testTag("preview-capture")) {
                PosterPreview(document = document, images = emptyList())
            }
        }

        val baseSample = 108 to 192
        val sunSample = 886 to 346
        val ribbonSamples = listOf(540 to 925, 540 to 1130)
        PaletteId.entries.forEach { paletteId ->
            compose.runOnIdle { document = EditorDocument(palette = paletteId) }
            val preview = compose.onNodeWithTag("preview-capture").captureToImage()
            val export = PosterRenderer().render(document, emptyList(), 1080, 1920)
            try {
                listOf(baseSample, sunSample).plus(ribbonSamples).forEach { (x, y) ->
                    assertPixelChannelsWithinTolerance(preview, export, x, y, tolerance = 8)
                }
                ribbonSamples.forEach { (x, y) ->
                    assertColorDiffersFromGradient(export.getPixel(x, y), paletteId.colors(), x, y)
                }
            } finally {
                export.recycle()
            }
        }
    }

    @Test
    fun cobaltAndVioletCopyZonesMakeWarmWhiteCopyLegibleAndStayAbsentWhenCopyIsBlank() {
        var document by mutableStateOf(EditorDocument())
        compose.setContent {
            Box(Modifier.width(270.dp).testTag("preview-capture")) {
                PosterPreview(document = document, images = emptyList())
            }
        }

        val copySamples = listOf(886 to 346, 990 to 450)
        listOf(PaletteId.Cobalt, PaletteId.Violet).forEach { paletteId ->
            compose.runOnIdle { document = EditorDocument(palette = paletteId, title = "I") }
            val preview = compose.onNodeWithTag("preview-capture").captureToImage()
            val export = PosterRenderer().render(document, emptyList(), 1080, 1920)
            try {
                copySamples.forEach { (x, y) ->
                    val contrast = contrastRatio(export.getPixel(x, y), 0xFFFFF8E9.toInt())
                    assertTrue("$paletteId copy-zone contrast at ($x, $y) is $contrast", contrast >= 4.5)
                    assertPixelChannelsWithinTolerance(preview, export, x, y, tolerance = 8)
                }
            } finally {
                export.recycle()
            }

            compose.runOnIdle { document = EditorDocument(palette = paletteId) }
            val blankPreview = compose.onNodeWithTag("preview-capture").captureToImage()
            val blankExport = PosterRenderer().render(document, emptyList(), 1080, 1920)
            try {
                val blankSample = if (paletteId == PaletteId.Cobalt) 886 to 346 else 990 to 450
                assertColorChannelsWithinTolerance(
                    label = "$paletteId blank copy must leave its existing decoration path uncovered",
                    expected = expectedUndarkenedBackground(paletteId, blankSample.first, blankSample.second),
                    actual = blankExport.getPixel(blankSample.first, blankSample.second),
                    tolerance = 1,
                )
                assertPixelChannelsWithinTolerance(
                    preview = blankPreview,
                    export = blankExport,
                    exportX = blankSample.first,
                    exportY = blankSample.second,
                    tolerance = 8,
                )
            } finally {
                blankExport.recycle()
            }
        }
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

        // Absolute row agreement is the property that matters and it is the tighter check: the
        // preview quantises each ink-top to its own 709 px grid, so a single row is worth about
        // 1.5 reference units. Every row below was measured within 1.0 after the copy block moved
        // onto explicit baselines.
        listOf(
            "first line" to (previewFirstLineTop to exportFirstLineTop),
            "second line" to (previewSecondLineTop to exportSecondLineTop),
            "subtitle" to (previewSubtitleTop to exportSubtitleTop),
        ).forEach { (label, rows) ->
            val (previewRow, exportRow) = rows
            assertTrue(
                "Preview $label row is $previewRow but export row is $exportRow",
                abs(previewRow - exportRow) <= 2f,
            )
        }
        // The two checks below subtract one quantised row from another, so they carry twice the
        // preview's rounding. They stay as structural checks; the absolute rows above are the gate.
        assertTrue(
            "Preview line spacing is $previewLineSpacing but export spacing is $exportLineSpacing",
            abs(previewLineSpacing - exportLineSpacing) <= 3f,
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
        // Sample 40px outside the frame's left edge: past Soft's 20px spread, inside Strong's 52px.
        // Derived from the placement because the frame now follows the source's aspect ratio, so a
        // fixed coordinate stops straddling that boundary the moment the geometry changes.
        val frame = PosterLayout.imagePlacements(
            IntSize(1080, 1920),
            LayoutMode.Focus,
            listOf(320f / 640f),
        ).single().placement
        val exportSampleX = (frame.left - 40f).roundToInt()
        val exportSampleY = (frame.top + frame.height / 2f).roundToInt()
        val scale = strongShadow.width / 1080f
        val sampleX = (exportSampleX * scale).roundToInt()
        val sampleY = (exportSampleY * scale).roundToInt()

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
            exportBackground.getPixel(exportSampleX, exportSampleY),
            exportSoftShadow.getPixel(exportSampleX, exportSampleY),
        )
        assertNotEquals(
            exportBackground.getPixel(exportSampleX, exportSampleY),
            exportStrongShadow.getPixel(exportSampleX, exportSampleY),
        )
        val frameSampleX = (frame.left + 8f).roundToInt()
        val frameSampleY = (frame.top + frame.height / 2f).roundToInt()
        val previewFrameX = (frameSampleX * scale).roundToInt()
        val previewFrameY = (frameSampleY * scale).roundToInt()
        val palette = EditorDocument().palette.colors()
        assertEquals(palette.frameColor, exportStrongShadow.getPixel(frameSampleX, frameSampleY))
        assertPixelChannelsWithinTolerance(
            preview = strongShadow,
            export = exportStrongShadow,
            exportX = frameSampleX,
            exportY = frameSampleY,
            tolerance = 8,
        )
        assertPixelChannelsWithinTolerance(
            preview = strongShadow,
            export = exportStrongShadow,
            exportX = exportSampleX,
            exportY = exportSampleY,
            tolerance = 8,
        )
        assertTrue(strongShadow.toPixelMap()[previewFrameX, previewFrameY].alpha >= 0.99f)
    }

    @Test
    fun focusToStackShowsAnIntermediateSpringFrame() {
        var document by mutableStateOf(EditorDocument(layout = LayoutMode.Focus))
        val first = solidBitmap(android.graphics.Color.RED)
        val second = solidBitmap(android.graphics.Color.GREEN)
        var previewImages by mutableStateOf(listOf(first.asImageBitmap(), second.asImageBitmap()))
        try {
            compose.setContent {
                Box(Modifier.width(270.dp).testTag("preview-capture")) {
                    PosterPreview(
                        document = document,
                        images = previewImages,
                    )
                }
            }

            compose.mainClock.autoAdvance = false
            val focus = compose.onNodeWithTag("preview-capture").captureToImage()
            compose.runOnIdle { document = document.copy(layout = LayoutMode.Stack) }
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeBy(32)
            val intermediate = compose.onNodeWithTag("preview-capture").captureToImage()
            compose.mainClock.advanceTimeBy(5_000)
            val stack = compose.onNodeWithTag("preview-capture").captureToImage()

            val focusRed = redDominantCentroid(focus)
            val intermediateRed = redDominantCentroid(intermediate)
            val stackRed = redDominantCentroid(stack)
            assertCoordinateStrictlyBetween(
                label = "red screenshot x",
                start = focusRed.x,
                intermediate = intermediateRed.x,
                end = stackRed.x,
                tolerance = 0.5f,
            )
            assertCoordinateStrictlyBetween(
                label = "red screenshot y",
                start = focusRed.y,
                intermediate = intermediateRed.y,
                end = stackRed.y,
                tolerance = 0.5f,
            )
        } finally {
            compose.mainClock.autoAdvance = true
            compose.runOnIdle { previewImages = emptyList() }
            first.recycle()
            second.recycle()
        }
    }

    @Test
    fun constrainedCanvasResizeShowsAnIntermediateImagePosition() {
        val source = solidBitmap(android.graphics.Color.RED)
        var previewImages by mutableStateOf(listOf(source.asImageBitmap()))
        var previewWidth by mutableStateOf(270.dp)
        try {
            compose.setContent {
                Box(
                    Modifier
                        .width(300.dp)
                        .height(540.dp)
                        .testTag("preview-capture"),
                ) {
                    PosterPreview(
                        document = EditorDocument(layout = LayoutMode.Focus),
                        images = previewImages,
                        modifier = Modifier.width(previewWidth),
                    )
                }
            }

            compose.mainClock.autoAdvance = false
            val wide = compose.onNodeWithTag("preview-capture").captureToImage()
            compose.runOnIdle { previewWidth = 210.dp }
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeBy(32)
            val intermediate = compose.onNodeWithTag("preview-capture").captureToImage()
            compose.mainClock.advanceTimeBy(5_000)
            val narrow = compose.onNodeWithTag("preview-capture").captureToImage()

            assertCoordinateStrictlyBetween(
                label = "resized red screenshot x",
                start = redDominantCentroid(wide).x,
                intermediate = redDominantCentroid(intermediate).x,
                end = redDominantCentroid(narrow).x,
                tolerance = 0.5f,
            )
        } finally {
            compose.mainClock.autoAdvance = true
            compose.runOnIdle { previewImages = emptyList() }
            source.recycle()
        }
    }

    @Test
    fun splitPreviewMatchesRepresentativeExportPixels() {
        val document = EditorDocument(
            imageCount = 2,
            layout = LayoutMode.Split,
            palette = PaletteId.Moss,
            frameEnabled = false,
            shadow = ShadowLevel.Strong,
        )
        val first = solidBitmap(android.graphics.Color.RED)
        val second = solidBitmap(android.graphics.Color.GREEN)
        var previewImages by mutableStateOf(listOf(first.asImageBitmap(), second.asImageBitmap()))
        var export: Bitmap? = null
        try {
            compose.setContent {
                Box(Modifier.width(270.dp).testTag("preview-capture")) {
                    PosterPreview(document = document, images = previewImages)
                }
            }

            val preview = compose.onNodeWithTag("preview-capture").captureToImage()
            val rendered = PosterRenderer().render(document, listOf(first, second), 1080, 1920)
            export = rendered
            assertRedDominant(rendered.getPixel(295, 1115), 295, 1115)
            assertGreenDominant(rendered.getPixel(785, 1165), 785, 1165)
            assertMintFixtureSample(rendered.getPixel(108, 192), 108, 192)
            assertMintFixtureSample(rendered.getPixel(972, 1728), 972, 1728)
            listOf(
                295 to 1115,
                785 to 1165,
                108 to 192,
                972 to 1728,
            ).forEach { (exportX, exportY) ->
                assertPixelChannelsWithinTolerance(
                    preview = preview,
                    export = rendered,
                    exportX = exportX,
                    exportY = exportY,
                    tolerance = 8,
                )
            }
        } finally {
            compose.runOnIdle { previewImages = emptyList() }
            export?.recycle()
            first.recycle()
            second.recycle()
        }
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

    private fun solidBitmap(color: Int): Bitmap = Bitmap.createBitmap(
        320,
        640,
        Bitmap.Config.ARGB_8888,
    ).apply {
        eraseColor(color)
    }

    private data class PixelCentroid(
        val x: Float,
        val y: Float,
    )

    private fun redDominantCentroid(image: ImageBitmap): PixelCentroid {
        val pixels = image.toPixelMap()
        var xTotal = 0L
        var yTotal = 0L
        var count = 0L
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val color = pixels[x, y]
                if (
                    color.red >= 0.75f &&
                    color.red >= color.green + 0.45f &&
                    color.red >= color.blue + 0.45f &&
                    color.alpha >= 0.9f
                ) {
                    xTotal += x
                    yTotal += y
                    count += 1
                }
            }
        }
        check(count > 0) { "No red-dominant screenshot pixels found" }
        return PixelCentroid(x = xTotal.toFloat() / count, y = yTotal.toFloat() / count)
    }

    private fun assertCoordinateStrictlyBetween(
        label: String,
        start: Float,
        intermediate: Float,
        end: Float,
        tolerance: Float,
    ) {
        val lower = minOf(start, end) + tolerance
        val upper = maxOf(start, end) - tolerance
        assertTrue(
            "$label did not interpolate: start=$start intermediate=$intermediate end=$end",
            intermediate > lower && intermediate < upper,
        )
    }

    private fun assertRedDominant(color: Int, x: Int, y: Int) {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        assertTrue(
            "Export sample ($x, $y) is not meaningfully red: rgba=($red, $green, $blue, ${android.graphics.Color.alpha(color)})",
            red >= 200 && red >= green + 120 && red >= blue + 120 && android.graphics.Color.alpha(color) >= 250,
        )
    }

    private fun assertGreenDominant(color: Int, x: Int, y: Int) {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        assertTrue(
            "Export sample ($x, $y) is not meaningfully green: rgba=($red, $green, $blue, ${android.graphics.Color.alpha(color)})",
            green >= 200 && green >= red + 120 && green >= blue + 120 && android.graphics.Color.alpha(color) >= 250,
        )
    }

    /**
     * The unshadowed Mint background at ([x], [y]).
     *
     * Derived rather than tabulated: the renderer's gradient runs from (0, 0) to (1080, 1920), so
     * the interpolation factor is the point's projection onto that diagonal. The previous
     * hard-coded triples silently encoded whatever shadow happened to fall on the sample, so
     * moving a frame rebased the "background" expectation instead of failing honestly.
     */
    private fun assertMintFixtureSample(color: Int, x: Int, y: Int) {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        val palette = PaletteId.Moss.colors()
        val progress = (x * 1080f + y * 1920f) / (1080f * 1080f + 1920f * 1920f)
        fun channel(component: (Int) -> Int): Int {
            val from = component(palette.startColor)
            return (from + progress * (component(palette.endColor) - from)).roundToInt()
        }
        val expectedRed = channel(android.graphics.Color::red)
        val expectedGreen = channel(android.graphics.Color::green)
        val expectedBlue = channel(android.graphics.Color::blue)
        val tolerance = 4
        assertTrue(
            "Export sample ($x, $y) does not match the Mint fixture: " +
                "rgba=($red, $green, $blue, ${android.graphics.Color.alpha(color)})",
            abs(red - expectedRed) <= tolerance &&
                abs(green - expectedGreen) <= tolerance &&
                abs(blue - expectedBlue) <= tolerance &&
                android.graphics.Color.alpha(color) >= 250,
        )
    }

    private fun assertColorDiffersFromGradient(color: Int, palette: PosterPalette, x: Int, y: Int) {
        val gradient = expectedGradientColor(palette, x, y)
        val distance = abs(android.graphics.Color.red(color) - android.graphics.Color.red(gradient)) +
            abs(android.graphics.Color.green(color) - android.graphics.Color.green(gradient)) +
            abs(android.graphics.Color.blue(color) - android.graphics.Color.blue(gradient))
        assertTrue("Decoration missing at ($x, $y): distance=$distance", distance >= 20)
    }

    private fun expectedGradientColor(palette: PosterPalette, x: Int, y: Int): Int {
        val progress = ((x * 1080f + y * 1920f) / (1080f * 1080f + 1920f * 1920f))
            .coerceIn(0f, 1f)
        fun channel(component: (Int) -> Int): Int {
            val start = component(palette.startColor)
            val end = component(palette.endColor)
            return (start + progress * (end - start)).roundToInt()
        }
        return android.graphics.Color.rgb(
            channel(android.graphics.Color::red),
            channel(android.graphics.Color::green),
            channel(android.graphics.Color::blue),
        )
    }

    private fun assertPixelChannelsWithinTolerance(
        preview: ImageBitmap,
        export: Bitmap,
        exportX: Int,
        exportY: Int,
        tolerance: Int,
    ) {
        val previewX = (exportX * preview.width / 1080f).roundToInt().coerceIn(0, preview.width - 1)
        val previewY = (exportY * preview.height / 1920f).roundToInt().coerceIn(0, preview.height - 1)
        val previewColor = preview.toPixelMap()[previewX, previewY]
        val exportColor = export.getPixel(exportX, exportY)
        val previewChannels = listOf(
            (previewColor.red * 255).roundToInt(),
            (previewColor.green * 255).roundToInt(),
            (previewColor.blue * 255).roundToInt(),
            (previewColor.alpha * 255).roundToInt(),
        )
        val exportChannels = listOf(
            android.graphics.Color.red(exportColor),
            android.graphics.Color.green(exportColor),
            android.graphics.Color.blue(exportColor),
            android.graphics.Color.alpha(exportColor),
        )

        previewChannels.zip(exportChannels).forEachIndexed { channelIndex, (previewChannel, exportChannel) ->
            assertTrue(
                "Pixel ($exportX, $exportY) channel $channelIndex differs: " +
                    "preview=$previewChannel export=$exportChannel",
                abs(previewChannel - exportChannel) <= tolerance,
            )
        }
    }

    private fun contrastRatio(first: Int, second: Int): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun component(shift: Int): Double {
            val encoded = ((color ushr shift) and 0xFF) / 255.0
            return if (encoded <= 0.04045) encoded / 12.92 else ((encoded + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * component(16) + 0.7152 * component(8) + 0.0722 * component(0)
    }

    private fun expectedUndarkenedBackground(paletteId: PaletteId, x: Int, y: Int): Int {
        val (start, end, sun) = when (paletteId) {
            PaletteId.Cobalt -> Triple(0xFF3557F0.toInt(), 0xFF78DBEF.toInt(), 0xFFFFF8E9.toInt())
            PaletteId.Violet -> Triple(0xFF5D50D8.toInt(), 0xFFF3A1C7.toInt(), 0xFFFFD466.toInt())
            else -> error("Only copy-zone palettes are covered")
        }
        val progress = ((x * 1080f + y * 1920f) / (1080f * 1080f + 1920f * 1920f)).coerceIn(0f, 1f)
        fun gradientChannel(component: (Int) -> Int): Int =
            (component(start) + progress * (component(end) - component(start))).roundToInt()
        var red = gradientChannel(android.graphics.Color::red)
        var green = gradientChannel(android.graphics.Color::green)
        var blue = gradientChannel(android.graphics.Color::blue)
        val sunCenterX = 0.82f * 1080f
        val sunCenterY = 0.18f * 1920f
        if ((x - sunCenterX) * (x - sunCenterX) + (y - sunCenterY) * (y - sunCenterY) <= 240f * 240f) {
            val alpha = 176f / 255f
            red = (android.graphics.Color.red(sun) * alpha + red * (1f - alpha)).roundToInt()
            green = (android.graphics.Color.green(sun) * alpha + green * (1f - alpha)).roundToInt()
            blue = (android.graphics.Color.blue(sun) * alpha + blue * (1f - alpha)).roundToInt()
        }
        return android.graphics.Color.rgb(red, green, blue)
    }

    private fun assertColorChannelsWithinTolerance(label: String, expected: Int, actual: Int, tolerance: Int) {
        val differences = listOf(
            abs(android.graphics.Color.red(expected) - android.graphics.Color.red(actual)),
            abs(android.graphics.Color.green(expected) - android.graphics.Color.green(actual)),
            abs(android.graphics.Color.blue(expected) - android.graphics.Color.blue(actual)),
        )
        assertTrue("$label: expected=$expected actual=$actual differences=$differences", differences.all { it <= tolerance })
    }

    @Test
    fun copyBlockLandsAtTheSameHeightInPreviewAndExportForEveryScript() {
        // Latin already agrees; Hangul reaches the copy block through a fallback face, and the
        // export path's line boxes do not follow that fallback while the preview's do.
        var document by mutableStateOf(EditorDocument())
        compose.setContent {
            Box(Modifier.width(270.dp).testTag("preview-capture")) {
                PosterPreview(document = document, images = emptyList())
            }
        }
        listOf(
            "latin" to EditorDocument(
                title = "MAKE IT\nLOOK LAUNCHED.",
                subtitle = "From screenshot to storefront.",
            ),
            "hangul" to EditorDocument(
                title = "출시한 앱처럼\n보이게.",
                subtitle = "스크린샷에서 스토어까지.",
            ),
            // A 60-code-point title overflows two lines at 78 units across a 900-unit measure, so
            // this case runs the ellipsised path that drawPosterCopy still delegates to
            // StaticLayout.draw.
            "overflowing" to EditorDocument(
                title = "Launch your app with visuals that look designed not exported",
                subtitle = "Every promotional frame, straight from the screenshots you already have.",
            ),
        ).forEach { (script, candidate) ->
            compose.runOnIdle { document = candidate }
            compose.waitForIdle()
            val preview = compose.onNodeWithTag("preview-capture").captureToImage()
            val export = PosterRenderer().render(candidate, emptyList(), 1080, 1920)
            try {
                val previewBox = normalizedCopyInkBox(preview.toPixelMap(), preview.width, preview.height)
                val exportBox = normalizedCopyInkBox(export)
                listOf(
                    "top" to abs(previewBox.first - exportBox.first),
                    "bottom" to abs(previewBox.second - exportBox.second),
                ).forEach { (edge, difference) ->
                    assertTrue(
                        "$script copy block $edge differs by $difference of poster height " +
                            "(preview $previewBox, export $exportBox)",
                        difference <= COPY_BLOCK_PARITY_TOLERANCE,
                    )
                }
            } finally {
                export.recycle()
            }
        }
    }

    /** Normalized top and bottom of the dark copy ink in the upper band, so both scales compare. */
    private fun normalizedCopyInkBox(bitmap: Bitmap): Pair<Float, Float> {
        var minY = bitmap.height
        var maxY = -1
        val bandBottom = (bitmap.height * COPY_BLOCK_BAND_FRACTION).toInt()
        for (y in 0 until bandBottom) {
            for (x in 0 until bitmap.width) {
                if (isCopyInk(bitmap.getPixel(x, y))) {
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        assertTrue("no copy ink found in the upper band", maxY >= 0)
        return minY.toFloat() / bitmap.height to maxY.toFloat() / bitmap.height
    }

    private fun normalizedCopyInkBox(
        pixels: androidx.compose.ui.graphics.PixelMap,
        width: Int,
        height: Int,
    ): Pair<Float, Float> {
        var minY = height
        var maxY = -1
        val bandBottom = (height * COPY_BLOCK_BAND_FRACTION).toInt()
        for (y in 0 until bandBottom) {
            for (x in 0 until width) {
                if (isCopyInk(pixels[x, y].toArgb())) {
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        assertTrue("no copy ink found in the upper band", maxY >= 0)
        return minY.toFloat() / height to maxY.toFloat() / height
    }

    private fun isCopyInk(pixel: Int): Boolean {
        val luminance = (
            android.graphics.Color.red(pixel) +
                android.graphics.Color.green(pixel) +
                android.graphics.Color.blue(pixel)
            ) / 3
        return luminance < COPY_INK_MAX_LUMINANCE
    }

    private companion object {
        /** Latin agrees within 0.0015 today; Hangul diverges by 0.0092 and 0.0151. */
        const val COPY_BLOCK_PARITY_TOLERANCE = 0.004f
        const val COPY_BLOCK_BAND_FRACTION = 0.30f
        const val COPY_INK_MAX_LUMINANCE = 110
    }

    private fun assertFocusDescription(locale: Locale) {
        val configuration = Configuration(
            InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration,
        ).apply {
            setLocale(locale)
        }
        val localizedContext = InstrumentationRegistry.getInstrumentation().targetContext
            .createConfigurationContext(configuration)
        val sources = List(2) { Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888) }
        try {
            compose.setContent {
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides configuration,
                ) {
                    PosterPreview(
                        document = EditorDocument(imageCount = 2, layout = LayoutMode.Focus),
                        images = sources.map(Bitmap::asImageBitmap),
                    )
                }
            }

            val previewDescription = localizedContext.getString(
                R.string.poster_preview_description,
                localizedContext.getString(R.string.layout_focus),
                localizedContext.resources.getQuantityString(
                    R.plurals.poster_preview_screenshot_count,
                    1,
                    1,
                ),
                localizedContext.getString(R.string.palette_ink),
            )
            compose.onNodeWithContentDescription(previewDescription).assertIsDisplayed()
        } finally {
            sources.forEach(Bitmap::recycle)
        }
    }
}
