package io.github.andrewdongminyoo.screenloom.render

import androidx.compose.ui.unit.IntSize
import io.github.andrewdongminyoo.screenloom.editor.LayoutMode

data class PosterPlacement(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float,
)

object PosterLayout {
    private const val ExportWidth = 1080f
    private const val ExportHeight = 1920f

    fun placements(
        canvasSize: IntSize,
        layout: LayoutMode,
        imageCount: Int,
    ): List<PosterPlacement> {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || imageCount <= 0) return emptyList()

        val source = when (layout) {
            LayoutMode.Focus -> listOf(focusPlacement)
            LayoutMode.Stack -> if (imageCount >= 2) stackPlacements else listOf(stackPlacements.last())
            LayoutMode.Split -> if (imageCount >= 2) splitPlacements else listOf(focusPlacement)
        }
        val scaleX = canvasSize.width / ExportWidth
        val scaleY = canvasSize.height / ExportHeight
        return source.take(imageCount.coerceAtMost(2)).map { placement ->
            placement.copy(
                left = placement.left * scaleX,
                top = placement.top * scaleY,
                width = placement.width * scaleX,
                height = placement.height * scaleY,
            )
        }
    }

    private val focusPlacement = PosterPlacement(
        left = 190f,
        top = 580f,
        width = 700f,
        height = 1080f,
        rotationDegrees = 0f,
    )

    private val stackPlacements = listOf(
        PosterPlacement(
            left = 330f,
            top = 620f,
            width = 620f,
            height = 980f,
            rotationDegrees = 6f,
        ),
        PosterPlacement(
            left = 130f,
            top = 680f,
            width = 650f,
            height = 1010f,
            rotationDegrees = -6f,
        ),
    )

    private val splitPlacements = listOf(
        PosterPlacement(
            left = 75f,
            top = 600f,
            width = 440f,
            height = 1030f,
            rotationDegrees = -2f,
        ),
        PosterPlacement(
            left = 565f,
            top = 650f,
            width = 440f,
            height = 1030f,
            rotationDegrees = 2f,
        ),
    )
}
