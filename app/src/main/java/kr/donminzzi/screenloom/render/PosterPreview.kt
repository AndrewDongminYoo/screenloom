package kr.donminzzi.screenloom.render

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.editor.EditorDocument
import kr.donminzzi.screenloom.editor.LayoutMode
import kr.donminzzi.screenloom.editor.PaletteId
import kr.donminzzi.screenloom.editor.ShadowLevel
import kotlin.math.roundToInt

private const val PosterImageAlphaDurationMillis = 150

private data class PosterPreviewTarget(
    val canvasSize: IntSize,
    val layout: LayoutMode,
    val imageAspectRatios: List<Float>,
) {
    private val imagePlacements = PosterLayout.imagePlacements(canvasSize, layout, imageAspectRatios)

    fun placementFor(imageIndex: Int): PosterPlacement = imagePlacements
        .firstOrNull { it.imageIndex == imageIndex }
        ?.placement
        ?: PosterLayout.imagePlacements(
            canvasSize,
            LayoutMode.Focus,
            imageAspectRatios.take(1),
        ).singleOrNull()
            ?.placement
        ?: PosterPlacement(0f, 0f, 0f, 0f, 0f)

    fun alphaFor(imageIndex: Int): Float = if (imagePlacements.any { it.imageIndex == imageIndex }) 1f else 0f

    fun drawOrder(): List<Int> = buildList {
        addAll(imagePlacements.map { it.imageIndex })
        addAll((0 until imageAspectRatios.size.coerceAtMost(2)).filterNot(::contains))
    }
}

private data class AnimatedPosterImage(
    val imageIndex: Int,
    val placement: PosterPlacement,
    val alpha: Float,
)

