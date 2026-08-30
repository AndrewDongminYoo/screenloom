package kr.donminzzi.screenloom

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun shareIntentCarriesThePngUriWithTemporaryReadAccess() {
        val output = Uri.parse("content://screenloom/output")

        val intent = createSharePngIntent(output)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        assertEquals(output, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals(output, intent.clipData?.getItemAt(0)?.uri)
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }
}
