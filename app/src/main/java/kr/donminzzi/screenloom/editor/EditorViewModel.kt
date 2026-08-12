package kr.donminzzi.screenloom.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val mutableState = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = mutableState.asStateFlow()

    fun import(uris: List<Uri>) {
        val selected = uris.take(MaxImages)
        if (selected.isEmpty()) return

        viewModelScope.launch {
            val previous = mutableState.value
            mutableState.update { current -> current.copy(isImporting = true, message = null) }
            val results = selected.map { uri ->
                imageLoader.decode(uri, PreviewMaxDimension).map { bitmap ->
                    ImportedImage(uri, bitmap)
                }
            }
            val images = results.mapNotNull { result -> result.getOrNull() }
            if (images.isEmpty()) {
                mutableState.value = previous.copy(message = ImportFailureMessage)
                return@launch
            }

            previous.images.forEach { image -> recycle(image.bitmap) }
            mutableState.value = EditorUiState(
                document = EditorReducer.reduce(
                    previous.document,
                    EditorAction.SetImageCount(images.size),
                ),
                images = images,
                message = ImportFailureMessage.takeIf { images.size < results.size },
            )
        }
    }

    fun dispatch(action: EditorAction) {
        mutableState.update { current ->
            current.copy(document = EditorReducer.reduce(current.document, action))
        }
    }

    fun export(uri: Uri) {
        val current = mutableState.value
        if (current.images.isEmpty()) {
            mutableState.update { state -> state.copy(message = EmptyExportMessage) }
            return
        }
        if (current.isExporting) return

        viewModelScope.launch {
            mutableState.update { state -> state.copy(isExporting = true, message = null) }
            val result = posterWriter.export(
                uri = uri,
                document = current.document,
                images = current.images.map(ImportedImage::bitmap),
            )
            mutableState.update { state ->
                state.copy(
                    isExporting = false,
                    message = when (result) {
                        ExportResult.Success -> ExportSuccessMessage
                        is ExportResult.Failure -> result.reason
                    },
                )
            }
        }
    }

    fun consumeMessage() {
        mutableState.update { current -> current.copy(message = null) }
    }

    override fun onCleared() {
        mutableState.value.images.forEach { image -> recycle(image.bitmap) }
        super.onCleared()
    }

    private fun recycle(bitmap: Bitmap) {
        if (!bitmap.isRecycled) bitmap.recycle()
    }

    private companion object {
        const val MaxImages = 2
        const val PreviewMaxDimension = 2048
        const val ImportFailureMessage = "Unable to read that image"
        const val EmptyExportMessage = "Choose a screenshot first"
        const val ExportSuccessMessage = "PNG saved"
    }
}
