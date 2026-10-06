package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.StudyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StudyTimerService : Service() {

    companion object {
        private const val TAG = "StudyTimerService"
        const val CHANNEL_ID = "study_timer_foreground_channel"
        const val NOTIFICATION_ID = 2027
        
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_SYNC = "com.example.service.ACTION_SYNC"

        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_CHAPTER = "extra_chapter"
        const val EXTRA_START_REALTIME = "extra_start_realtime"
        const val EXTRA_BASE_SECONDS = "extra_base_seconds"
        const val EXTRA_IS_COUNTDOWN = "extra_is_countdown"
        const val EXTRA_TARGET_SECONDS = "extra_target_seconds"

        var isServiceRunning = false
            private set

        fun startTimer(
            context: Context,
            subject: String,
            chapter: String,
            startRealtime: Long,
            baseSeconds: Int,
            isCountdown: Boolean = false,
            targetSeconds: Int = 0
        ) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_CHAPTER, chapter)
                putExtra(EXTRA_START_REALTIME, startRealtime)
                putExtra(EXTRA_BASE_SECONDS, baseSeconds)
                putExtra(EXTRA_IS_COUNTDOWN, isCountdown)
                putExtra(EXTRA_TARGET_SECONDS, targetSeconds)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseTimer(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null

    private var subject: String = "Physics"
    private var chapter: String = "Units and Measurements"
    private var startRealtime: Long = 0L
    private var baseSeconds: Int = 0
    private var isCountdown: Boolean = false
    private var targetSeconds: Int = 0
    private var sessionDate: String = ""
    private var sessionStartEpoch: Long = 0L

    private fun getTodayDateKey(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun getTodayMidnightMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Physics"
                chapter = intent.getStringExtra(EXTRA_CHAPTER) ?: "General Study"
                startRealtime = intent.getLongExtra(EXTRA_START_REALTIME, SystemClock.elapsedRealtime())
                baseSeconds = intent.getIntExtra(EXTRA_BASE_SECONDS, 0)
                isCountdown = intent.getBooleanExtra(EXTRA_IS_COUNTDOWN, false)
                targetSeconds = intent.getIntExtra(EXTRA_TARGET_SECONDS, 0)
                sessionDate = getTodayDateKey()
                sessionStartEpoch = System.currentTimeMillis()

                startForegroundNotification()
                startTicker()
            }
            ACTION_PAUSE -> {
                saveCurrentElapsedToPrefs(isRunning = false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP -> {
                handleStopAndSave()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "NEETTracker:StudyTimerWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // Max 12 hours timeout safety
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live study focus timer in the background and on lock screen"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getElapsedSeconds(): Int {
        if (startRealtime <= 0L) return baseSeconds
        val currentDelta = ((SystemClock.elapsedRealtime() - startRealtime) / 1000L).toInt()
        return baseSeconds + currentDelta.coerceAtLeast(0)
    }

    private fun formatTime(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }

    private fun buildNotification(): Notification {
        val totalSeconds = getElapsedSeconds()
        val timeFormatted = formatTime(totalSeconds)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, StudyTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val pauseIntent = Intent(this, StudyTimerService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pendingPause = PendingIntent.getService(
            this, 2, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val chronometerBase = SystemClock.elapsedRealtime() - (totalSeconds * 1000L)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏱️ Studying $subject ($timeFormatted)")
            .setContentText("$chapter • Focus Session Active")
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis() - (totalSeconds * 1000L))
            .addAction(R.mipmap.ic_launcher, "Pause ⏸️", pendingPause)
            .addAction(R.mipmap.ic_launcher, "Stop & Save 💾", pendingStop)

        return builder.build()
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (true) {
                delay(1000)

                // Check midnight rollover
                val currentTodayKey = getTodayDateKey()
                if (sessionDate.isNotEmpty() && sessionDate != currentTodayKey) {
                    val todayMidnight = getTodayMidnightMillis()
                    val totalPrevDaySeconds = getElapsedSeconds()

                    if (totalPrevDaySeconds > 0) {
                        val dao = AppDatabase.getDatabase(applicationContext).appDao()
                        serviceScope.launch(Dispatchers.IO) {
                            try {
                                dao.insertStudyLog(
                                    StudyLog(
                                        subject = subject,
                                        chapter = chapter.ifBlank { "General Study" },
                                        durationSeconds = totalPrevDaySeconds,
                                        timestamp = todayMidnight - 1000L
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to insert midnight rollover study log", e)
                            }
                        }
                    }

                    // Reset for new day starting at 00:00:00 with same subject & chapter
                    val nowRealtime = SystemClock.elapsedRealtime()
                    val currentEpoch = System.currentTimeMillis()
                    val deltaSinceMidnight = ((currentEpoch - todayMidnight) / 1000L).toInt().coerceAtLeast(0)

                    baseSeconds = 0
                    startRealtime = nowRealtime - (deltaSinceMidnight * 1000L)
                    sessionDate = currentTodayKey
                    sessionStartEpoch = todayMidnight

                    val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("timer_is_running", true)
                        .putLong("timer_start_realtime", startRealtime)
                        .putLong("timer_session_start_epoch", sessionStartEpoch)
                        .putString("timer_session_date", sessionDate)
                        .putInt("timer_accumulated_seconds", 0)
                        .putString("timer_subject", subject)
                        .putString("timer_chapter", chapter)
                        .putInt("timer_last_known_seconds", deltaSinceMidnight)
                        .apply()

                    startForegroundNotification()
                }

                val totalSeconds = getElapsedSeconds()

                // Save periodically to shared prefs so state is always fresh
                val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("timer_is_running", true)
                    .putLong("timer_start_realtime", startRealtime)
                    .putLong("timer_session_start_epoch", sessionStartEpoch)
                    .putString("timer_session_date", sessionDate)
                    .putInt("timer_accumulated_seconds", baseSeconds)
                    .putString("timer_subject", subject)
                    .putString("timer_chapter", chapter)
                    .putInt("timer_last_known_seconds", totalSeconds)
                    .apply()

                // Check countdown target
                if (isCountdown && targetSeconds > 0 && totalSeconds >= targetSeconds) {
                    handleStopAndSave()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
            }
        }
    }

    private fun saveCurrentElapsedToPrefs(isRunning: Boolean) {
        val totalSeconds = getElapsedSeconds()
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("timer_is_running", isRunning)
            .putLong("timer_start_realtime", 0L)
            .putLong("timer_session_start_epoch", sessionStartEpoch)
            .putString("timer_session_date", sessionDate)
            .putInt("timer_accumulated_seconds", totalSeconds)
            .putString("timer_subject", subject)
            .putString("timer_chapter", chapter)
            .putInt("timer_last_known_seconds", totalSeconds)
            .apply()
    }

    private fun handleStopAndSave() {
        val totalSeconds = getElapsedSeconds()
        if (totalSeconds > 0) {
            val dao = AppDatabase.getDatabase(applicationContext).appDao()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    dao.insertStudyLog(
                        StudyLog(
                            subject = subject,
                            chapter = chapter.ifBlank { "General Study" },
                            durationSeconds = totalSeconds
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert study log", e)
                }
            }
        }

        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("timer_is_running", false)
            .putLong("timer_start_realtime", 0L)
            .putLong("timer_session_start_epoch", 0L)
            .putString("timer_session_date", "")
            .putInt("timer_accumulated_seconds", 0)
            .putInt("timer_last_known_seconds", 0)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        tickerJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
    }
}
