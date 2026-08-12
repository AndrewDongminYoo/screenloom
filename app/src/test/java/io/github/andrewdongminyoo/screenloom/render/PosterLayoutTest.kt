package io.github.andrewdongminyoo.screenloom.render

import androidx.compose.ui.unit.IntSize
import io.github.andrewdongminyoo.screenloom.editor.LayoutMode
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
    fun splitNeverProducesMorePlacementsThanImages() {
        assertEquals(1, PosterLayout.placements(output, LayoutMode.Split, imageCount = 1).size)
        assertEquals(2, PosterLayout.placements(output, LayoutMode.Split, imageCount = 2).size)
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
}
