package kr.donminzzi.screenloom.editor

import android.content.SharedPreferences

class EditorStylePreferences(
    private val preferences: SharedPreferences,
) {
    fun load(): EditorStyle = try {
        EditorStyle(
            layout = readEnum(LayoutKey, LayoutMode.Focus),
            palette = readEnum(PaletteKey, PaletteId.Ink),
            frameEnabled = preferences.getBoolean(FrameEnabledKey, true),
            shadow = readEnum(ShadowKey, ShadowLevel.Medium),
        )
    } catch (_: ClassCastException) {
        EditorStyle()
    }

    fun save(style: EditorStyle) {
        preferences.edit()
            .putString(LayoutKey, style.layout.name)
            .putString(PaletteKey, style.palette.name)
            .putBoolean(FrameEnabledKey, style.frameEnabled)
            .putString(ShadowKey, style.shadow.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T {
        val storedName = preferences.getString(key, null) ?: return default
        return enumValues<T>().firstOrNull { it.name == storedName } ?: default
    }

    companion object {
        const val Name = "screenloom_editor_style"

        private const val LayoutKey = "layout"
        private const val PaletteKey = "palette"
        private const val FrameEnabledKey = "frame_enabled"
        private const val ShadowKey = "shadow"
    }
}
