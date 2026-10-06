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

class MistakeNotebookWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT_MISTAKE) {
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val current = prefs.getInt("widget_mistake_current_index", 0)
            prefs.edit().putInt("widget_mistake_current_index", current + 1).apply()
            updateAll(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val ACTION_NEXT_MISTAKE = "com.example.widget.ACTION_NEXT_MISTAKE"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.widget_mistake_notebook)
                val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                // 1. Fetch real mistakes from AppDatabase
                val db = AppDatabase.getDatabase(context)
                val realMistakes = try {
                    db.appDao().getRecentMistakesDirect()
                } catch (_: Exception) {
                    emptyList()
                }

                val totalCount = realMistakes.size
                var idx = prefs.getInt("widget_mistake_current_index", 0)
                if (totalCount > 0 && idx >= totalCount) {
                    idx = 0
                    prefs.edit().putInt("widget_mistake_current_index", 0).apply()
                }

                val subjectText: String
                val questionText: String
                val solutionText: String

                if (realMistakes.isNotEmpty()) {
                    val m = realMistakes[idx % realMistakes.size]
                    subjectText = "📕 Mistake Book • ${m.subject.uppercase()}"
                    questionText = "Q: " + (if (m.question.isNotBlank()) m.question else if (m.chapter.isNotBlank()) m.chapter else "Mistake Question from ${m.subject}")
                    solutionText = "⚠️ Error Type: " + (if (m.mistakeType.isNotBlank()) m.mistakeType else "Conceptual Review")
                } else {
                    subjectText = "📕 MISTAKE NOTEBOOK"
                    questionText = "No mistakes logged yet! Solve MCQs and add wrong questions to your notebook for active revision."
                    solutionText = "💡 Revise conceptual traps, calculation errors & formula blunders here."
                }

                views.setTextViewText(R.id.tv_mistake_subject, subjectText)
                views.setTextViewText(R.id.tv_mistake_question, questionText)
                views.setTextViewText(R.id.tv_mistake_solution, solutionText)

                val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

                // Next randomizer intent
                val nextIntent = Intent(context, MistakeNotebookWidgetProvider::class.java).apply {
                    action = ACTION_NEXT_MISTAKE
                }
                views.setOnClickPendingIntent(
                    R.id.btn_next_mistake,
                    PendingIntent.getBroadcast(context, 301, nextIntent, piFlags)
                )

                // Launch Mistake Screen in App
                val rootIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("NAVIGATE_TO", "MISTAKE_NOTEBOOK_SCREEN")
                }
                val rootPi = PendingIntent.getActivity(context, 300, rootIntent, piFlags)
                views.setOnClickPendingIntent(R.id.widget_mistake_root, rootPi)
                views.setOnClickPendingIntent(R.id.btn_open_mistakes_app, rootPi)

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
                ComponentName(context, MistakeNotebookWidgetProvider::class.java)
            )
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
