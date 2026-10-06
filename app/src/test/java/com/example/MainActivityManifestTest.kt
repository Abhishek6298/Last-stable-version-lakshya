package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityManifestTest {

    @Test
    fun testMainActivityIsExported() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_ACTIVITIES
        )
        val mainActivityInfo = packageInfo.activities?.firstOrNull {
            it.name == MainActivity::class.java.name
        }
        assertNotNull("MainActivity should be present in package manifest", mainActivityInfo)
        assertTrue("MainActivity should be exported to support opening PDFs from external apps", mainActivityInfo!!.exported)
    }
}
