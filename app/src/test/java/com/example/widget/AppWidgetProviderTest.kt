package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppWidgetProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNeetCountdownWidgetInflation() {
        val views = RemoteViews(context.packageName, R.layout.widget_neet_countdown)
        val parent = FrameLayout(context)
        val inflated = views.apply(context, parent)
        assertNotNull("Widget layout must inflate successfully in RemoteViews", inflated)
    }

    @Test
    fun testInteractiveTaskDppWidgetInflation() {
        val views = RemoteViews(context.packageName, R.layout.widget_interactive_tasks)
        val parent = FrameLayout(context)
        val inflated = views.apply(context, parent)
        assertNotNull("Interactive tasks widget layout must inflate successfully in RemoteViews", inflated)
    }

    @Test
    fun testQuickNotesWidgetInflation() {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_notes)
        val parent = FrameLayout(context)
        val inflated = views.apply(context, parent)
        assertNotNull("Quick notes widget layout must inflate successfully in RemoteViews", inflated)
    }

    @Test
    fun testMistakeNotebookWidgetInflation() {
        val views = RemoteViews(context.packageName, R.layout.widget_mistake_notebook)
        val parent = FrameLayout(context)
        val inflated = views.apply(context, parent)
        assertNotNull("Mistake notebook widget layout must inflate successfully in RemoteViews", inflated)
    }

    @Test
    fun testNeetCountdownWidgetProviderUpdate() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetManager != null) {
            NeetCountdownWidgetProvider.updateAppWidget(context, appWidgetManager, 1)
        }
    }

    @Test
    fun testInteractiveTaskDppWidgetProviderUpdate() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetManager != null) {
            InteractiveTaskDppWidgetProvider.updateAppWidget(context, appWidgetManager, 2)
        }
    }

    @Test
    fun testQuickNotesWidgetProviderUpdate() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetManager != null) {
            QuickNotesWidgetProvider.updateAppWidget(context, appWidgetManager, 3)
        }
    }

    @Test
    fun testMistakeNotebookWidgetProviderUpdate() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetManager != null) {
            MistakeNotebookWidgetProvider.updateAppWidget(context, appWidgetManager, 4)
        }
    }
}
