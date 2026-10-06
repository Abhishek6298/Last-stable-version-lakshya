package com.example.utils

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.DailyPractice
import com.example.data.MockTest
import com.example.data.StudyLog
import com.example.data.getMaxScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TreeMap

object NeetProgressPdfGenerator {
    private const val TAG = "NeetProgressPdfGenerator"

    data class ReportData(
        val candidateName: String,
        val candidateClass: String,
        val targetNeetDate: String,
        val totalStudyMinutes: Long,
        val mockTests: List<MockTest>,
        val dailyPractices: List<DailyPractice>,
        val studyLogs: List<StudyLog>,
        val completedChecklistCount: Int,
        val totalFlashcardsMastered: Int = 0,
        val daysRemaining: Long = 0L,
        val streakDays: Int = 0,
        val avatarUri: String? = null
    )

    data class SaveResult(
        val success: Boolean,
        val uri: Uri? = null,
        val file: File? = null,
        val displayPath: String = "",
        val message: String = ""
    )

    data class MonthlyAggregate(
        val monthKey: String, // "yyyy-MM"
        val displayMonth: String, // "September 2026"
        var studySeconds: Long = 0L,
        var physicsSolved: Int = 0,
        var chemistrySolved: Int = 0,
        var biologySolved: Int = 0,
        var mockTestsCount: Int = 0,
        val mockScores: MutableList<Int> = mutableListOf()
    ) {
        val totalSolved: Int get() = physicsSolved + chemistrySolved + biologySolved
        val avgMockScore: Int get() = if (mockScores.isNotEmpty()) (mockScores.average()).toInt() else 0
        val bestMockScore: Int get() = mockScores.maxOrNull() ?: 0
    }

    private fun loadAvatarBitmap(context: Context, avatarUriString: String?): Bitmap? {
        try {
            if (!avatarUriString.isNullOrBlank()) {
                val uri = Uri.parse(avatarUriString)
                val decoded = when (uri.scheme) {
                    "content" -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    "file" -> {
                        BitmapFactory.decodeFile(uri.path)
                    }
                    else -> {
                        val file = File(avatarUriString)
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    }
                }
                if (decoded != null) return decoded
            }

            // Fallback to persistent avatar file
            val persistentFile = File(context.filesDir, "user_avatar_persistent.jpg")
            if (persistentFile.exists()) {
                return BitmapFactory.decodeFile(persistentFile.absolutePath)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load avatar bitmap for PDF", e)
        }
        return null
    }

    private fun getCircularBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        val srcRect = Rect(
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            (bitmap.width + size) / 2,
            (bitmap.height + size) / 2
        )
        val dstRect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())

        canvas.drawOval(dstRect, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        return output
    }

    private fun computeMonthlyAggregates(data: ReportData): List<MonthlyAggregate> {
        val monthMap = TreeMap<String, MonthlyAggregate>(reverseOrder()) // Most recent first
        val monthKeySdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthDisplaySdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // 1. Process Daily Practices
        data.dailyPractices.forEach { practice ->
            val mKey = if (practice.date.length >= 7) practice.date.take(7) else ""
            if (mKey.isNotBlank()) {
                val agg = monthMap.getOrPut(mKey) {
                    val disp = try {
                        val parsed = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(mKey)
                        if (parsed != null) monthDisplaySdf.format(parsed) else mKey
                    } catch (e: Exception) {
                        mKey
                    }
                    MonthlyAggregate(mKey, disp)
                }
                agg.physicsSolved += practice.physicsSolved
                agg.chemistrySolved += practice.chemistrySolved
                agg.biologySolved += practice.biologySolved
            }
        }

        // 2. Process Study Logs
        data.studyLogs.forEach { log ->
            val mKey = monthKeySdf.format(Date(log.timestamp))
            val agg = monthMap.getOrPut(mKey) {
                val disp = monthDisplaySdf.format(Date(log.timestamp))
                MonthlyAggregate(mKey, disp)
            }
            agg.studySeconds += log.durationSeconds
        }

        // 3. Process Mock Tests
        data.mockTests.forEach { test ->
            val mKey = monthKeySdf.format(Date(test.timestamp))
            val agg = monthMap.getOrPut(mKey) {
                val disp = monthDisplaySdf.format(Date(test.timestamp))
                MonthlyAggregate(mKey, disp)
            }
            agg.mockTestsCount++
            agg.mockScores.add(test.score)
        }

        // Ensure current month is present even if empty
        val currentMonthKey = monthKeySdf.format(Date())
        if (!monthMap.containsKey(currentMonthKey)) {
            monthMap[currentMonthKey] = MonthlyAggregate(
                currentMonthKey,
                monthDisplaySdf.format(Date())
            )
        }

        return monthMap.values.toList()
    }

