package com.example.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDatabaseTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var openHelper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addMigrations(
                AppDatabase.MIGRATION_23_24,
                AppDatabase.MIGRATION_24_25,
                AppDatabase.MIGRATION_25_26,
                AppDatabase.MIGRATION_26_27,
                AppDatabase.MIGRATION_27_28,
                AppDatabase.MIGRATION_28_29,
                AppDatabase.MIGRATION_29_30
            )
            .allowMainThreadQueries()
            .build()

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
    }

    @After
    fun tearDown() {
        database.close()
        openHelper.close()
    }

    @Test
    fun testDatabaseCreationAndDaoAccess() {
        val appDao = database.appDao()
        assertNotNull(appDao)
    }

    @Test
    fun testMigration23To24() {
        val db = openHelper.writableDatabase
        AppDatabase.MIGRATION_23_24.migrate(db)
        val cursor = db.query("SELECT count(*) FROM notes")
        assertNotNull(cursor)
        cursor.close()
    }

    @Test
    fun testMigration24To25() {
        val db = openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS `mistake_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL, `question` TEXT NOT NULL, `mistakeType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
        AppDatabase.MIGRATION_24_25.migrate(db)
        val cursor = db.query("SELECT chapter FROM mistake_logs")
        assertNotNull(cursor)
        cursor.close()
    }

    @Test
    fun testMigration25To26() {
        val db = openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS `mistake_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL, `question` TEXT NOT NULL, `mistakeType` TEXT NOT NULL, `chapter` TEXT NOT NULL DEFAULT '', `timestamp` INTEGER NOT NULL)")
        AppDatabase.MIGRATION_25_26.migrate(db)
        val cursor = db.query("SELECT pdfUri, pdfName FROM mistake_logs")
        assertNotNull(cursor)
        cursor.close()
    }

    @Test
    fun testMigration26To27() {
        val db = openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_practice` (`date` TEXT PRIMARY KEY NOT NULL, `physicsTarget` INTEGER NOT NULL, `chemistryTarget` INTEGER NOT NULL, `biologyTarget` INTEGER NOT NULL, `physicsSolved` INTEGER NOT NULL, `chemistrySolved` INTEGER NOT NULL, `biologySolved` INTEGER NOT NULL)")
        AppDatabase.MIGRATION_26_27.migrate(db)
        val cursor = db.query("SELECT physicsDifficulty, chemistryDifficulty, biologyDifficulty FROM daily_practice")
        assertNotNull(cursor)
        cursor.close()
    }

    @Test
    fun testMigration27To28() {
        val db = openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS `ai_saved_tests` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL)")
        AppDatabase.MIGRATION_27_28.migrate(db)
        val cursor = db.query("SELECT institute, difficultyDistribution FROM ai_saved_tests")
        assertNotNull(cursor)
        cursor.close()
    }

    @Test
    fun testMigration28To29() {
        val db = openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS `scheduled_mock_tests` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `scheduledDate` TEXT NOT NULL)")
        AppDatabase.MIGRATION_28_29.migrate(db)
        val cursor = db.query("SELECT isPinned FROM scheduled_mock_tests")
        assertNotNull(cursor)
        cursor.close()
    }

    @Test
    fun testMigration29To30() {
        val db = openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS `mistake_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL, `question` TEXT NOT NULL, `mistakeType` TEXT NOT NULL, `chapter` TEXT NOT NULL DEFAULT '', `timestamp` INTEGER NOT NULL)")
        AppDatabase.MIGRATION_29_30.migrate(db)
        val cursor = db.query("SELECT imageUri FROM mistake_logs")
        assertNotNull(cursor)
        cursor.close()
    }
}
