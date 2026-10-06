package com.example.ui

import com.example.ui.screens.calculateStreak
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.system.measureNanoTime

class HabitTrackerPerformanceTest {

    @Test
    fun testCalculateStreakCorrectness() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val today = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dayBeforeYesterday = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -2) // gap
        val fourDaysAgo = sdf.format(cal.time)

        val completedDatesList = listOf(today, yesterday, dayBeforeYesterday, fourDaysAgo)

        val streakFromList = calculateStreak(completedDatesList, sdf)

        assertEquals(3, streakFromList)
    }

    @Test
    fun benchmarkLookupPerformance() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val datesList = mutableListOf<String>()

        // Create 1000 completed dates
        for (i in 0 until 1000) {
            datesList.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val datesSet = datesList.toSet()

        // Generate 30 query dates (simulating last 30 days)
        cal.time = java.util.Date()
        val queryDates = (0 until 30).map {
            val d = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            d
        }

        val iterations = 10_000

        // Benchmark List.contains
        val listTimeNano = measureNanoTime {
            repeat(iterations) {
                for (dateStr in queryDates) {
                    datesList.contains(dateStr)
                }
            }
        }

        // Benchmark Set.contains
        val setTimeNano = measureNanoTime {
            repeat(iterations) {
                for (dateStr in queryDates) {
                    datesSet.contains(dateStr)
                }
            }
        }

        println("BENCHMARK_LIST_LOOKUP_TIME_NS: $listTimeNano")
        println("BENCHMARK_SET_LOOKUP_TIME_NS: $setTimeNano")
        val speedup = listTimeNano.toDouble() / setTimeNano.toDouble()
        println("BENCHMARK_SPEEDUP: ${String.format("%.2f", speedup)}x")
    }
}
