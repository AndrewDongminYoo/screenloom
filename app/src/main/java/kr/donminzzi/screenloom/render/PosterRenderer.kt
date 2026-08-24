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
    val headlineColor: Int,
    val supportingCopyColor: Int,
    val frameColor: Int,
    val shadowColor: Int,
    val ribbonOneColor: Int,
    val ribbonTwoColor: Int,
    val sunColor: Int,
    val copyZoneColor: Int?,
)

internal const val POSTER_SHADOW_LAYER_COUNT = 12
internal const val POSTER_TITLE_LINE_HEIGHT = 86f
internal const val POSTER_SUBTITLE_LINE_HEIGHT = 42f

// The copy block is placed from explicit baselines, not from font-metric-driven line boxes.
// Hangul reaches the block through a fallback face whose metrics differ from the Latin serif's,
// and the two renderers disagreed by 18-29px at 1080x1920 while agreeing within 3px on Latin.
internal const val POSTER_REFERENCE_WIDTH = 1080f
internal const val POSTER_COPY_LEFT = 90f
internal const val POSTER_TITLE_FIRST_BASELINE = 222f
internal const val POSTER_SUBTITLE_FIRST_BASELINE = 180f
internal const val POSTER_COPY_BASELINE_GAP = 70f
internal const val POSTER_SUN_ALPHA = 176
internal const val POSTER_RIBBON_ALPHA = 72
internal const val POSTER_COPY_ZONE_ALPHA = 235

internal data class PosterCopyZoneSpec(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val cornerRadius: Float,
)

internal val PosterCopyZone = PosterCopyZoneSpec(
    left = 60f,
    top = 105f,
    right = 1020f,
    bottom = 480f,
    cornerRadius = 48f,
)

internal data class PosterRibbonSpec(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float,
)

internal val PosterRibbonSpecs = listOf(
    PosterRibbonSpec(left = -170f, top = 820f, width = 1420f, height = 210f, rotationDegrees = -12f),
    PosterRibbonSpec(left = -160f, top = 1040f, width = 1400f, height = 180f, rotationDegrees = 14f),
)

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
    PaletteId.Ink -> PosterPalette(
        startColor = 0xFFFFF8E9.toInt(),
        endColor = 0xFFFFD9A2.toInt(),
        headlineColor = 0xFF18213D.toInt(),
        supportingCopyColor = 0xFF18213D.toInt(),
        frameColor = 0xFF18213D.toInt(),
        shadowColor = 0xFF18213D.toInt(),
        ribbonOneColor = 0xFF566EFF.toInt(),
        ribbonTwoColor = 0xFFFF6B4A.toInt(),
        sunColor = 0xFFFFD466.toInt(),
        copyZoneColor = null,
    )
    PaletteId.Cobalt -> PosterPalette(
        startColor = 0xFF3557F0.toInt(),
        endColor = 0xFF78DBEF.toInt(),
        headlineColor = 0xFFFFF8E9.toInt(),
        supportingCopyColor = 0xFFFFF8E9.toInt(),
        frameColor = 0xFF18213D.toInt(),
        shadowColor = 0xFF18213D.toInt(),
        ribbonOneColor = 0xFFFFD466.toInt(),
        ribbonTwoColor = 0xFFFF6B4A.toInt(),
        sunColor = 0xFFFFF8E9.toInt(),
        copyZoneColor = 0xFF3557F0.toInt(),
    )
    PaletteId.Coral -> PosterPalette(
        startColor = 0xFFFF765C.toInt(),
        endColor = 0xFFFFC46D.toInt(),
        headlineColor = 0xFF18213D.toInt(),
        supportingCopyColor = 0xFF18213D.toInt(),
        frameColor = 0xFF18213D.toInt(),
        shadowColor = 0xFF18213D.toInt(),
        ribbonOneColor = 0xFFFFF8E9.toInt(),
        ribbonTwoColor = 0xFF566EFF.toInt(),
        sunColor = 0xFFFFF0BD.toInt(),
        copyZoneColor = null,
    )
    PaletteId.Moss -> PosterPalette(
        startColor = 0xFF6BD7B3.toInt(),
        endColor = 0xFFD8EF6A.toInt(),
        headlineColor = 0xFF18213D.toInt(),
        supportingCopyColor = 0xFF18213D.toInt(),
        frameColor = 0xFF18213D.toInt(),
        shadowColor = 0xFF18213D.toInt(),
        ribbonOneColor = 0xFF18213D.toInt(),
        ribbonTwoColor = 0xFFFF6B4A.toInt(),
        sunColor = 0xFFFFF8E9.toInt(),
        copyZoneColor = null,
    )
    PaletteId.Violet -> PosterPalette(
        startColor = 0xFF5D50D8.toInt(),
        endColor = 0xFFF3A1C7.toInt(),
        headlineColor = 0xFFFFF8E9.toInt(),
        supportingCopyColor = 0xFFFFF8E9.toInt(),
        frameColor = 0xFF18213D.toInt(),
        shadowColor = 0xFF18213D.toInt(),
        ribbonOneColor = 0xFFFFD466.toInt(),
        ribbonTwoColor = 0xFFFFF8E9.toInt(),
        sunColor = 0xFFFFD466.toInt(),
        copyZoneColor = 0xFF5D50D8.toInt(),
    )
    PaletteId.Sunrise -> PosterPalette(
        startColor = 0xFFFFE26C.toInt(),
        endColor = 0xFFFF7C56.toInt(),
        headlineColor = 0xFF18213D.toInt(),
        supportingCopyColor = 0xFF18213D.toInt(),
        frameColor = 0xFF18213D.toInt(),
        shadowColor = 0xFF18213D.toInt(),
        ribbonOneColor = 0xFF566EFF.toInt(),
        ribbonTwoColor = 0xFFFFF8E9.toInt(),
        sunColor = 0xFFFFF8E9.toInt(),
        copyZoneColor = null,
    )
}

