package com.jesuskrastev.bali

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Covers [TestLabDetector], which keeps Firebase Test Lab robots out of analytics. */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class TestLabDetectorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a normal device is not Test Lab`() {
        assertThat(TestLabDetector.isTestLab(context)).isFalse()
    }

    @Test
    fun `a device Firebase Test Lab flagged is Test Lab`() {
        Settings.System.putString(context.contentResolver, "firebase.test.lab", "true")

        assertThat(TestLabDetector.isTestLab(context)).isTrue()
    }

    @Test
    fun `only the exact value true counts`() {
        Settings.System.putString(context.contentResolver, "firebase.test.lab", "false")

        assertThat(TestLabDetector.isTestLab(context)).isFalse()
    }
}
