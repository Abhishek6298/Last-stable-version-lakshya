package com.example.data

enum class GoalStatus(val value: String) {
    TODO("todo"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed");

    companion object {
        fun fromValue(value: String): GoalStatus {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: TODO
        }
    }
}
