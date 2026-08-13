package kr.donminzzi.screenloom.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import kr.donminzzi.screenloom.editor.EditorDocument
import kr.donminzzi.screenloom.editor.PaletteId
import kr.donminzzi.screenloom.editor.ShadowLevel
import kotlin.math.roundToInt

data class PosterPalette(
    val startColor: Int,
    val endColor: Int,
    val accentColor: Int,
)

internal const val POSTER_SHADOW_LAYER_COUNT = 12
internal const val POSTER_TITLE_LINE_HEIGHT = 86f
internal const val POSTER_SUBTITLE_LINE_HEIGHT = 42f

// Shared 0-255 alphas. The preview divides by 255f; both renderers must read these rather
// than hand-converting, which is how the subtitle drifted to 0.78f against the export's 190.
internal const val POSTER_SUBTITLE_ALPHA = 190
internal const val POSTER_GLOW_ALPHA = 30
internal const val POSTER_TEXTURE_ALPHA = 18

internal data class PosterShadowSpec(
    val radius: Float,
    val offsetY: Float,
    val alpha: Int = 150,
) {
    fun expansion(layerIndex: Int): Float = radius * layerIndex / POSTER_SHADOW_LAYER_COUNT

    fun layerAlpha(layerIndex: Int): Int {
        val distance = layerIndex.toFloat() / POSTER_SHADOW_LAYER_COUNT
        return (alpha.toFloat() / POSTER_SHADOW_LAYER_COUNT * (1f - distance * 0.65f))
            .roundToInt()
            .coerceAtLeast(1)
    }
}

internal fun ShadowLevel.posterShadowSpec(scale: Float): PosterShadowSpec = PosterShadowSpec(
    radius = when (this) {
        ShadowLevel.Soft -> 20f
        ShadowLevel.Medium -> 34f
        ShadowLevel.Strong -> 52f
    } * scale,
    offsetY = 18f * scale,
)

fun PaletteId.colors(): PosterPalette = when (this) {
    PaletteId.Ink -> PosterPalette(0xFF0B1020.toInt(), 0xFF243B6B.toInt(), 0xFFFFD166.toInt())
    PaletteId.Cobalt -> PosterPalette(0xFF101B4D.toInt(), 0xFF3457D5.toInt(), 0xFFFFF4E6.toInt())
    PaletteId.Coral -> PosterPalette(0xFF351C35.toInt(), 0xFFF06A6A.toInt(), 0xFFFFE2B8.toInt())
    PaletteId.Moss -> PosterPalette(0xFF10251F.toInt(), 0xFF4D8061.toInt(), 0xFFE9D8A6.toInt())
    PaletteId.Violet -> PosterPalette(0xFF1C1338.toInt(), 0xFF7B5BC7.toInt(), 0xFFFFB4A2.toInt())
    PaletteId.Sunrise -> PosterPalette(0xFF3A1C2E.toInt(), 0xFFF28C54.toInt(), 0xFFFFE8C2.toInt())
}

class PosterRenderer {
    fun render(
        document: EditorDocument,
        images: List<Bitmap>,
        width: Int,
        height: Int,
    ): Bitmap {
        require(width > 0 && height > 0) { "Poster dimensions must be positive" }
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val scale = width / 1080f
        drawBackground(canvas, document.palette.colors(), width, height, scale)
        drawCopy(canvas, document, width, scale)
        drawScreenshots(canvas, document, images, width, height, scale)
        return output
    }

    private fun drawBackground(
        canvas: Canvas,
        palette: PosterPalette,
        width: Int,
        height: Int,
        scale: Float,
    ) {
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                palette.startColor,
                palette.endColor,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentColor
            alpha = POSTER_GLOW_ALPHA
        }
        canvas.drawCircle(width * 0.82f, height * 0.18f, 240f * scale, glow)

