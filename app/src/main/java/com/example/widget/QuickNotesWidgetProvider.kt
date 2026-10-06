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
import com.example.ui.QuickNoteActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickNotesWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("widget_notes_current_index", 0)

        when (intent.action) {
            ACTION_NEXT_NOTE -> {
                prefs.edit().putInt("widget_notes_current_index", currentIndex + 1).apply()
                updateAll(context)
            }
            ACTION_PREV_NOTE -> {
                val prev = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                prefs.edit().putInt("widget_notes_current_index", prev).apply()
                updateAll(context)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val ACTION_NEXT_NOTE = "com.example.widget.ACTION_NEXT_NOTE"
        const val ACTION_PREV_NOTE = "com.example.widget.ACTION_PREV_NOTE"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.widget_quick_notes)
                val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                // 1. Fetch real notes from AppDatabase
                val realNotes = try {
                    AppDatabase.getDatabase(context).appDao().getRecentNotesDirect()
                } catch (_: Exception) {
                    emptyList()
                }

                val totalNotes = realNotes.size
                var currentIndex = prefs.getInt("widget_notes_current_index", 0)
                if (totalNotes > 0 && currentIndex >= totalNotes) {
                    currentIndex = 0
                    prefs.edit().putInt("widget_notes_current_index", 0).apply()
                }

                val title: String
                val body: String
                val currentNoteId: Int
                val currentNoteTitle: String
                val currentNoteContent: String

                if (realNotes.isNotEmpty()) {
                    val note = realNotes[currentIndex % realNotes.size]
                    currentNoteId = note.id
                    currentNoteTitle = note.title
                    currentNoteContent = note.content
                    title = if (note.title.isNotBlank()) "📝 ${note.title}" else "📝 Quick Note #${currentIndex + 1}"
                    body = note.content.ifBlank { "Tap to edit this note..." }
                    views.setTextViewText(R.id.tv_note_index, "${currentIndex + 1}/$totalNotes")
                } else {
                    currentNoteId = -1
                    currentNoteTitle = ""
                    currentNoteContent = ""
                    title = "📝 Quick Notes"
                    body = "No notes yet. Tap '✍️ + Write' to write your quick note or formula right here!"
                    views.setTextViewText(R.id.tv_note_index, "0/0")
                }

                views.setTextViewText(R.id.tv_notes_badge, "📝 QUICK NOTES")
                views.setTextViewText(R.id.tv_note_title, title)
                views.setTextViewText(R.id.tv_note_body, body)

                val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

                // Navigation PendingIntents
                val nextIntent = Intent(context, QuickNotesWidgetProvider::class.java).apply {
                    action = ACTION_NEXT_NOTE
                }
                views.setOnClickPendingIntent(R.id.btn_next_note, PendingIntent.getBroadcast(context, 201, nextIntent, piFlags))

                val prevIntent = Intent(context, QuickNotesWidgetProvider::class.java).apply {
                    action = ACTION_PREV_NOTE
                }
                views.setOnClickPendingIntent(R.id.btn_prev_note, PendingIntent.getBroadcast(context, 202, prevIntent, piFlags))

                // Instant OnePlus-style Quick Note Writer Launcher
                val writeIntent = Intent(context, QuickNoteActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val writePi = PendingIntent.getActivity(context, 203, writeIntent, piFlags)
                views.setOnClickPendingIntent(R.id.btn_write_note, writePi)
                views.setOnClickPendingIntent(R.id.btn_write_new_note_bottom, writePi)

                // Edit or View current Note on card tap
                val editNoteIntent = Intent(context, QuickNoteActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (currentNoteId != -1) {
                        putExtra("EXTRA_NOTE_ID", currentNoteId)
                        putExtra("EXTRA_NOTE_TITLE", currentNoteTitle)
                        putExtra("EXTRA_NOTE_CONTENT", currentNoteContent)
                    }
                }
                val editNotePi = PendingIntent.getActivity(context, 204, editNoteIntent, piFlags)
                views.setOnClickPendingIntent(R.id.card_note_content, editNotePi)

                // Open Notes / Habits screen in Main App
                val appNotesIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("NAVIGATE_TO", "HABITS_SCREEN")
                }
                val appNotesPi = PendingIntent.getActivity(context, 200, appNotesIntent, piFlags)
                views.setOnClickPendingIntent(R.id.widget_notes_root, appNotesPi)
                views.setOnClickPendingIntent(R.id.btn_open_notes_app, appNotesPi)

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
                ComponentName(context, QuickNotesWidgetProvider::class.java)
            )
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
