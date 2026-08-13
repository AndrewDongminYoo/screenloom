package kr.donminzzi.screenloom.render

import androidx.compose.ui.unit.IntSize
import kr.donminzzi.screenloom.editor.LayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosterLayoutTest {
    private val output = IntSize(width = 1080, height = 1920)

    @Test
    fun focusProducesOneCenteredPlacement() {
        val result = PosterLayout.placements(output, LayoutMode.Focus, imageCount = 1)

        assertEquals(listOf(PosterPlacement(190f, 580f, 700f, 1080f, 0f)), result)
    }

    @Test
    fun focusWithTwoImagesStillProducesTheSingleFocusPlacement() {
        assertEquals(
            listOf(PosterPlacement(190f, 580f, 700f, 1080f, 0f)),
            PosterLayout.placements(output, LayoutMode.Focus, imageCount = 2),
        )
    }

    @Test
    fun stackProducesTwoOpposingPlacements() {
        val result = PosterLayout.placements(output, LayoutMode.Stack, imageCount = 2)

        assertEquals(
            listOf(
                PosterPlacement(330f, 620f, 620f, 980f, 6f),
                PosterPlacement(130f, 680f, 650f, 1010f, -6f),
            ),
            result,
        )
    }

    @Test
    fun stackWithOneImageUsesTheForegroundStackPlacement() {
        assertEquals(
            listOf(PosterPlacement(130f, 680f, 650f, 1010f, -6f)),
            PosterLayout.placements(output, LayoutMode.Stack, imageCount = 1),
        )
    }

    @Test
    fun splitNeverProducesMorePlacementsThanImages() {
        assertEquals(1, PosterLayout.placements(output, LayoutMode.Split, imageCount = 1).size)
        assertEquals(2, PosterLayout.placements(output, LayoutMode.Split, imageCount = 2).size)
    }

    @Test
    fun splitWithOneImageFallsBackToFocusGeometry() {
        assertEquals(
            listOf(PosterPlacement(190f, 580f, 700f, 1080f, 0f)),
            PosterLayout.placements(output, LayoutMode.Split, imageCount = 1),
        )
    }

    @Test
    fun splitWithTwoImagesUsesBothExactPlacements() {
        assertEquals(
            listOf(
                PosterPlacement(75f, 600f, 440f, 1030f, -2f),
                PosterPlacement(565f, 650f, 440f, 1030f, 2f),
            ),
            PosterLayout.placements(output, LayoutMode.Split, imageCount = 2),
        )
    }

    @Test
    fun previewCoordinatesScaleFromExportCanvas() {
        val result = PosterLayout.placements(
            canvasSize = IntSize(width = 540, height = 960),
            layout = LayoutMode.Focus,
            imageCount = 1,
        )

        assertEquals(listOf(PosterPlacement(95f, 290f, 350f, 540f, 0f)), result)
    }

    @Test
    fun stackMapsOriginalSecondImageBehindOriginalFirstImage() {
        assertEquals(
            listOf(1, 0),
            PosterLayout.imagePlacements(output, LayoutMode.Stack, listOf(Phone, Phone))
                .map(PosterImagePlacement::imageIndex),
        )
    }

    @Test
    fun splitPreservesOriginalImageOrder() {
        assertEquals(
            listOf(0, 1),
            PosterLayout.imagePlacements(output, LayoutMode.Split, listOf(Phone, Phone))
                .map(PosterImagePlacement::imageIndex),
        )
    }

    @Test
    fun focusExposesOnlyOriginalFirstImage() {
        assertEquals(
            listOf(0),
            PosterLayout.imagePlacements(output, LayoutMode.Focus, listOf(Phone, Phone))
                .map(PosterImagePlacement::imageIndex),
        )
    }

    @Test
    fun aFrameNarrowerThanItsBoxKeepsTheFullSourceWidth() {
        // Split's 440x1030 box is 0.427; a 9:16 capture is wider, so the frame loses height,
        // never width. Before this fitting it cost 24% of the screenshot's width.
        val placement = PosterLayout
            .imagePlacements(output, LayoutMode.Split, listOf(Phone, Phone))
            .first()
            .placement

        assertEquals(440f, placement.width, Tolerance)
        assertEquals(440f / Phone, placement.height, Tolerance)
        assertEquals(Phone, placement.width / placement.height, Tolerance)
    }

    @Test
    fun aFrameWiderThanItsBoxKeepsTheFullSourceHeight() {
        val placement = PosterLayout
            .imagePlacements(output, LayoutMode.Focus, listOf(Phone))
            .single()
            .placement

        assertEquals(1080f, placement.height, Tolerance)
        assertEquals(1080f * Phone, placement.width, Tolerance)
        assertEquals(Phone, placement.width / placement.height, Tolerance)
    }

    @Test
    fun everyFittedFrameStaysCentredInsideItsBox() {
        for (layout in LayoutMode.entries) {
            val boxes = PosterLayout.placements(output, layout, imageCount = 2)
            val fitted = PosterLayout.imagePlacements(output, layout, listOf(Phone, Phone))
            for ((index, entry) in fitted.withIndex()) {
                val box = boxes[index]
                val placement = entry.placement
                assertEquals("$layout ratio", Phone, placement.width / placement.height, Tolerance)
                assertTrue("$layout width", placement.width <= box.width + Tolerance)
                assertTrue("$layout height", placement.height <= box.height + Tolerance)
                assertEquals(
                    "$layout centre x",
                    box.left + box.width / 2f,
                    placement.left + placement.width / 2f,
                    Tolerance,
                )
                assertEquals(
                    "$layout centre y",
                    box.top + box.height / 2f,
                    placement.top + placement.height / 2f,
                    Tolerance,
                )
            }
        }
    }

    @Test
    fun anUnknownAspectRatioLeavesTheBoxUntouched() {
        assertEquals(
            PosterLayout.placements(output, LayoutMode.Focus, imageCount = 1).single(),
            PosterLayout.imagePlacements(output, LayoutMode.Focus, listOf(0f)).single().placement,
        )
    }

    private companion object {
        /** A 1080x1920 phone capture, the shape the templates are worst at holding. */
        const val Phone = 1080f / 1920f
        const val Tolerance = 0.01f
    }
}