        val texture = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = POSTER_TEXTURE_ALPHA
        }
        repeat(9) { row ->
            repeat(6) { column ->
                val x = (82f + column * 184f + (row % 2) * 36f) * scale
                val y = (120f + row * 210f) * scale
                canvas.drawCircle(x, y, 2.2f * scale, texture)
            }
        }
    }

    private fun drawCopy(
        canvas: Canvas,
        document: EditorDocument,
        width: Int,
        scale: Float,
    ) {
        if (document.title.isBlank() && document.subtitle.isBlank()) return
        var nextY = 150f * scale
        if (document.title.isNotBlank()) {
            val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(245, 241, 232)
                textSize = 78f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val layout = StaticLayout.Builder.obtain(
                document.title,
                0,
                document.title.length,
                title,
                (width - 180f * scale).roundToInt(),
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(POSTER_TITLE_LINE_HEIGHT * scale, 0f)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(90f * scale, nextY)
            layout.draw(canvas)
            canvas.restore()
            nextY += layout.height + 22f * scale
        }
        if (document.subtitle.isNotBlank()) {
            val subtitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = POSTER_SUBTITLE_ALPHA
                textSize = 32f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val layout = StaticLayout.Builder.obtain(
                document.subtitle,
                0,
                document.subtitle.length,
                subtitle,
                (width - 180f * scale).roundToInt(),
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(POSTER_SUBTITLE_LINE_HEIGHT * scale, 0f)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(90f * scale, nextY)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private fun drawScreenshots(
        canvas: Canvas,
        document: EditorDocument,
        images: List<Bitmap>,
        width: Int,
        height: Int,
        scale: Float,
    ) {
        val imagePlacements = PosterLayout.imagePlacements(
            canvasSize = androidx.compose.ui.unit.IntSize(width, height),
            layout = document.layout,
            imageAspectRatios = images.map { image -> image.width.toFloat() / image.height },
        )
        imagePlacements.forEach { imagePlacement ->
            drawScreenshot(
                canvas = canvas,
                placement = imagePlacement.placement,
                bitmap = images[imagePlacement.imageIndex],
                frameEnabled = document.frameEnabled,
                shadowLevel = document.shadow,
                scale = scale,
            )
        }
    }

    private fun drawScreenshot(
        canvas: Canvas,
        placement: PosterPlacement,
        bitmap: Bitmap,
        frameEnabled: Boolean,
        shadowLevel: ShadowLevel,
        scale: Float,
    ) {
        val bounds = RectF(
            placement.left,
            placement.top,
            placement.left + placement.width,
            placement.top + placement.height,
        )
        val radius = 42f * scale
        canvas.save()
        canvas.rotate(placement.rotationDegrees, bounds.centerX(), bounds.centerY())
        val shadowSpec = shadowLevel.posterShadowSpec(scale)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (layerIndex in POSTER_SHADOW_LAYER_COUNT downTo 1) {
            val expansion = shadowSpec.expansion(layerIndex)
            val shadowBounds = RectF(bounds).apply {
                offset(0f, shadowSpec.offsetY)
                inset(-expansion, -expansion)
            }
            shadowPaint.color = Color.argb(shadowSpec.layerAlpha(layerIndex), 0, 0, 0)
            canvas.drawRoundRect(
                shadowBounds,
                radius + expansion,
                radius + expansion,
                shadowPaint,
            )
        }

        val inset = if (frameEnabled) 16f * scale else 0f
        if (frameEnabled) {
            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 23, 30) }
            canvas.drawRoundRect(bounds, radius, radius, frame)
        }
        val screen = RectF(bounds).apply { inset(inset, inset) }
        val screenRadius = (radius - inset).coerceAtLeast(10f * scale)
        val clip = Path().apply { addRoundRect(screen, screenRadius, screenRadius, Path.Direction.CW) }
        canvas.clipPath(clip)
        canvas.drawBitmap(bitmap, centerCrop(bitmap, screen), screen, imagePaint)
        canvas.restore()
    }

    private fun centerCrop(bitmap: Bitmap, target: RectF): Rect {
        val sourceRatio = bitmap.width.toFloat() / bitmap.height
        val targetRatio = target.width() / target.height()
        return if (sourceRatio > targetRatio) {
            val cropWidth = (bitmap.height * targetRatio).roundToInt()
            val left = (bitmap.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, bitmap.height)
        } else {
            val cropHeight = (bitmap.width / targetRatio).roundToInt()
            val top = (bitmap.height - cropHeight) / 2
            Rect(0, top, bitmap.width, top + cropHeight)
        }
    }

    private companion object {
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
}
