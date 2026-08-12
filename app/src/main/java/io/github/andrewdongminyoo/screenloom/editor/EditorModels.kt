package io.github.andrewdongminyoo.screenloom.editor

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
