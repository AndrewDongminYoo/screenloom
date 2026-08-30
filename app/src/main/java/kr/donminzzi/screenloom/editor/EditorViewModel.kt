package kr.donminzzi.screenloom.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.media.ExportResult
import kr.donminzzi.screenloom.media.ImageLoader
import kr.donminzzi.screenloom.media.PosterWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val imageLoader: ImageLoader,
    private val posterWriter: PosterWriter,
    initialStyle: EditorStyle = EditorStyle(),
    private val onStyleChanged: (EditorStyle) -> Unit = {},
) : ViewModel() {
    private val mutableState = MutableStateFlow(EditorUiState(document = initialStyle.toDocument()))
    val state: StateFlow<EditorUiState> = mutableState.asStateFlow()
    private var resetDocument: EditorDocument? = null

    fun import(uris: List<Uri>) {
        val selected = uris.take(MaxImages)
        if (selected.isEmpty()) return
        val previous = mutableState.value
        if (previous.isImporting || previous.isExporting) return

        resetDocument = null
        mutableState.value = previous.copy(
            isImporting = true,
            canUndoReset = false,
            message = null,
        )
        viewModelScope.launch {
            val results = selected.map { uri ->
                imageLoader.decode(uri, PreviewMaxDimension).map { bitmap ->
                    ImportedImage(uri, bitmap)
                }
            }
            val images = results.mapNotNull { result -> result.getOrNull() }
            if (images.isEmpty()) {
                mutableState.value = previous.copy(
                    canUndoReset = false,
                    message = R.string.import_failure,
                )
                return@launch
            }

            previous.images.forEach { image -> recycle(image.bitmap) }
            val document = EditorReducer.reduce(
                previous.document,
                EditorAction.SetImageCount(images.size),
            )
            mutableState.value = EditorUiState(
                document = document,
                images = images,
                message = R.string.import_failure.takeIf { images.size < results.size },
            )
            publishStyleChange(previous.document, document)
        }
    }

    fun dispatch(action: EditorAction) {
        val current = mutableState.value
        if (current.isImporting || current.isExporting) return

        val document = EditorReducer.reduce(current.document, action)
        if (document == current.document) return

        resetDocument = current.document.takeIf { action == EditorAction.Reset }
        mutableState.value = current.copy(
            document = document,
            lastExportUri = null,
            canUndoReset = action == EditorAction.Reset,
            message = R.string.reset_complete.takeIf { action == EditorAction.Reset } ?: current.message,
        )
        publishStyleChange(current.document, document)
    }

    fun export(uri: Uri) {
        val current = mutableState.value
        if (current.images.isEmpty()) {
            mutableState.update { state -> state.copy(message = R.string.empty_export) }
            return
        }
        if (current.isImporting || current.isExporting) return

        resetDocument = null
        mutableState.update { state ->
            state.copy(
                isExporting = true,
                lastExportUri = null,
                canUndoReset = false,
                message = null,
            )
        }
        viewModelScope.launch {
            val result = posterWriter.export(
                uri = uri,
                document = current.document,
                images = current.images.map(ImportedImage::bitmap),
            )
            mutableState.update { state ->
                state.copy(
                    isExporting = false,
                    lastExportUri = uri.takeIf { result is ExportResult.Success },
                    message = when (result) {
                        ExportResult.Success -> R.string.export_success
                        is ExportResult.Failure -> result.messageRes
                    },
                )
            }
        }
    }

    fun createAnother() {
        val current = mutableState.value
        if (current.isImporting || current.isExporting) return

        current.images.forEach { image -> recycle(image.bitmap) }
        resetDocument = null
        mutableState.value = EditorUiState(document = current.document.style().toDocument())
    }

    fun undoReset() {
        val restored = resetDocument ?: return
        val current = mutableState.value
        if (!current.canUndoReset || current.isImporting || current.isExporting) return

        resetDocument = null
        mutableState.value = current.copy(
            document = restored,
            lastExportUri = null,
            canUndoReset = false,
            message = null,
        )
        publishStyleChange(current.document, restored)
    }

    fun consumeMessage() {
        resetDocument = null
        mutableState.update { current -> current.copy(canUndoReset = false, message = null) }
    }

    override fun onCleared() {
        mutableState.value.images.forEach { image -> recycle(image.bitmap) }
        super.onCleared()
    }

    private fun recycle(bitmap: Bitmap) {
        if (!bitmap.isRecycled) bitmap.recycle()
    }

    private fun publishStyleChange(previous: EditorDocument, current: EditorDocument) {
        if (current.style() != previous.style()) onStyleChanged(current.style())
    }

    private companion object {
        const val MaxImages = 2
        const val PreviewMaxDimension = 2048
    }
}
