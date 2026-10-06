package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UpcomingMockTestWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.widget_upcoming_mock_test)

                // 1. Fetch upcoming mock tests from AppDatabase
                val db = AppDatabase.getDatabase(context)
                val upcomingTests = try {
                    db.appDao().getUpcomingMockTestsDirect()
                } catch (_: Exception) {
                    emptyList()
                }

                val title: String
                val dateInfo: String
                val countdownBadge: String
                val syllabusText: String

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val nowCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (upcomingTests.isNotEmpty()) {
                    val test = upcomingTests.first()
                    title = test.title.ifBlank { "All India Mock Test" }

                    var daysDiff = 0L
                    var parsedDate: Date? = null
                    try {
                        parsedDate = sdf.parse(test.scheduledDate)
                        if (parsedDate != null) {
                            val diffMillis = parsedDate.time - nowCal.timeInMillis
                            daysDiff = TimeUnit.MILLISECONDS.toDays(diffMillis)
                        }
                    } catch (_: Exception) {}

                    countdownBadge = when {
                        daysDiff < 0L -> "⚠️ Due Today"
                        daysDiff == 0L -> "🔥 Today (02:00 PM)"
                        daysDiff == 1L -> "⏳ Tomorrow"
                        else -> "⏳ In $daysDiff Days"
                    }

                    val formattedDate = parsedDate?.let {
                        try {
                            SimpleDateFormat("EEE, MMM d • 02:00 PM", Locale.getDefault()).format(it)
                        } catch (_: Exception) {
                            test.scheduledDate
                        }
                    } ?: test.scheduledDate

                    dateInfo = "🗓️ $formattedDate"

                    // Subject Syllabus Order: Physics -> Biology -> Chemistry
                    val syllParts = mutableListOf<String>()
                    if (test.physicsSyllabus.isNotBlank()) syllParts.add("Phy: ${test.physicsSyllabus}")
                    if (test.biologySyllabus.isNotBlank()) syllParts.add("Bio: ${test.biologySyllabus}")
                    if (test.chemistrySyllabus.isNotBlank()) syllParts.add("Chem: ${test.chemistrySyllabus}")

                    syllabusText = if (syllParts.isNotEmpty()) {
                        "📚 " + syllParts.joinToString(" • ")
                    } else if (test.syllabusNotes.isNotBlank()) {
                        "📚 " + test.syllabusNotes
                    } else {
                        "📚 Full Syllabus NTA Pattern (Physics, Biology, Chemistry)"
                    }
                } else {
                    title = "No Upcoming Test Scheduled"
                    countdownBadge = "📅 Plan Test"
                    dateInfo = "🗓️ Tap to schedule your next full CBT mock test"
                    syllabusText = "📚 Target: Physics • Biology • Chemistry"
                }

                views.setTextViewText(R.id.tv_mock_badge, "🎯 UPCOMING MOCK TEST")
                views.setTextViewText(R.id.tv_mock_countdown, countdownBadge)
                views.setTextViewText(R.id.tv_mock_title, title)
                views.setTextViewText(R.id.tv_mock_date, dateInfo)
                views.setTextViewText(R.id.tv_mock_syllabus, syllabusText)
                views.setTextViewText(R.id.tv_mock_pattern, "⚡ 720 Marks • NTA CBT")

                // PendingIntents for 1-Tap CBT Launch / Mock Schedule Launch
                val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val cbtIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("NAVIGATE_TO", "AI_CBT_SCREEN")
                }
                val cbtPi = PendingIntent.getActivity(context, 401, cbtIntent, piFlags)

                views.setOnClickPendingIntent(R.id.widget_mock_root, cbtPi)
                views.setOnClickPendingIntent(R.id.card_mock_hero, cbtPi)
                views.setOnClickPendingIntent(R.id.btn_launch_mock_cbt, cbtPi)

                withContext(Dispatchers.Main) {
                    try {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (_: Exception) {}
                }
            }
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, UpcomingMockTestWidgetProvider::class.java)
            )
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