    fun createPdfDocument(context: Context, data: ReportData): PdfDocument {
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        val avatarBitmap = loadAvatarBitmap(context, data.avatarUri)

        val totalHours = data.totalStudyMinutes / 60
        val totalMins = data.totalStudyMinutes % 60
        val phySolved = data.dailyPractices.sumOf { it.physicsSolved }
        val chemSolved = data.dailyPractices.sumOf { it.chemistrySolved }
        val bioSolved = data.dailyPractices.sumOf { it.biologySolved }
        val totalSolved = phySolved + chemSolved + bioSolved
        val bestTest = data.mockTests.maxByOrNull { it.score }
        val bestScore = bestTest?.score ?: 0
        val bestMax = bestTest?.getMaxScore() ?: 720
        val monthlyList = computeMonthlyAggregates(data)

        // ==========================================
        // PAGE 1: DOSSIER, PROFILE, ALL-TIME OVERVIEW & MONTHLY BREAKDOWN
        // ==========================================
        val page1Info = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(page1Info)
        val canvas1 = page1.canvas

        // 1. HEADER BANNER
        paint.color = Color.parseColor("#1E1B4B") // Deep Navy Indigo
        canvas1.drawRect(0f, 0f, 595f, 105f, paint)

        paint.color = Color.parseColor("#6366F1") // Accent line
        canvas1.drawRect(0f, 101f, 595f, 105f, paint)

        // App Branding Title
        paint.color = Color.WHITE
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("LAKSHYA PREP • ACADEMIC PROGRESS DOSSIER", 25f, 40f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#C7D2FE")
        canvas1.drawText("Official Comprehensive Performance, Practice & Monthly Analytics Report", 25f, 60f, paint)

        val reportId = "LAKSHYA-" + System.currentTimeMillis().toString().takeLast(6)
        val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas1.drawText("Report ID: $reportId  |  Generated: $currentDate", 25f, 78f, paint)

        // Header Badge
        paint.color = Color.parseColor("#059669")
        canvas1.drawRoundRect(445f, 25f, 570f, 52f, 8f, 8f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("OFFICIAL DOSSIER", 458f, 42f, paint)

        var y = 120f

        // 2. CANDIDATE PROFILE CARD (With Profile Photo!)
        paint.color = Color.parseColor("#F8FAFC")
        canvas1.drawRoundRect(25f, y, 570f, y + 84f, 12f, 12f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas1.drawRoundRect(25f, y, 570f, y + 84f, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        // Draw Profile Photo or Avatar Badge
        val photoSize = 60
        val photoX = 38f
        val photoY = y + 12f
        if (avatarBitmap != null) {
            try {
                val circular = getCircularBitmap(avatarBitmap, photoSize)
                canvas1.drawBitmap(circular, photoX, photoY, null)

                // Draw subtle border ring around photo
                paint.color = Color.parseColor("#6366F1")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas1.drawCircle(photoX + (photoSize / 2f), photoY + (photoSize / 2f), (photoSize / 2f), paint)
                paint.style = Paint.Style.FILL
            } catch (e: Exception) {
                drawDefaultAvatarBadge(canvas1, paint, photoX, photoY, photoSize, data.candidateName)
            }
        } else {
            drawDefaultAvatarBadge(canvas1, paint, photoX, photoY, photoSize, data.candidateName)
        }

        // Candidate Details
        val textStartX = 110f
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText(data.candidateName.ifBlank { "NEET Aspirant" }, textStartX, y + 30f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("Target Batch / Class: ${data.candidateClass.ifBlank { "12th / Dropper" }}", textStartX, y + 50f, paint)
        canvas1.drawText("Target Exam Date: ${data.targetNeetDate}", textStartX, y + 68f, paint)

        // Countdown Pill on Right
        paint.color = Color.parseColor("#EEF2FF")
        canvas1.drawRoundRect(400f, y + 18f, 555f, y + 66f, 10f, 10f, paint)
        paint.color = Color.parseColor("#4F46E5")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("${data.daysRemaining} DAYS LEFT", 420f, y + 46f, paint)

        y += 102f

        // 3. PERFORMANCE OVERVIEW (ALL TIME PHYSICS, CHEMISTRY, BIOLOGY & COMBINED QUESTIONS)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("ALL-TIME PERFORMANCE OVERVIEW", 25f, y, paint)

        y += 12f
        // Row 1: Study Time, Total Combined Solved, Best Mock
        // Card 1: Study Time
        paint.color = Color.parseColor("#F1F5F9")
        canvas1.drawRoundRect(25f, y, 195f, y + 50f, 8f, 8f, paint)
        paint.color = Color.parseColor("#4F46E5")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("${totalHours}h ${totalMins}m", 35f, y + 24f, paint)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("TOTAL STUDY TIME", 35f, y + 40f, paint)

        // Card 2: Total Combined Solved (All Time P + C + B)
        paint.color = Color.parseColor("#ECFDF5")
        canvas1.drawRoundRect(210f, y, 385f, y + 50f, 8f, 8f, paint)
        paint.color = Color.parseColor("#059669")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("$totalSolved Qs", 220f, y + 24f, paint)
        paint.color = Color.parseColor("#047857")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("ALL-TIME COMBINED SOLVED (P+C+B)", 220f, y + 40f, paint)

        // Card 3: Best Mock Score
        paint.color = Color.parseColor("#FFFBEB")
        canvas1.drawRoundRect(400f, y, 570f, y + 50f, 8f, 8f, paint)
        paint.color = Color.parseColor("#D97706")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("$bestScore / $bestMax", 410f, y + 24f, paint)
        paint.color = Color.parseColor("#B45309")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("BEST MOCK TEST SCORE", 410f, y + 40f, paint)

        y += 56f

        // Row 2: Physics Solved, Chemistry Solved, Biology Solved
        // Physics Card
        paint.color = Color.parseColor("#EEF2FF")
        canvas1.drawRoundRect(25f, y, 195f, y + 46f, 8f, 8f, paint)
        paint.color = Color.parseColor("#4338CA")
        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("$phySolved Qs", 35f, y + 22f, paint)
        paint.color = Color.parseColor("#4F46E5")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("⚡ PHYSICS QUESTIONS SOLVED", 35f, y + 36f, paint)

        // Chemistry Card
        paint.color = Color.parseColor("#FEF3C7")
        canvas1.drawRoundRect(210f, y, 385f, y + 46f, 8f, 8f, paint)
        paint.color = Color.parseColor("#B45309")
        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("$chemSolved Qs", 220f, y + 22f, paint)
        paint.color = Color.parseColor("#D97706")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("🧪 CHEMISTRY QUESTIONS SOLVED", 220f, y + 36f, paint)

        // Biology Card
        paint.color = Color.parseColor("#DCFCE7")
        canvas1.drawRoundRect(400f, y, 570f, y + 46f, 8f, 8f, paint)
        paint.color = Color.parseColor("#15803D")
        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("$bioSolved Qs", 410f, y + 22f, paint)
        paint.color = Color.parseColor("#16A34A")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("🌿 BIOLOGY QUESTIONS SOLVED", 410f, y + 36f, paint)

        y += 62f

        // 4. MONTH-BY-MONTH PRACTICE & STUDY BREAKDOWN TABLE
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("MONTH-BY-MONTH PRACTICE & STUDY BREAKDOWN", 25f, y, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("Individual monthly study hours, subject questions & test statistics", 25f, y + 14f, paint)

        y += 24f

        // Table Header
        paint.color = Color.parseColor("#4F46E5")
        canvas1.drawRoundRect(25f, y, 570f, y + 24f, 6f, 6f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Month & Year", 35f, y + 16f, paint)
        canvas1.drawText("Study Time", 145f, y + 16f, paint)
        canvas1.drawText("Physics", 230f, y + 16f, paint)
        canvas1.drawText("Chemistry", 295f, y + 16f, paint)
        canvas1.drawText("Biology", 365f, y + 16f, paint)
        canvas1.drawText("Total MCQs", 430f, y + 16f, paint)
        canvas1.drawText("Tests (Best)", 500f, y + 16f, paint)

        y += 24f

        val displayMonths = monthlyList.take(9)
        displayMonths.forEachIndexed { index, m ->
            y += 2f
            val rowBg = if (index % 2 == 0) "#F8FAFC" else "#FFFFFF"
            paint.color = Color.parseColor(rowBg)
            canvas1.drawRect(25f, y, 570f, y + 22f, paint)

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas1.drawText(m.displayMonth.take(18), 35f, y + 15f, paint)

            val mHours = m.studySeconds / 3600
            val mMins = (m.studySeconds % 3600) / 60
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas1.drawText("${mHours}h ${mMins}m", 145f, y + 15f, paint)

            paint.color = Color.parseColor("#4338CA")
            canvas1.drawText("${m.physicsSolved}", 230f, y + 15f, paint)

            paint.color = Color.parseColor("#B45309")
            canvas1.drawText("${m.chemistrySolved}", 295f, y + 15f, paint)

            paint.color = Color.parseColor("#15803D")
            canvas1.drawText("${m.biologySolved}", 365f, y + 15f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#059669")
            canvas1.drawText("${m.totalSolved}", 430f, y + 15f, paint)

            paint.color = Color.parseColor("#4F46E5")
            val testInfo = if (m.mockTestsCount > 0) "${m.mockTestsCount} (${m.bestMockScore})" else "—"
            canvas1.drawText(testInfo, 500f, y + 15f, paint)

            y += 22f
        }

        // Page 1 Footer
        paint.color = Color.parseColor("#E2E8F0")
        canvas1.drawLine(25f, 790f, 570f, 790f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas1.drawText("Generated by Lakshya Prep • Official Academic Dossier • Confidential", 25f, 810f, paint)
        canvas1.drawText("Page 1 of 2", 520f, 810f, paint)

        pdfDocument.finishPage(page1)

        // ==========================================
        // PAGE 2: MOCK TEST HISTORY & STUDY SESSIONS
        // ==========================================
        val page2Info = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(page2Info)
        val canvas2 = page2.canvas

        // Sub Header Banner
        paint.color = Color.parseColor("#1E1B4B")
        canvas2.drawRect(0f, 0f, 595f, 65f, paint)

        paint.color = Color.parseColor("#6366F1")
        canvas2.drawRect(0f, 62f, 595f, 65f, paint)

        paint.color = Color.WHITE
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("LAKSHYA PREP • DETAILED TEST & REVISION DOSSIER", 25f, 32f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#C7D2FE")
        canvas2.drawText("Candidate: ${data.candidateName.ifBlank { "NEET Aspirant" }}  |  Report ID: $reportId", 25f, 50f, paint)

        var y2 = 85f

        // 1. MOCK TEST PERFORMANCE HISTORY TABLE
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("MOCK TEST PERFORMANCE HISTORY", 25f, y2, paint)

        y2 += 14f
        // Table Header
        paint.color = Color.parseColor("#4F46E5")
        canvas2.drawRoundRect(25f, y2, 570f, y2 + 24f, 6f, 6f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("#", 35f, y2 + 16f, paint)
        canvas2.drawText("Test Name", 60f, y2 + 16f, paint)
        canvas2.drawText("Score", 290f, y2 + 16f, paint)
        canvas2.drawText("P / C / B Breakdown", 370f, y2 + 16f, paint)
        canvas2.drawText("Date", 495f, y2 + 16f, paint)

        y2 += 24f
        if (data.mockTests.isEmpty()) {
            y2 += 16f
            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas2.drawText("No mock tests recorded yet in your dashboard history.", 35f, y2, paint)
            y2 += 24f
        } else {
            val displayTests = data.mockTests.take(14)
            displayTests.forEachIndexed { index, test ->
                y2 += 2f
                val rowBg = if (index % 2 == 0) "#F8FAFC" else "#FFFFFF"
                paint.color = Color.parseColor(rowBg)
                canvas2.drawRect(25f, y2, 570f, y2 + 22f, paint)

                paint.color = Color.parseColor("#334155")
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(test.timestamp))
                canvas2.drawText("${index + 1}", 35f, y2 + 15f, paint)
                canvas2.drawText(test.testName.take(30), 60f, y2 + 15f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#4F46E5")
                canvas2.drawText("${test.score}/${test.getMaxScore()}", 290f, y2 + 15f, paint)

                paint.color = Color.parseColor("#475569")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas2.drawText("${test.physics} / ${test.chemistry} / ${test.biology}", 370f, y2 + 15f, paint)
                canvas2.drawText(dateStr, 495f, y2 + 15f, paint)

                y2 += 22f
            }
        }

        y2 += 30f

        // 2. SYLLABUS & REVISION MILESTONES SUMMARY
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("SYLLABUS & MASTERY MILESTONES", 25f, y2, paint)

        y2 += 14f

        // Card 1: Completed Topics
        paint.color = Color.parseColor("#FDF2F8")
        canvas2.drawRoundRect(25f, y2, 195f, y2 + 56f, 8f, 8f, paint)
        paint.color = Color.parseColor("#DB2777")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("${data.completedChecklistCount} Topics", 35f, y2 + 25f, paint)
        paint.color = Color.parseColor("#9D174D")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("NEET SYLLABUS COMPLETED", 35f, y2 + 43f, paint)

        // Card 2: Flashcards
        paint.color = Color.parseColor("#F5F3FF")
        canvas2.drawRoundRect(210f, y2, 385f, y2 + 56f, 8f, 8f, paint)
        paint.color = Color.parseColor("#7C3AED")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("${data.totalFlashcardsMastered} Cards", 220f, y2 + 25f, paint)
        paint.color = Color.parseColor("#5B21B6")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("FLASHCARDS MASTERED", 220f, y2 + 43f, paint)

        // Card 3: Active Streak
        paint.color = Color.parseColor("#FFF7ED")
        canvas2.drawRoundRect(400f, y2, 570f, y2 + 56f, 8f, 8f, paint)
        paint.color = Color.parseColor("#EA580C")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("${data.streakDays} Days", 410f, y2 + 25f, paint)
        paint.color = Color.parseColor("#9A3412")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("ACTIVE STUDY STREAK", 410f, y2 + 43f, paint)

        y2 += 75f

        // 3. NEET PREPARATION INSIGHTS & STUDY DISCIPLINE CARD
        paint.color = Color.parseColor("#F8FAFC")
        canvas2.drawRoundRect(25f, y2, 570f, y2 + 88f, 10f, 10f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas2.drawRoundRect(25f, y2, 570f, y2 + 88f, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("🎯 ASPIRANT ACADEMIC READINESS INSIGHTS", 40f, y2 + 24f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("• Practice Consistency: Balanced MCQ coverage across Physics, Chemistry, and Biology.", 40f, y2 + 44f, paint)
        canvas2.drawText("• Mock Test Routine: Regular timed simulations help identify weak areas and error patterns.", 40f, y2 + 60f, paint)
        canvas2.drawText("• Active Revision: Continue spaced repetition with Flashcards and high-yield NCERT checklists.", 40f, y2 + 76f, paint)

        // Page 2 Footer
        paint.color = Color.parseColor("#E2E8F0")
        canvas2.drawLine(25f, 790f, 570f, 790f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas2.drawText("Generated by Lakshya Prep • Official Academic Dossier • Verified Academic Tracking", 25f, 810f, paint)
        canvas2.drawText("Page 2 of 2", 520f, 810f, paint)

        pdfDocument.finishPage(page2)

        return pdfDocument
    }

    private fun drawDefaultAvatarBadge(canvas: Canvas, paint: Paint, x: Float, y: Float, size: Int, name: String) {
        val radius = size / 2f
        val cx = x + radius
        val cy = y + radius

        paint.color = Color.parseColor("#4F46E5")
        canvas.drawCircle(cx, cy, radius, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "L"
        val textWidth = paint.measureText(initial)
        canvas.drawText(initial, cx - (textWidth / 2f), cy + 8f, paint)
    }

    suspend fun savePdfToStorage(context: Context, data: ReportData): SaveResult = withContext(Dispatchers.IO) {
        try {
            val sanitizedCandidate = data.candidateName.replace(Regex("[^a-zA-Z0-9_]"), "_").take(15)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = if (sanitizedCandidate.isNotBlank()) "Lakshya_Academic_Report_${sanitizedCandidate}_$timeStamp.pdf" else "Lakshya_Academic_Report_$timeStamp.pdf"

            val pdfDocument = createPdfDocument(context, data)

            // Cache fallback file for easy internal access/sharing
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { os ->
                pdfDocument.writeTo(os)
            }

            var savedUri: Uri? = null
            var savedFile: File? = null
            var displayPath = "Downloads/$fileName"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Lakshya_Reports")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = context.contentResolver.insert(collection, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        pdfDocument.writeTo(os)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                    savedUri = uri
                    displayPath = "Downloads/Lakshya_Reports/$fileName"
                }
            } else {
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Lakshya_Reports")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { os ->
                    pdfDocument.writeTo(os)
                }
                savedFile = targetFile
                savedUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
                displayPath = targetFile.absolutePath

                try {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf("application/pdf"),
                        null
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to scan file for media scanner", e)
                }
            }

            pdfDocument.close()

            val finalUri = savedUri ?: FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val finalFile = savedFile ?: cacheFile

            SaveResult(
                success = true,
                uri = finalUri,
                file = finalFile,
                displayPath = displayPath,
                message = "Saved successfully to $displayPath"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PDF to storage", e)
            SaveResult(
                success = false,
                message = e.localizedMessage ?: "Failed to save PDF"
            )
        }
    }

    suspend fun generatePdf(context: Context, data: ReportData): File? {
        val result = savePdfToStorage(context, data)
        return result.file
    }

    fun openPdf(context: Context, uri: Uri? = null, file: File? = null) {
        try {
            val targetUri = uri ?: file?.let {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            } ?: return

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open Academic Progress Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No PDF viewer app found to view directly", e)
            Toast.makeText(context, "Saved to Downloads! No PDF viewer app found to view directly.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error while trying to open PDF", e)
            Toast.makeText(context, "Saved to Downloads! Unable to open PDF.", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdf(context: Context, file: File) {
        sharePdf(context, null, file)
    }

    fun sharePdf(context: Context, uri: Uri? = null, file: File? = null) {
        try {
            val targetUri = uri ?: file?.let {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            } ?: return

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, targetUri)
                putExtra(Intent.EXTRA_SUBJECT, "Lakshya Prep Academic Progress Report")
                putExtra(Intent.EXTRA_TEXT, "Here is my official Lakshya Prep academic progress dossier.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Academic Report PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app found to share PDF", e)
            Toast.makeText(context, "No app found to share PDF.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error while trying to share PDF", e)
        }
    }
}
