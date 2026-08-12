package io.github.andrewdongminyoo.screenloom.editor

object EditorReducer {
    fun reduce(
        document: EditorDocument,
        action: EditorAction,
    ): EditorDocument = when (action) {
        is EditorAction.SetImageCount -> {
            val imageCount = action.count.coerceIn(0, 2)
            document.copy(
                imageCount = imageCount,
                layout = document.layout.takeUnless { it == LayoutMode.Split && imageCount < 2 }
                    ?: LayoutMode.Focus,
            )
        }

        is EditorAction.SetLayout -> document.copy(
            layout = action.layout.takeUnless { it == LayoutMode.Split && !document.canUseSplit }
                ?: document.layout,
        )

        is EditorAction.SetTitle -> document.copy(title = action.value.take(60))
        is EditorAction.SetSubtitle -> document.copy(subtitle = action.value.take(100))
        is EditorAction.SetPalette -> document.copy(palette = action.palette)
        is EditorAction.SetFrameEnabled -> document.copy(frameEnabled = action.enabled)
        is EditorAction.SetShadow -> document.copy(shadow = action.shadow)
        EditorAction.Reset -> EditorDocument(imageCount = document.imageCount)
    }
}
