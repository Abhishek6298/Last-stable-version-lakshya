package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MotivationalQuotesTest {

    @Test
    fun testQuotesListIsNotEmpty() {
        assertTrue("Quotes list should not be empty", MotivationalQuotes.quotes.isNotEmpty())
    }

    @Test
    fun testGetQuoteOfTheDayReturnsValidQuote() {
        val quote = MotivationalQuotes.getQuoteOfTheDay()
        assertTrue("Quote should be in the list of quotes", MotivationalQuotes.quotes.contains(quote))
    }

    @Test
    fun testGetQuoteOfTheDayLogic() {
        // Reproduce the logic to verify correctness
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val expectedIndex = dayOfYear % MotivationalQuotes.quotes.size
        val expectedQuote = MotivationalQuotes.quotes[expectedIndex]

        val actualQuote = MotivationalQuotes.getQuoteOfTheDay()

        assertEquals("Quote of the day should match the expected index based on current day of year", expectedQuote, actualQuote)
    }
}
