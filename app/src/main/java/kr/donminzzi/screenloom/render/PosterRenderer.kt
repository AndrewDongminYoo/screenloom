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
import kr.donminzzi.screenloom.editor.EditorDocument
import kr.donminzzi.screenloom.editor.LayoutMode
import kr.donminzzi.screenloom.editor.PaletteId
import kr.donminzzi.screenloom.editor.ShadowLevel
import kotlin.math.roundToInt

data class PosterPalette(
    val startColor: Int,
    val endColor: Int,
    val accentColor: Int,
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
            alpha = 30
        }
        canvas.drawCircle(width * 0.82f, height * 0.18f, 240f * scale, glow)

        val texture = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 18
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
                .setMaxLines(2)
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
                alpha = 190
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
                .setMaxLines(2)
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
        val placements = PosterLayout.placements(
            canvasSize = androidx.compose.ui.unit.IntSize(width, height),
            layout = document.layout,
            imageCount = images.size,
        )
        val orderedImages = if (document.layout == LayoutMode.Stack && images.size >= 2) {
            listOf(images[1], images[0])
        } else {
            images
        }
        placements.zip(orderedImages).forEach { (placement, bitmap) ->
            drawScreenshot(canvas, placement, bitmap, document.frameEnabled, document.shadow, scale)
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
        val shadowRadius = when (shadowLevel) {
            ShadowLevel.Soft -> 20f
            ShadowLevel.Medium -> 34f
            ShadowLevel.Strong -> 52f
        } * scale
        canvas.save()
        canvas.rotate(placement.rotationDegrees, bounds.centerX(), bounds.centerY())
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(130, 0, 0, 0)
            setShadowLayer(shadowRadius, 0f, 18f * scale, Color.argb(150, 0, 0, 0))
        }
        canvas.drawRoundRect(bounds, radius, radius, shadow)

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
