package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.ui.components.loadSavedTimetable
import com.example.ui.components.saveTimetableToPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InteractiveTaskDppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        when (action) {
            ACTION_ITEM_CLICK -> {
                val itemType = intent.getStringExtra(EXTRA_ITEM_TYPE)
                val isDone = intent.getBooleanExtra(EXTRA_IS_DONE, false)

                CoroutineScope(Dispatchers.IO).launch {
                    if (itemType == "TIMETABLE_SLOT") {
                        val slotId = intent.getStringExtra(EXTRA_SLOT_ID)
                        if (!slotId.isNullOrBlank()) {
                            try {
                                val plan = loadSavedTimetable(context)
                                val updatedSlots = plan.slots.map { slot ->
                                    if (slot.id == slotId) slot.copy(isCompleted = !slot.isCompleted) else slot
                                }
                                saveTimetableToPrefs(context, plan.copy(slots = updatedSlots))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else if (itemType == "GOAL") {
                        val goalId = intent.getIntExtra(EXTRA_GOAL_ID, -1)
                        if (goalId != -1) {
                            try {
                                val newStatus = if (isDone) "todo" else "completed"
                                AppDatabase.getDatabase(context).appDao().updateGoalStatus(goalId, newStatus)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    updateAll(context)
                }
            }
            ACTION_REFRESH_TASKS -> {
                updateAll(context)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.lv_timetable_tasks)
    }

    companion object {
        const val ACTION_ITEM_CLICK = "com.example.widget.ACTION_ITEM_CLICK"
        const val ACTION_REFRESH_TASKS = "com.example.widget.ACTION_REFRESH_TASKS"
        const val EXTRA_ITEM_TYPE = "EXTRA_ITEM_TYPE"
        const val EXTRA_SLOT_ID = "EXTRA_SLOT_ID"
        const val EXTRA_GOAL_ID = "EXTRA_GOAL_ID"
        const val EXTRA_IS_DONE = "EXTRA_IS_DONE"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.widget_interactive_tasks)

                // 1. Calculate summary counts
                val plan = try { loadSavedTimetable(context) } catch (_: Exception) { null }
                val db = AppDatabase.getDatabase(context)
                val goals = try { db.appDao().getRecentGoalsDirect() } catch (_: Exception) { emptyList() }

                val slotCount = plan?.slots?.size ?: 0
                val completedSlots = plan?.slots?.count { it.isCompleted } ?: 0
                val goalCount = goals.size
                val completedGoals = goals.count { it.status.equals("completed", ignoreCase = true) }

                val totalCount = slotCount + goalCount
                val totalCompleted = completedSlots + completedGoals
                val pct = if (totalCount > 0) (totalCompleted * 100 / totalCount) else 0

                views.setTextViewText(R.id.tv_tasks_header, "📅 FULL TIMETABLE (${totalCount} Slots)")
                views.setTextViewText(R.id.tv_tasks_ratio, "$totalCompleted/$totalCount ($pct%)")

                // 2. Set up scrollable ListView adapter
                val serviceIntent = Intent(context, TimetableWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.lv_timetable_tasks, serviceIntent)
                views.setEmptyView(R.id.lv_timetable_tasks, R.id.tv_empty_timetable)

                val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

                // 3. Set up PendingIntent Template for ListView item clicks
                val itemClickIntent = Intent(context, InteractiveTaskDppWidgetProvider::class.java).apply {
                    action = ACTION_ITEM_CLICK
                }
                val itemClickPi = PendingIntent.getBroadcast(context, 2000, itemClickIntent, piFlags)
                views.setPendingIntentTemplate(R.id.lv_timetable_tasks, itemClickPi)

                // 4. Refresh Button
                val refreshIntent = Intent(context, InteractiveTaskDppWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_TASKS
                }
                views.setOnClickPendingIntent(
                    R.id.btn_refresh_tasks,
                    PendingIntent.getBroadcast(context, 2001, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                // 5. Action Buttons
                val timetableAppIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("NAVIGATE_TO", "TIMETABLE_SCREEN")
                }
                views.setOnClickPendingIntent(
                    R.id.btn_open_timetable_app,
                    PendingIntent.getActivity(context, 2002, timetableAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                val addGoalAppIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("NAVIGATE_TO", "TIMETABLE_SCREEN")
                }
                views.setOnClickPendingIntent(
                    R.id.btn_add_goal_app,
                    PendingIntent.getActivity(context, 2003, addGoalAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                withContext(Dispatchers.Main) {
                    try {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_timetable_tasks)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, InteractiveTaskDppWidgetProvider::class.java)
            )
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
            if (ids.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.lv_timetable_tasks)
            }
        }
    }
}
