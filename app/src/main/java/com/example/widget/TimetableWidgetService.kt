package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.AppDatabase
import com.example.ui.components.loadSavedTimetable

class TimetableWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TimetableRemoteViewsFactory(applicationContext, intent)
    }
}

class TimetableRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    enum class ItemType { TIMETABLE_SLOT, GOAL }

    data class TimetableRowItem(
        val type: ItemType,
        val idStr: String? = null,
        val idInt: Int? = null,
        val timeBadge: String,
        val title: String,
        val subtitle: String,
        val isCompleted: Boolean,
        val badgeColorHex: String
    )

    private val itemList = mutableListOf<TimetableRowItem>()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        itemList.clear()
        try {
            // 1. Fetch AI Timetable Plan (all slots)
            val timetablePlan = loadSavedTimetable(context)
            if (timetablePlan.slots.isNotEmpty()) {
                timetablePlan.slots.forEach { slot ->
                    val isBreak = slot.subject.equals("Break", ignoreCase = true)
                    val (icon, colorHex) = when {
                        isBreak -> "☕ " to "#64748B"
                        slot.subject.contains("phy", ignoreCase = true) -> "⚛️ " to "#38BDF8"
                        slot.subject.contains("chem", ignoreCase = true) -> "🧪 " to "#FBBF24"
                        slot.subject.contains("bio", ignoreCase = true) || slot.subject.contains("botan", ignoreCase = true) || slot.subject.contains("zool", ignoreCase = true) -> "🧬 " to "#34D399"
                        slot.subject.contains("rev", ignoreCase = true) -> "📚 " to "#F472B6"
                        else -> "🎯 " to "#818CF8"
                    }

                    itemList.add(
                        TimetableRowItem(
                            type = ItemType.TIMETABLE_SLOT,
                            idStr = slot.id,
                            timeBadge = slot.startTime,
                            title = icon + slot.title,
                            subtitle = if (slot.description.isNotBlank()) slot.description else "${slot.subject} • ${slot.durationMinutes} min",
                            isCompleted = slot.isCompleted,
                            badgeColorHex = colorHex
                        )
                    )
                }
            }

            // 2. Fetch Daily Goals from Room Database
            val db = AppDatabase.getDatabase(context)
            val realGoals = kotlinx.coroutines.runBlocking { db.appDao().getRecentGoalsDirect() }
            realGoals.forEach { goal ->
                val isDone = goal.status.equals("completed", ignoreCase = true)
                val icon = when (goal.subject.lowercase()) {
                    "physics" -> "⚛️ "
                    "chemistry" -> "🧪 "
                    "biology" -> "🧬 "
                    else -> "🎯 "
                }
                itemList.add(
                    TimetableRowItem(
                        type = ItemType.GOAL,
                        idInt = goal.id,
                        timeBadge = "Goal",
                        title = icon + goal.text,
                        subtitle = "${goal.subject} Target",
                        isCompleted = isDone,
                        badgeColorHex = "#A855F7"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        itemList.clear()
    }

    override fun getCount(): Int = itemList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= itemList.size) return null
        val item = itemList[position]

        val rv = RemoteViews(context.packageName, R.layout.widget_timetable_item)
        rv.setTextViewText(R.id.tv_item_check, if (item.isCompleted) "✅" else "⬜")
        rv.setTextViewText(R.id.tv_item_time, item.timeBadge)
        rv.setTextViewText(R.id.tv_item_title, item.title)

        if (item.subtitle.isNotBlank()) {
            rv.setTextViewText(R.id.tv_item_subtitle, item.subtitle)
            rv.setViewVisibility(R.id.tv_item_subtitle, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.tv_item_subtitle, View.GONE)
        }

        if (item.isCompleted) {
            rv.setTextColor(R.id.tv_item_title, Color.parseColor("#94A3B8"))
        } else {
            rv.setTextColor(R.id.tv_item_title, Color.parseColor("#F1F5F9"))
        }

        // Fill-in Intent for clicking the row or checkbox
        val fillInIntent = Intent().apply {
            putExtra(InteractiveTaskDppWidgetProvider.EXTRA_ITEM_TYPE, item.type.name)
            putExtra(InteractiveTaskDppWidgetProvider.EXTRA_SLOT_ID, item.idStr)
            putExtra(InteractiveTaskDppWidgetProvider.EXTRA_GOAL_ID, item.idInt ?: -1)
            putExtra(InteractiveTaskDppWidgetProvider.EXTRA_IS_DONE, item.isCompleted)
        }

        rv.setOnClickFillInIntent(R.id.row_timetable_item, fillInIntent)
        rv.setOnClickFillInIntent(R.id.tv_item_check, fillInIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
