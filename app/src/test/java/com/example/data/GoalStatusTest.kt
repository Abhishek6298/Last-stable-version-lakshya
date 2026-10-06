package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalStatusTest {

    @Test
    fun testGoalStatusValues() {
        assertEquals("todo", GoalStatus.TODO.value)
        assertEquals("in_progress", GoalStatus.IN_PROGRESS.value)
        assertEquals("completed", GoalStatus.COMPLETED.value)
    }

    @Test
    fun testFromValueValid() {
        assertEquals(GoalStatus.TODO, GoalStatus.fromValue("todo"))
        assertEquals(GoalStatus.IN_PROGRESS, GoalStatus.fromValue("in_progress"))
        assertEquals(GoalStatus.COMPLETED, GoalStatus.fromValue("completed"))
    }

    @Test
    fun testFromValueIgnoreCase() {
        assertEquals(GoalStatus.TODO, GoalStatus.fromValue("TODO"))
        assertEquals(GoalStatus.IN_PROGRESS, GoalStatus.fromValue("In_Progress"))
        assertEquals(GoalStatus.COMPLETED, GoalStatus.fromValue("Completed"))
    }

    @Test
    fun testFromValueInvalidFallback() {
        assertEquals(GoalStatus.TODO, GoalStatus.fromValue("invalid_status"))
        assertEquals(GoalStatus.TODO, GoalStatus.fromValue(""))
    }
}
