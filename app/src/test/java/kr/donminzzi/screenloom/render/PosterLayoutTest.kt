package kr.donminzzi.screenloom.render

import androidx.compose.ui.unit.IntSize
import kr.donminzzi.screenloom.editor.LayoutMode
import org.junit.Assert.assertEquals
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
            listOf(
                PosterImagePlacement(
                    imageIndex = 1,
                    placement = PosterPlacement(330f, 620f, 620f, 980f, 6f),
                ),
                PosterImagePlacement(
                    imageIndex = 0,
                    placement = PosterPlacement(130f, 680f, 650f, 1010f, -6f),
                ),
            ),
            PosterLayout.imagePlacements(output, LayoutMode.Stack, imageCount = 2),
        )
    }

    @Test
    fun splitPreservesOriginalImageOrder() {
        assertEquals(
            listOf(
                PosterImagePlacement(
                    imageIndex = 0,
                    placement = PosterPlacement(75f, 600f, 440f, 1030f, -2f),
                ),
                PosterImagePlacement(
                    imageIndex = 1,
                    placement = PosterPlacement(565f, 650f, 440f, 1030f, 2f),
                ),
            ),
            PosterLayout.imagePlacements(output, LayoutMode.Split, imageCount = 2),
        )
    }

    @Test
    fun focusExposesOnlyOriginalFirstImage() {
        assertEquals(
            listOf(
                PosterImagePlacement(
                    imageIndex = 0,
                    placement = PosterPlacement(190f, 580f, 700f, 1080f, 0f),
                ),
            ),
            PosterLayout.imagePlacements(output, LayoutMode.Focus, imageCount = 2),
        )
    }
}
