package kr.donminzzi.screenloom.media

import android.graphics.Bitmap
import android.net.Uri
import kr.donminzzi.screenloom.editor.EditorDocument
import kr.donminzzi.screenloom.render.PosterRenderer
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
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
        } catch (cancellation: CancellationException) {
            // Never report cancellation as a failure: the caller is being torn down.
            throw cancellation
        } catch (_: Throwable) {
            // Throwable rather than Exception so an OutOfMemoryError while allocating the
            // 1080x1920 output surfaces as a recoverable snackbar instead of a crash.
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
