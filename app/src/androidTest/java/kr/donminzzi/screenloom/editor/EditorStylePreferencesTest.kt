package kr.donminzzi.screenloom.editor

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorStylePreferencesTest {
    private val preferences = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getSharedPreferences("EditorStylePreferencesTest", Context.MODE_PRIVATE)

    @Test
    fun savesAndRestoresEveryVisualStyleValue() {
        val expected = EditorStyle(
            layout = LayoutMode.Split,
            palette = PaletteId.Violet,
            frameEnabled = false,
            shadow = ShadowLevel.Strong,
        )
        preferences.edit().clear().commit()
        try {
            EditorStylePreferences(preferences).save(expected)

            assertEquals(expected, EditorStylePreferences(preferences).load())
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun invalidStoredEnumValuesRecoverToTheDefaultStyle() {
        preferences.edit()
            .clear()
            .putString("layout", "Unknown")
            .putString("palette", "Unknown")
            .putString("shadow", "Unknown")
            .commit()
        try {
            assertEquals(EditorStyle(), EditorStylePreferences(preferences).load())
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun invalidStoredValueTypesRecoverToTheDefaultStyle() {
        preferences.edit()
            .clear()
            .putBoolean("layout", true)
            .putString("frame_enabled", "true")
            .commit()
        try {
            assertEquals(EditorStyle(), EditorStylePreferences(preferences).load())
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
