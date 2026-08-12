package io.github.andrewdongminyoo.screenloom.media

import android.graphics.Bitmap
import android.net.Uri
import io.github.andrewdongminyoo.screenloom.editor.EditorDocument
import io.github.andrewdongminyoo.screenloom.render.PosterRenderer
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface OutputStreamProvider {
    fun open(uri: Uri): OutputStream?
}

sealed interface ExportResult {
    data object Success : ExportResult

    data class Failure(val reason: String) : ExportResult
}

fun interface PosterWriter {
    suspend fun export(
        uri: Uri,
        document: EditorDocument,
        images: List<Bitmap>,
    ): ExportResult
}

class PosterExporter(
    private val renderer: PosterRenderer,
    private val outputStreamProvider: OutputStreamProvider,
) : PosterWriter {
    override suspend fun export(
        uri: Uri,
        document: EditorDocument,
        images: List<Bitmap>,
    ): ExportResult {
        var output: Bitmap? = null
        return try {
            output = withContext(Dispatchers.Default) {
                renderer.render(document, images, ExportWidth, ExportHeight)
            }
            withContext(Dispatchers.IO) {
                val stream = outputStreamProvider.open(uri) ?: return@withContext ExportResult.Failure(FailureMessage)
                stream.use { destination ->
                    if (!output.compress(Bitmap.CompressFormat.PNG, 100, destination)) {
                        return@withContext ExportResult.Failure(FailureMessage)
                    }
                    destination.flush()
                }
                ExportResult.Success
            }
        } catch (_: Exception) {
            ExportResult.Failure(FailureMessage)
        } finally {
            output?.takeUnless(Bitmap::isRecycled)?.recycle()
        }
    }

    private companion object {
        const val ExportWidth = 1080
        const val ExportHeight = 1920
        const val FailureMessage = "Unable to save PNG"
    }
}
