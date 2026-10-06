package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class StudyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val notificationType = inputData.getString("NOTIFICATION_TYPE") ?: "Daily Reminder"
            val prompt = when (notificationType) {
                "Mock Test" -> "Generate a very short, motivating push notification message (under 30 words) for a NEET 2027 aspirant reminding them about an upcoming mock test. Include an emoji."
                "Revision" -> "Generate a very short, motivating push notification message (under 30 words) for a NEET 2027 aspirant reminding them to revise biology or chemistry today. Include an emoji."
                "Timetable" -> "Generate a short, encouraging push notification (under 30 words) for a NEET aspirant telling them to stick to their study timetable today. Include an emoji."
                "Daily Target" -> "Generate a very short, exciting push notification (under 30 words) for a NEET aspirant to check and complete their daily targets 🎯. Include an emoji."
                "New Content" -> "Generate a very short push notification (under 30 words) for a NEET aspirant letting them know that new study material, flashcards, or content might be available. Include an emoji."
                "Practice Test" -> "Generate a very short, motivating push notification (under 30 words) reminding a NEET aspirant to take a practice test to boost their speed and accuracy. Include an emoji."
                else -> "Generate a very short, motivating push notification message (under 30 words) for a NEET 2027 aspirant reminding them to study hard today. Include an emoji."
            }

            val message = fetchGeminiMotivation(prompt)
            showNotification(notificationType, message)

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error generating or showing study reminder notification", e)
            // Fallback message
            showNotification("NEET Tracker 2027", "Time to hit the books! Stay focused on your NEET goals! \uD83D\uDCDA")
            Result.success()
        }
    }

    private fun fetchGeminiMotivation(prompt: String): String {
        val apiKey = com.example.data.GeminiChatAssistant.getApiKey(applicationContext)

        if (apiKey.isEmpty()) {
            return "Keep pushing towards your NEET 2027 dream! 🌟"
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            if (responseBody != null) {
                val jsonObject = JSONObject(responseBody)
                val candidates = jsonObject.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text").trim().replace("\"", "")
                    }
                }
            }
        }
        return "Stay focused and study hard for NEET! \uD83D\uDCDA"
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "neet_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NEET Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders for NEET preparation"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = Random.nextInt()
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        private const val TAG = "StudyReminderWorker"
    }
}