@Composable
fun PosterPreview(
    document: EditorDocument,
    images: List<ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val layoutLabel = stringResource(
        when (document.layout) {
            LayoutMode.Focus -> R.string.layout_focus
            LayoutMode.Stack -> R.string.layout_stack
            LayoutMode.Split -> R.string.layout_split
        },
    )
    val paletteLabel = stringResource(
        when (document.palette) {
            PaletteId.Ink -> R.string.palette_ink
            PaletteId.Cobalt -> R.string.palette_cobalt
            PaletteId.Coral -> R.string.palette_coral
            PaletteId.Moss -> R.string.palette_moss
            PaletteId.Violet -> R.string.palette_violet
            PaletteId.Sunrise -> R.string.palette_sunrise
        },
    )
    val visibleImageCount = PosterLayout.visibleImageCount(
        layout = document.layout,
        selectedCount = images.size,
    )
    val screenshotCount = pluralStringResource(
        R.plurals.poster_preview_screenshot_count,
        visibleImageCount,
        visibleImageCount,
    )
    val previewDescription = stringResource(
        R.string.poster_preview_description,
        layoutLabel,
        screenshotCount,
        paletteLabel,
    )
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clearAndSetSemantics { contentDescription = previewDescription },
    ) {
        val canvasSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val transition = updateTransition(
            targetState = PosterPreviewTarget(
                canvasSize = canvasSize,
                layout = document.layout,
                imageAspectRatios = images.map { image -> image.width.toFloat() / image.height },
            ),
            label = "poster placements",
        )
        val animatedImages = (0 until images.size.coerceAtMost(2)).map { imageIndex ->
            key(imageIndex) {
                val left by transition.animateFloat(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    },
                    label = "poster image $imageIndex left",
                ) { target -> target.placementFor(imageIndex).left }
                val top by transition.animateFloat(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    },
                    label = "poster image $imageIndex top",
                ) { target -> target.placementFor(imageIndex).top }
                val width by transition.animateFloat(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    },
                    label = "poster image $imageIndex width",
                ) { target -> target.placementFor(imageIndex).width }
                val height by transition.animateFloat(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    },
                    label = "poster image $imageIndex height",
                ) { target -> target.placementFor(imageIndex).height }
                val rotationDegrees by transition.animateFloat(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    },
                    label = "poster image $imageIndex rotation",
                ) { target -> target.placementFor(imageIndex).rotationDegrees }
                val alpha by transition.animateFloat(
                    transitionSpec = { tween(durationMillis = PosterImageAlphaDurationMillis) },
                    label = "poster image $imageIndex alpha",
                ) { target -> target.alphaFor(imageIndex) }
                AnimatedPosterImage(
                    imageIndex = imageIndex,
                    placement = PosterPlacement(left, top, width, height, rotationDegrees),
                    alpha = alpha,
                )
            }
        }
        val animatedImagesByIndex = animatedImages.associateBy { it.imageIndex }
        val palette = document.palette.colors()
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(palette.startColor), Color(palette.endColor)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
            )
            drawCircle(
                color = Color(palette.sunColor).copy(alpha = POSTER_SUN_ALPHA / 255f),
                radius = 240f * size.width / 1080f,
                center = Offset(size.width * 0.82f, size.height * 0.18f),
            )
            PosterRibbonSpecs.zip(listOf(palette.ribbonOneColor, palette.ribbonTwoColor)).forEach { (spec, color) ->
                drawPreviewRibbon(spec, color)
            }
            if (document.title.isNotBlank() || document.subtitle.isNotBlank()) {
                palette.copyZoneColor?.let { drawPreviewCopyZone(it) }
            }
            transition.targetState.drawOrder().forEach { imageIndex ->
                val animatedImage = animatedImagesByIndex.getValue(imageIndex)
                drawPreviewImage(
                    placement = animatedImage.placement,
                    image = images[imageIndex],
                    frameEnabled = document.frameEnabled,
                    shadowLevel = document.shadow,
                    frameColor = palette.frameColor,
                    shadowColor = palette.shadowColor,
                    alpha = animatedImage.alpha,
                )
            }
            // The copy block goes through the exporter's own function so preview and export
            // cannot drift apart. See drawPosterCopy in PosterRenderer.kt.
            drawIntoCanvas { canvas ->
                drawPosterCopy(
                    canvas = canvas.nativeCanvas,
                    document = document,
                    palette = palette,
                    scale = size.width / POSTER_REFERENCE_WIDTH,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewImage(
    placement: PosterPlacement,
    image: ImageBitmap,
    frameEnabled: Boolean,
    shadowLevel: ShadowLevel,
    frameColor: Int,
    shadowColor: Int,
    alpha: Float,
) {
    if (alpha <= 0f) return
    val topLeft = Offset(placement.left, placement.top)
    val targetSize = Size(placement.width, placement.height)
    val corner = size.width * (42f / 1080f)
    val shadowSpec = shadowLevel.posterShadowSpec(size.width / 1080f)
    rotate(placement.rotationDegrees, pivot = topLeft + Offset(targetSize.width / 2f, targetSize.height / 2f)) {
        for (layerIndex in POSTER_SHADOW_LAYER_COUNT downTo 1) {
            val expansion = shadowSpec.expansion(layerIndex)
            drawRoundRect(
                color = Color(shadowColor).copy(alpha = shadowSpec.layerAlpha(layerIndex) / 255f * alpha),
                topLeft = topLeft + Offset(-expansion, shadowSpec.offsetY - expansion),
                size = Size(
                    width = targetSize.width + expansion * 2f,
                    height = targetSize.height + expansion * 2f,
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner + expansion),
            )
        }
        if (frameEnabled) {
            drawRoundRect(
                color = Color(frameColor).copy(alpha = alpha),
                topLeft = topLeft,
                size = targetSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner),
            )
        }
        val inset = if (frameEnabled) size.width * (16f / 1080f) else 0f
        val screen = Rect(
            left = topLeft.x + inset,
            top = topLeft.y + inset,
            right = topLeft.x + targetSize.width - inset,
            bottom = topLeft.y + targetSize.height - inset,
        )
        val clip = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = screen,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius((corner - inset).coerceAtLeast(4f)),
                ),
            )
        }
        clipPath(clip) {
            val source = centerCrop(image, screen.size)
            drawImage(
                image = image,
                srcOffset = source.first,
                srcSize = source.second,
                dstOffset = IntOffset(screen.left.roundToInt(), screen.top.roundToInt()),
                dstSize = IntSize(screen.width.roundToInt(), screen.height.roundToInt()),
                alpha = alpha,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewRibbon(
    spec: PosterRibbonSpec,
    color: Int,
) {
    val scale = size.width / 1080f
    val topLeft = Offset(spec.left * scale, spec.top * scale)
    val ribbonSize = Size(spec.width * scale, spec.height * scale)
    rotate(
        degrees = spec.rotationDegrees,
        pivot = topLeft + Offset(ribbonSize.width / 2f, ribbonSize.height / 2f),
    ) {
        drawRoundRect(
            color = Color(color).copy(alpha = POSTER_RIBBON_ALPHA / 255f),
            topLeft = topLeft,
            size = ribbonSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(ribbonSize.height / 2f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewCopyZone(color: Int) {
    val scale = size.width / 1080f
    drawRoundRect(
        color = Color(color).copy(alpha = POSTER_COPY_ZONE_ALPHA / 255f),
        topLeft = Offset(PosterCopyZone.left * scale, PosterCopyZone.top * scale),
        size = Size(
            width = (PosterCopyZone.right - PosterCopyZone.left) * scale,
            height = (PosterCopyZone.bottom - PosterCopyZone.top) * scale,
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(PosterCopyZone.cornerRadius * scale),
    )
}

private fun centerCrop(
    image: ImageBitmap,
    target: Size,
): Pair<IntOffset, IntSize> {
    val sourceRatio = image.width.toFloat() / image.height
    val targetRatio = target.width / target.height
    return if (sourceRatio > targetRatio) {
        val cropWidth = (image.height * targetRatio).roundToInt()
        IntOffset((image.width - cropWidth) / 2, 0) to IntSize(cropWidth, image.height)
    } else {
        val cropHeight = (image.width / targetRatio).roundToInt()
        IntOffset(0, (image.height - cropHeight) / 2) to IntSize(image.width, cropHeight)
    }
}
