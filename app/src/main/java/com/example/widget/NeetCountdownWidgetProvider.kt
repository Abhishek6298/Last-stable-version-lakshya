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

class NeetCountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.widget_neet_countdown)
                val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                // 1. Read Target Millis (Synced with AppViewModel & Settings)
                var targetMillis = prefs.getLong("neet_target_millis", 0L)
                if (targetMillis <= 0L) {
                    targetMillis = prefs.getLong("neet_target_date_millis", 0L)
                }
                if (targetMillis <= 0L) {
                    val strDate = prefs.getString("neet_target_date", null)
                    if (!strDate.isNullOrBlank()) {
                        try {
                            targetMillis = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(strDate)?.time ?: 0L
                        } catch (_: Exception) {}
                    }
                }

                if (targetMillis <= System.currentTimeMillis()) {
                    val cal = Calendar.getInstance()
                    if (cal.get(Calendar.MONTH) >= Calendar.MAY) {
                        cal.add(Calendar.YEAR, 1)
                    }
                    cal.set(Calendar.MONTH, Calendar.MAY)
                    cal.set(Calendar.DAY_OF_MONTH, 3)
                    cal.set(Calendar.HOUR_OF_DAY, 14)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    targetMillis = cal.timeInMillis
                }

                val examName = prefs.getString("active_exam_goal", "NEET") ?: "NEET"
                val diffMillis = (targetMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                val daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis)

                // 2. Format Target Date
                val targetDateFormatted = try {
                    SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(targetMillis))
                } catch (_: Exception) {
                    "May 3, 2026"
                }

                // 3. Compute Real Synchronized Study Streak
                var streak = prefs.getInt("study_streak_days", 0)
                try {
                    val db = AppDatabase.getDatabase(context)
                    val logs = db.appDao().getAllStudyLogsDirect()
                    val practices = db.appDao().getAllDailyPracticesDirect()
                    
                    val activeDates = mutableSetOf<String>()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    logs.forEach { 
                        if (it.durationSeconds > 0 && it.timestamp > 0) {
                            activeDates.add(sdf.format(Date(it.timestamp)))
                        }
                    }
                    practices.forEach { 
                        if (it.physicsSolved > 0 || it.chemistrySolved > 0 || it.biologySolved > 0) {
                            activeDates.add(it.date)
                        }
                    }
                    
                    val cal = Calendar.getInstance()
                    val todayStr = sdf.format(cal.time)
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = sdf.format(cal.time)
                    
                    val isTodayActive = activeDates.contains(todayStr)
                    val isYesterdayActive = activeDates.contains(yesterdayStr)
                    
                    if (isTodayActive || isYesterdayActive) {
                        var count = 0
                        val checkCal = Calendar.getInstance()
                        if (!isTodayActive) {
                            checkCal.add(Calendar.DAY_OF_YEAR, -1)
                        }
                        while (true) {
                            val dStr = sdf.format(checkCal.time)
                            if (activeDates.contains(dStr)) {
                                count++
                                checkCal.add(Calendar.DAY_OF_YEAR, -1)
                            } else {
                                break
                            }
                        }
                        streak = count
                        prefs.edit().putInt("study_streak_days", streak).apply()
                    } else {
                        streak = 0
                        prefs.edit().putInt("study_streak_days", 0).apply()
                    }
                } catch (_: Exception) {}

                val streakBadgeText = if (streak > 0) "🔥 $streak ${if (streak == 1) "Day" else "Days"} Streak" else "⚡ Start Streak!"

                // 4. Update RemoteViews
                views.setTextViewText(R.id.tv_countdown_badge, "⚡ $examName GOAL")
                views.setTextViewText(R.id.tv_streak_badge, streakBadgeText)
                views.setTextViewText(R.id.tv_countdown_days, daysLeft.toString())
                views.setTextViewText(R.id.tv_countdown_label, "DAYS TO $examName")
                views.setTextViewText(R.id.tv_target_date_info, "🎯 Target: $targetDateFormatted")

                // 5. 1-Tap Launch MainActivity
                val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val rootIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(context, 0, rootIntent, piFlags)
                )

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
                ComponentName(context, NeetCountdownWidgetProvider::class.java)
            )
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
