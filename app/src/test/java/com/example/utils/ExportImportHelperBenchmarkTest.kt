package com.example.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportImportHelperBenchmarkTest {

    private lateinit var context: Context
    private lateinit var backupFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppDatabase.closeAndResetDatabase()
    }

    @After
    fun tearDown() {
        AppDatabase.closeAndResetDatabase()
        if (::backupFile.isInitialized && backupFile.exists()) {
            backupFile.delete()
        }
    }

    @Test
    fun benchmarkImportStudyLogs() = runBlocking {
        val root = JSONObject()
        val logsArray = JSONArray()
        val count = 1000

        for (i in 0 until count) {
            val obj = JSONObject()
            obj.put("subject", "Subject $i")
            obj.put("chapter", "Chapter $i")
            obj.put("durationSeconds", 300 + i)
            obj.put("timestamp", System.currentTimeMillis() - i * 1000L)
            logsArray.put(obj)
        }
        root.put("studyLogs", logsArray)

        backupFile = File(context.cacheDir, "benchmark_study_logs.json")
        backupFile.writeText(root.toString())

        val uri = android.net.Uri.fromFile(backupFile)

        val timeMs = measureTimeMillis {
            val result = ExportImportHelper.importData(context, uri)
            assertEquals(true, result.first)
        }

        val db = AppDatabase.getDatabase(context)
        val logs = db.appDao().getAllStudyLogs().first()
        assertEquals(count, logs.size)

        println("BENCHMARK_STUDY_LOGS_IMPORT_TIME_MS: $timeMs")
    }
}
