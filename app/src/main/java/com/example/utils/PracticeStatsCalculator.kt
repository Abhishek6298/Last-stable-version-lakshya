package com.example.utils

import com.example.data.DailyPractice
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class PracticePeriodStats(
    val physicsSolved: Int,
    val chemistrySolved: Int,
    val biologySolved: Int,
    val totalSolved: Int,
    val physicsTarget: Int,
    val chemistryTarget: Int,
    val biologyTarget: Int,
    val totalTarget: Int
)

object PracticeStatsCalculator {
    fun calculateStatsForDays(dailyPractices: List<DailyPractice>, daysCount: Int): PracticePeriodStats {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        // Days ago cutoff timestamp
        val cutoffTime = cal.timeInMillis - ((daysCount - 1) * 24 * 3600 * 1000L)
        
        var pSolved = 0
        var cSolved = 0
        var bSolved = 0
        var pTarget = 0
        var cTarget = 0
        var bTarget = 0
        
        dailyPractices.forEach { dp ->
            val t = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dp.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
            if (t >= cutoffTime) {
                pSolved += dp.physicsSolved
                cSolved += dp.chemistrySolved
                bSolved += dp.biologySolved
                pTarget += dp.physicsTarget
                cTarget += dp.chemistryTarget
                bTarget += dp.biologyTarget
            }
        }
        
        val basePTarget = dailyPractices.lastOrNull()?.physicsTarget ?: 45
        val baseCTarget = dailyPractices.lastOrNull()?.chemistryTarget ?: 45
        val baseBTarget = dailyPractices.lastOrNull()?.biologyTarget ?: 90

        val finalPTarget = maxOf(pTarget, basePTarget * daysCount)
        val finalCTarget = maxOf(cTarget, baseCTarget * daysCount)
        val finalBTarget = maxOf(bTarget, baseBTarget * daysCount)

        return PracticePeriodStats(
            physicsSolved = pSolved,
            chemistrySolved = cSolved,
            biologySolved = bSolved,
            totalSolved = pSolved + cSolved + bSolved,
            physicsTarget = finalPTarget,
            chemistryTarget = finalCTarget,
            biologyTarget = finalBTarget,
            totalTarget = finalPTarget + finalCTarget + finalBTarget
        )
    }
}
