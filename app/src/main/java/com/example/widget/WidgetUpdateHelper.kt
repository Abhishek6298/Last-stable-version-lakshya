package com.example.widget

import android.content.Context

object WidgetUpdateHelper {
    fun updateAllWidgets(context: Context) {
        try {
            NeetCountdownWidgetProvider.updateAll(context)
            InteractiveTaskDppWidgetProvider.updateAll(context)
            QuickNotesWidgetProvider.updateAll(context)
            MistakeNotebookWidgetProvider.updateAll(context)
            UpcomingMockTestWidgetProvider.updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
