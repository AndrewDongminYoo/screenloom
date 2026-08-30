package kr.donminzzi.screenloom.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes

enum class LayoutMode {
    Focus,
    Stack,
    Split,
}

enum class PaletteId {
    Ink,
    Cobalt,
    Coral,
    Moss,
    Violet,
    Sunrise,
}

enum class ShadowLevel {
    Soft,
    Medium,
    Strong,
}

data class EditorStyle(
    val layout: LayoutMode = LayoutMode.Focus,
    val palette: PaletteId = PaletteId.Ink,
    val frameEnabled: Boolean = true,
    val shadow: ShadowLevel = ShadowLevel.Medium,
)

data class EditorDocument(
    val imageCount: Int = 0,
    val layout: LayoutMode = LayoutMode.Focus,
    val title: String = "",
    val subtitle: String = "",
    val palette: PaletteId = PaletteId.Ink,
    val frameEnabled: Boolean = true,
    val shadow: ShadowLevel = ShadowLevel.Medium,
) {
    val canUseSplit: Boolean
        get() = imageCount >= 2
}

fun EditorDocument.style(): EditorStyle = EditorStyle(
    layout = layout,
    palette = palette,
    frameEnabled = frameEnabled,
    shadow = shadow,
)

fun EditorStyle.toDocument(): EditorDocument = EditorDocument(
    layout = layout,
    palette = palette,
    frameEnabled = frameEnabled,
    shadow = shadow,
)

data class ImportedImage(
    val uri: Uri,
    val bitmap: Bitmap,
)

data class EditorUiState(
    val document: EditorDocument = EditorDocument(),
    val images: List<ImportedImage> = emptyList(),
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val lastExportUri: Uri? = null,
    val canUndoReset: Boolean = false,
    @get:StringRes val message: Int? = null,
)

sealed interface EditorAction {
    data class SetImageCount(val count: Int) : EditorAction

    data class SetLayout(val layout: LayoutMode) : EditorAction

    data class SetTitle(val value: String) : EditorAction

    data class SetSubtitle(val value: String) : EditorAction

    data class SetPalette(val palette: PaletteId) : EditorAction

    data class SetFrameEnabled(val enabled: Boolean) : EditorAction

    data class SetShadow(val shadow: ShadowLevel) : EditorAction

    data object Reset : EditorAction
}
