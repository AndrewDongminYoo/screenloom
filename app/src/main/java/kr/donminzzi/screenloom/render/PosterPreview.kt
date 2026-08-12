package kr.donminzzi.screenloom.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.editor.EditorDocument
import kr.donminzzi.screenloom.editor.LayoutMode
import kr.donminzzi.screenloom.editor.ShadowLevel
import kotlin.math.roundToInt

@Composable
fun PosterPreview(
    document: EditorDocument,
    images: List<ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val previewDescription = stringResource(R.string.poster_preview_description)
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clearAndSetSemantics { contentDescription = previewDescription },
    ) {
        val posterDensity = Density(LocalDensity.current.density, fontScale = 1f)
        val horizontalPadding = (maxWidth.value * 90f / 1080f).dp
        val topPadding = (maxWidth.value * 150f / 1080f).dp
        val titleSize = (maxWidth.value * 78f / 1080f).sp
        val titleLineHeight = (maxWidth.value * 86f / 1080f).sp
        val subtitleSize = (maxWidth.value * 32f / 1080f).sp
        val subtitleLineHeight = (maxWidth.value * 42f / 1080f).sp
        val subtitleTopPadding = (maxWidth.value * 22f / 1080f).dp
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val palette = document.palette.colors()
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(palette.startColor), Color(palette.endColor)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
            )
            drawCircle(
                color = Color(palette.accentColor).copy(alpha = 0.12f),
                radius = size.width * 0.22f,
                center = Offset(size.width * 0.82f, size.height * 0.18f),
            )
            repeat(9) { row ->
                repeat(6) { column ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.width * 0.002f,
                        center = Offset(
                            x = size.width * ((82f + column * 184f + (row % 2) * 36f) / 1080f),
                            y = size.height * ((120f + row * 210f) / 1920f),
                        ),
                    )
                }
            }
            val placements = PosterLayout.placements(
                canvasSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                layout = document.layout,
                imageCount = images.size,
            )
            val orderedImages = if (document.layout == LayoutMode.Stack && images.size >= 2) {
                listOf(images[1], images[0])
            } else {
                images
            }
            placements.zip(orderedImages).forEach { (placement, image) ->
                drawPreviewImage(placement, image, document.frameEnabled, document.shadow)
            }
        }
        CompositionLocalProvider(LocalDensity provides posterDensity) {
            Column(
                modifier = Modifier.padding(
                    start = horizontalPadding,
                    top = topPadding,
                    end = horizontalPadding,
                ),
            ) {
                if (document.title.isNotBlank()) {
                    Text(
                        text = document.title,
                        color = Color(0xFFF5F1E8),
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = titleLineHeight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (document.subtitle.isNotBlank()) {
                    Text(
                        text = document.subtitle,
                        modifier = Modifier.padding(
                            top = if (document.title.isNotBlank()) subtitleTopPadding else 0.dp,
                        ),
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = subtitleSize,
                        lineHeight = subtitleLineHeight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewImage(
    placement: PosterPlacement,
    image: ImageBitmap,
    frameEnabled: Boolean,
    shadowLevel: ShadowLevel,
) {
    val topLeft = Offset(placement.left, placement.top)
    val targetSize = Size(placement.width, placement.height)
    val corner = size.width * (42f / 1080f)
    val shadowSpec = shadowLevel.posterShadowSpec(size.width / 1080f)
    rotate(placement.rotationDegrees, pivot = topLeft + Offset(targetSize.width / 2f, targetSize.height / 2f)) {
        for (layerIndex in POSTER_SHADOW_LAYER_COUNT downTo 1) {
            val expansion = shadowSpec.expansion(layerIndex)
            drawRoundRect(
                color = Color.Black.copy(alpha = shadowSpec.layerAlpha(layerIndex) / 255f),
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
                color = Color(0xFF14171E),
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
            )
        }
    }
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
