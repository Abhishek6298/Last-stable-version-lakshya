package com.example.utils

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportImportHelperTest {

    @Test
    fun testCreateBackupZipFile_createsFileSuccessfully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val backupFile = ExportImportHelper.createBackupZipFile(context)
        assertNotNull(backupFile)
        assertTrue(backupFile!!.exists())
        assertTrue(backupFile.name.startsWith("NEET_Prep_Backup_"))
        assertTrue(backupFile.name.endsWith(".json"))
    }

    @Test
    fun testCreateBackupJson_containsExpectedKeys() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jsonString = ExportImportHelper.createBackupJson(context)
        assertNotNull(jsonString)
        val json = JSONObject(jsonString)
        assertTrue(json.has("version"))
        assertTrue(json.has("exportDate"))
        assertTrue(json.has("preferences"))
        assertTrue(json.has("studyLogs"))
    }

    @Test
    fun testExportAndImportData_success() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "test_export_backup.json")
        val uri = Uri.fromFile(testFile)

        val exportResult = ExportImportHelper.exportData(context, uri)
        assertTrue(exportResult.first)
        assertTrue(testFile.exists())
        assertTrue(testFile.length() > 0)

        val importResult = ExportImportHelper.importData(context, uri)
        assertTrue(importResult.first)
        assertEquals("Data imported successfully! 🎉", importResult.second)
    }

    @Test
    fun testImportData_invalidFile_returnsFailure() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invalidFile = File(context.cacheDir, "invalid_backup.json")
        invalidFile.writeText("This is not valid JSON content!!!")
        val uri = Uri.fromFile(invalidFile)

        val importResult = ExportImportHelper.importData(context, uri)
        assertFalse(importResult.first)
        assertTrue(importResult.second.startsWith("Import failed: Invalid backup file format"))
    }
}
