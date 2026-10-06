package com.example

import com.example.data.StudyPortalGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyPortalGuardTest {

    @Test
    fun testBlockedDomains() {
        val youtubeResult = StudyPortalGuard.checkUrl("https://www.youtube.com/watch?v=12345")
        assertTrue(youtubeResult.isBlocked)

        val instagramResult = StudyPortalGuard.checkUrl("https://instagram.com/p/12345")
        assertTrue(instagramResult.isBlocked)

        val redditResult = StudyPortalGuard.checkUrl("https://reddit.com/r/test")
        assertTrue(redditResult.isBlocked)
    }

    @Test
    fun testAllowedDomains() {
        val testbookResult = StudyPortalGuard.checkUrl("https://testbook.com/test-series")
        assertFalse(testbookResult.isBlocked)

        val pwResult = StudyPortalGuard.checkUrl("https://pwthor.live")
        assertFalse(pwResult.isBlocked)

        val ntaResult = StudyPortalGuard.checkUrl("https://neet.nta.nic.in")
        assertFalse(ntaResult.isBlocked)
    }

    @Test
    fun testIsAllowedSchemeAndUrl() {
        assertTrue(StudyPortalGuard.isAllowedSchemeAndUrl("https://testbook.com/test-series"))
        assertTrue(StudyPortalGuard.isAllowedSchemeAndUrl("http://neet.nta.nic.in"))

        // Non-http/https schemes should be rejected
        assertFalse(StudyPortalGuard.isAllowedSchemeAndUrl("file:///android_asset/malicious.html"))
        assertFalse(StudyPortalGuard.isAllowedSchemeAndUrl("content://media/external/file/123"))
        assertFalse(StudyPortalGuard.isAllowedSchemeAndUrl("javascript:alert(1)"))

        // Blocked domains under http/https should also be rejected
        assertFalse(StudyPortalGuard.isAllowedSchemeAndUrl("https://youtube.com"))
        assertFalse(StudyPortalGuard.isAllowedSchemeAndUrl("https://facebook.com"))
    }
}
