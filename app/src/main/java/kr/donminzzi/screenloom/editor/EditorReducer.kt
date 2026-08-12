package kr.donminzzi.screenloom.editor

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

        is EditorAction.SetTitle -> document.copy(title = action.value.takeCodePoints(60))
        is EditorAction.SetSubtitle -> document.copy(subtitle = action.value.takeCodePoints(100))
        is EditorAction.SetPalette -> document.copy(palette = action.palette)
        is EditorAction.SetFrameEnabled -> document.copy(frameEnabled = action.enabled)
        is EditorAction.SetShadow -> document.copy(shadow = action.shadow)
        EditorAction.Reset -> EditorDocument(imageCount = document.imageCount)
    }
}

internal fun String.codePointLength(): Int = codePointCount(0, length)

private fun String.takeCodePoints(maxCodePoints: Int): String {
    if (codePointLength() <= maxCodePoints) return this
    return substring(0, offsetByCodePoints(0, maxCodePoints))
}
