package kr.donminzzi.screenloom.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface ImageLoader {
    suspend fun decode(
        uri: Uri,
        maxDimension: Int,
    ): Result<Bitmap>
}

class ImageDecoder(
    private val contentResolver: ContentResolver,
) : ImageLoader {
    override suspend fun decode(
        uri: Uri,
        maxDimension: Int,
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        runCatching {
            require(maxDimension > 0) { "Maximum dimension must be positive" }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsInput = contentResolver.openInputStream(uri) ?: error("Unable to open image")
            boundsInput.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("Unable to decode image")
            decoded.rotated(readRotationDegrees(uri))
        }
    }

    // BitmapFactory ignores the EXIF orientation tag, so a camera photo picked through the
    // photo picker would otherwise render sideways in both the preview and the export.
    // Needs its own stream: content URI streams are not reliably re-seekable. Missing or
    // unparsable EXIF degrades to 0 rather than failing the decode.
    private fun readRotationDegrees(uri: Uri): Int = runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).rotationDegrees
        }
    }.getOrNull() ?: 0

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        if (rotated !== this) recycle()
        return rotated
    }

    companion object {
        fun calculateInSampleSize(
            width: Int,
            height: Int,
            maxDimension: Int,
        ): Int {
            var sampleSize = 1
            var longestEdge = maxOf(width, height)
            while (longestEdge > maxDimension) {
                sampleSize *= 2
                longestEdge /= 2
            }
            return sampleSize
        }
    }
}
