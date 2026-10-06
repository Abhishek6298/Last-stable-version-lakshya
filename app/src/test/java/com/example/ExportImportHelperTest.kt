package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.MockTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportImportHelperTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testImportMockTests_restoresDataCorrectly() {
        runBlocking {
            val root = JSONObject()
            val mockTestsArray = JSONArray()

            val test1 = JSONObject().apply {
                put("testName", "Full Mock Test 1")
                put("score", 650)
                put("physics", 160)
                put("chemistry", 150)
                put("biology", 340)
                put("negative", 20)
                put("timestamp", 1700000000000L)
                put("geminiAnalysis", "Good performance in Bio")
            }
            val test2 = JSONObject().apply {
                put("testName", "Full Mock Test 2")
                put("score", 680)
                put("physics", 170)
                put("chemistry", 160)
                put("biology", 350)
                put("negative", 12)
                put("timestamp", 1700086400000L)
                put("geminiAnalysis", "Improved Physics")
            }
            mockTestsArray.put(test1)
            mockTestsArray.put(test2)
            root.put("mockTests", mockTestsArray)

            val dao = db.appDao()

            val testsArray = root.getJSONArray("mockTests")
            val mockTestsList = ArrayList<MockTest>(testsArray.length())
            for (i in 0 until testsArray.length()) {
                val obj = testsArray.getJSONObject(i)
                mockTestsList.add(
                    MockTest(
                        testName = obj.optString("testName", "Mock Test"),
                        score = obj.optInt("score", 0),
                        physics = obj.optInt("physics", 0),
                        chemistry = obj.optInt("chemistry", 0),
                        biology = obj.optInt("biology", 0),
                        negative = obj.optInt("negative", 0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        geminiAnalysis = obj.optString("geminiAnalysis", "").ifBlank { null }
                    )
                )
            }
            dao.insertMockTests(mockTestsList)

            val restoredTests = dao.getAllMockTests().first()
            assertEquals(2, restoredTests.size)
            assertEquals("Full Mock Test 1", restoredTests[0].testName)
            assertEquals(650, restoredTests[0].score)
            assertEquals(160, restoredTests[0].physics)
            assertEquals("Good performance in Bio", restoredTests[0].geminiAnalysis)

            assertEquals("Full Mock Test 2", restoredTests[1].testName)
            assertEquals(680, restoredTests[1].score)
            assertEquals("Improved Physics", restoredTests[1].geminiAnalysis)
        }
    }

    @Test
    fun benchmarkBatchVsIterativeInsert() {
        runBlocking {
            val count = 500
            val testItems = List(count) { i ->
                MockTest(
                    testName = "Mock Test #$i",
                    score = 500 + (i % 200),
                    physics = 140,
                    chemistry = 150,
                    biology = 300,
                    negative = 10,
                    timestamp = System.currentTimeMillis() + i
                )
            }

            val dao = db.appDao()

            // Measure Iterative Insertion
            val iterativeTime = measureTimeMillis {
                for (test in testItems) {
                    dao.insertMockTest(test)
                }
            }

            dao.deleteAllMockTests()

            // Measure Batch Insertion
            val batchTime = measureTimeMillis {
                dao.insertMockTests(testItems)
            }

            val restoredCount = dao.getAllMockTests().first().size
            assertEquals(count, restoredCount)

            println("BENCHMARK RESULT ($count items):")
            println("Iterative Insertion Time: $iterativeTime ms")
            println("Batch Insertion Time:     $batchTime ms")

            assertTrue(
                "Batch insertion ($batchTime ms) should be faster than or equal to iterative ($iterativeTime ms)",
                batchTime <= iterativeTime
            )
        }
    }
}
