package kr.donminzzi.screenloom.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("Unable to decode image")
        }
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
