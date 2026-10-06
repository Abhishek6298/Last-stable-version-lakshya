package com.example.workers

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import java.util.Calendar

object NotificationScheduler {
    
    fun scheduleAllReminders(context: Context, forceUpdate: Boolean = false) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        
        scheduleReminder(context, "Daily Reminder", prefs.getInt("pref_time_daily_reminder", 8), "daily_reminder_work", forceUpdate) // Default 8 AM
        scheduleReminder(context, "Daily Target", prefs.getInt("pref_time_daily_target", 9), "daily_target_work", forceUpdate) // Default 9 AM
        scheduleReminder(context, "New Content", prefs.getInt("pref_time_new_content", 12), "new_content_work", forceUpdate) // Default 12 PM
        scheduleReminder(context, "Revision", prefs.getInt("pref_time_revision", 14), "revision_reminder_work", forceUpdate) // Default 2 PM
        scheduleReminder(context, "Practice Test", prefs.getInt("pref_time_practice_test", 17), "practice_test_work", forceUpdate) // Default 5 PM
        scheduleReminder(context, "Timetable", prefs.getInt("pref_time_timetable", 18), "timetable_reminder_work", forceUpdate) // Default 6 PM
        scheduleReminder(context, "Mock Test", prefs.getInt("pref_time_mock_test", 20), "mock_test_reminder_work", forceUpdate) // Default 8 PM
    }

    private fun scheduleReminder(context: Context, type: String, hourOfDay: Int, uniqueWorkName: String, forceUpdate: Boolean = false) {
        val inputData = Data.Builder()
            .putString("NOTIFICATION_TYPE", type)
            .build()

        // Calculate initial delay
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (dueDate.timeInMillis <= currentDate.timeInMillis) {
            dueDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        val timeDiff = (dueDate.timeInMillis - currentDate.timeInMillis).coerceAtLeast(60000L) // At least 1 minute into the future

        val reminderRequest = PeriodicWorkRequestBuilder<StudyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        val policy = if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName,
            policy,
            reminderRequest
        )
    }

    fun fireTestNotificationNow(context: Context, type: String) {
        val inputData = Data.Builder()
            .putString("NOTIFICATION_TYPE", type)
            .build()

        val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<StudyReminderWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeRequest)
    }
}
