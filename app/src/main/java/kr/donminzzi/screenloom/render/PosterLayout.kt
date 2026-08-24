package kr.donminzzi.screenloom.render

import androidx.compose.ui.unit.IntSize
import kr.donminzzi.screenloom.editor.LayoutMode

data class PosterPlacement(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float,
)

data class PosterImagePlacement(
    val imageIndex: Int,
    val placement: PosterPlacement,
)

object PosterLayout {
    private const val ExportWidth = 1080f
    private const val ExportHeight = 1920f

    internal fun visibleImageCount(
        layout: LayoutMode,
        selectedCount: Int,
    ): Int = when (layout) {
        LayoutMode.Focus -> selectedCount.coerceIn(0, 1)
        LayoutMode.Stack,
        LayoutMode.Split,
        -> selectedCount.coerceIn(0, 2)
    }

    fun placements(
        canvasSize: IntSize,
        layout: LayoutMode,
        imageCount: Int,
    ): List<PosterPlacement> {
        val visibleImageCount = visibleImageCount(layout, imageCount)
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || visibleImageCount == 0) return emptyList()

        val source = when (layout) {
            LayoutMode.Focus -> listOf(focusPlacement)
            LayoutMode.Stack -> if (visibleImageCount == 2) stackPlacements else listOf(stackPlacements.last())
            LayoutMode.Split -> if (visibleImageCount == 2) splitPlacements else listOf(focusPlacement)
        }
        val scaleX = canvasSize.width / ExportWidth
        val scaleY = canvasSize.height / ExportHeight
        return source.take(visibleImageCount).map { placement ->
            placement.copy(
                left = placement.left * scaleX,
                top = placement.top * scaleY,
                width = placement.width * scaleX,
                height = placement.height * scaleY,
            )
        }
    }

    /**
     * Screenshot frames for [layout], one per entry in [imageAspectRatios] (width / height of the
     * source, in that source's own pixels).
     *
     * Each frame is the largest rectangle with the source's aspect ratio that fits inside the
     * template box, centred on it. The templates are fixed shapes, but imported screenshots are
     * not: a 9:16 phone capture dropped into `Split`'s 440x1030 box used to lose 24% of its width
     * to the centre crop, which reads as the app mangling the screenshot rather than presenting
     * it. Fitting here keeps the whole screenshot visible for any source ratio, and leaves
     * `centerCrop` in both renderers as a no-op safety net.
     */
    fun imagePlacements(
        canvasSize: IntSize,
        layout: LayoutMode,
        imageAspectRatios: List<Float>,
    ): List<PosterImagePlacement> {
        val placements = placements(canvasSize, layout, imageAspectRatios.size)
        val ordered = if (layout == LayoutMode.Stack && placements.size >= 2) {
            listOf(
                PosterImagePlacement(imageIndex = 1, placement = placements[0]),
                PosterImagePlacement(imageIndex = 0, placement = placements[1]),
            )
        } else {
            placements.mapIndexed { index, placement ->
                PosterImagePlacement(imageIndex = index, placement = placement)
            }
        }
        return ordered.map { entry ->
            entry.copy(
                placement = entry.placement.fittedTo(
                    imageAspectRatios.getOrNull(entry.imageIndex) ?: 0f,
                ),
            )
        }
    }

    private fun PosterPlacement.fittedTo(aspectRatio: Float): PosterPlacement {
        if (aspectRatio <= 0f || width <= 0f || height <= 0f) return this
        val boxRatio = width / height
        val fittedWidth = if (aspectRatio > boxRatio) width else height * aspectRatio
        val fittedHeight = if (aspectRatio > boxRatio) width / aspectRatio else height
        return copy(
            left = left + (width - fittedWidth) / 2f,
            top = top + (height - fittedHeight) / 2f,
            width = fittedWidth,
            height = fittedHeight,
        )
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
