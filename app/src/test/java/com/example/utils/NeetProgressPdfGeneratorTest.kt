package com.example.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NeetProgressPdfGeneratorTest {

    private fun createDummyReportData(): NeetProgressPdfGenerator.ReportData {
        return NeetProgressPdfGenerator.ReportData(
            candidateName = "John Doe",
            candidateClass = "12th Standard",
            targetNeetDate = "05 May 2025",
            totalStudyMinutes = 300,
            mockTests = emptyList(),
            dailyPractices = emptyList(),
            studyLogs = emptyList(),
            completedChecklistCount = 10,
            totalFlashcardsMastered = 5,
            daysRemaining = 60,
            streakDays = 7
        )
    }

    @Test
    fun testSavePdfToStorage_whenPdfEngineFailsOnJvm_catchesExceptionAndReturnsFailureResult() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val data = createDummyReportData()
        val result = NeetProgressPdfGenerator.savePdfToStorage(context, data)

        // On host JVM, PdfDocument native engine is not available, so savePdfToStorage should catch the exception gracefully
        assertNotNull(result)
        assertFalse(result.success)
        assertNotNull(result.message)
    }

    @Test
    fun testOpenPdf_withNullParams_handlesGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Should return early without throwing
        NeetProgressPdfGenerator.openPdf(context, uri = null, file = null)
    }

    @Test
    fun testOpenPdf_withFile_handlesExceptionGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "test_report.pdf").apply { createNewFile() }
        // Should catch ActivityNotFoundException/Exception gracefully in Robolectric environment without throwing
        NeetProgressPdfGenerator.openPdf(context, file = testFile)
    }

    @Test
    fun testSharePdf_withFile_handlesExceptionGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "test_share.pdf").apply { createNewFile() }
        // Should catch ActivityNotFoundException/Exception gracefully in Robolectric environment without throwing
        NeetProgressPdfGenerator.sharePdf(context, file = testFile)
    }
}