class PosterRenderer {
    fun render(
        document: EditorDocument,
        images: List<Bitmap>,
        width: Int,
        height: Int,
    ): Bitmap {
        require(width > 0 && height > 0) { "Poster dimensions must be positive" }
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, false)
        val canvas = Canvas(output)
        val scale = width / 1080f
        val palette = document.palette.colors()
        drawBackground(canvas, palette, width, height, scale)
        drawCopyZone(canvas, document, palette, scale)
        drawScreenshots(canvas, document, images, palette, width, height, scale)
        drawPosterCopy(canvas, document, palette, scale)
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

        val sun = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.sunColor
            alpha = POSTER_SUN_ALPHA
        }
        canvas.drawCircle(width * 0.82f, height * 0.18f, 240f * scale, sun)
        PosterRibbonSpecs.zip(listOf(palette.ribbonOneColor, palette.ribbonTwoColor)).forEach { (spec, color) ->
            drawRibbon(canvas, spec, color, scale)
        }
    }

    private fun drawRibbon(
        canvas: Canvas,
        spec: PosterRibbonSpec,
        color: Int,
        scale: Float,
    ) {
        val bounds = RectF(
            spec.left * scale,
            spec.top * scale,
            (spec.left + spec.width) * scale,
            (spec.top + spec.height) * scale,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = POSTER_RIBBON_ALPHA
        }
        canvas.save()
        canvas.rotate(spec.rotationDegrees, bounds.centerX(), bounds.centerY())
        canvas.drawRoundRect(bounds, bounds.height() / 2f, bounds.height() / 2f, paint)
        canvas.restore()
    }

    private fun drawCopyZone(
        canvas: Canvas,
        document: EditorDocument,
        palette: PosterPalette,
        scale: Float,
    ) {
        if (document.title.isBlank() && document.subtitle.isBlank()) return
        val color = palette.copyZoneColor ?: return
        val bounds = RectF(
            PosterCopyZone.left * scale,
            PosterCopyZone.top * scale,
            PosterCopyZone.right * scale,
            PosterCopyZone.bottom * scale,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = POSTER_COPY_ZONE_ALPHA
        }
        canvas.drawRoundRect(
            bounds,
            PosterCopyZone.cornerRadius * scale,
            PosterCopyZone.cornerRadius * scale,
            paint,
        )
    }

    private fun drawScreenshots(
        canvas: Canvas,
        document: EditorDocument,
        images: List<Bitmap>,
        palette: PosterPalette,
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
                palette = palette,
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
        palette: PosterPalette,
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
            shadowPaint.color = Color.argb(
                shadowSpec.layerAlpha(layerIndex),
                Color.red(palette.shadowColor),
                Color.green(palette.shadowColor),
                Color.blue(palette.shadowColor),
            )
            canvas.drawRoundRect(
                shadowBounds,
                radius + expansion,
                radius + expansion,
                shadowPaint,
            )
        }

        val inset = if (frameEnabled) 16f * scale else 0f
        if (frameEnabled) {
            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.frameColor }
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

/**
 * Draws the poster copy for both renderers. Every line lands on a baseline computed from the
 * 1080-wide reference, so a fallback face's own ascent and descent cannot move the block.
 * StaticLayout still performs line breaking and ellipsis; only its vertical placement is overridden.
 */
internal fun drawPosterCopy(
    canvas: Canvas,
    document: EditorDocument,
    palette: PosterPalette,
    scale: Float,
) {
    if (document.title.isBlank() && document.subtitle.isBlank()) return
    canvas.save()
    // Everything below is computed in 1080-wide reference units and only then scaled, so the
    // preview and the export run identical layout arithmetic instead of quantising line advances
    // to whole pixels at two different sizes.
    canvas.scale(scale, scale)
    val lineWidth = (POSTER_REFERENCE_WIDTH - 2f * POSTER_COPY_LEFT).roundToInt()
    var baseline = if (document.title.isNotBlank()) {
        POSTER_TITLE_FIRST_BASELINE
    } else {
        POSTER_SUBTITLE_FIRST_BASELINE
    }
    if (document.title.isNotBlank()) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.headlineColor
            textSize = 78f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val layout = copyLayout(document.title, paint, lineWidth, POSTER_TITLE_LINE_HEIGHT)
        drawAtExplicitBaseline(canvas, layout, POSTER_COPY_LEFT, baseline)
        baseline += (layout.lineCount - 1) * POSTER_TITLE_LINE_HEIGHT + POSTER_COPY_BASELINE_GAP
    }
    if (document.subtitle.isNotBlank()) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.supportingCopyColor
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val layout = copyLayout(document.subtitle, paint, lineWidth, POSTER_SUBTITLE_LINE_HEIGHT)
        drawAtExplicitBaseline(canvas, layout, POSTER_COPY_LEFT, baseline)
    }
    canvas.restore()
}

private fun copyLayout(text: String, paint: TextPaint, width: Int, lineHeight: Float): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .setLineSpacing(lineHeight, 0f)
        .setMaxLines(2)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()

/**
 * Pins line 0 onto [firstBaseline]. With a line-spacing multiplier of zero every later line
 * advances by exactly the reference line height, so the whole block becomes font-independent.
 */
private fun drawAtExplicitBaseline(
    canvas: Canvas,
    layout: StaticLayout,
    left: Float,
    firstBaseline: Float,
) {
    canvas.save()
    canvas.translate(left, firstBaseline - layout.getLineBaseline(0))
    layout.draw(canvas)
    canvas.restore()
}
