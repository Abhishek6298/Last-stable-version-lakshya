package com.example.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AmbientStudySoundManagerTest {

    @Before
    fun setUp() {
        // Reset state before each test
        AmbientStudySoundManager.startSound("Rain", 0.5f)
        AmbientStudySoundManager.stopSound()
    }

    @After
    fun tearDown() {
        AmbientStudySoundManager.stopSound()
    }

    @Test
    fun testDefaultState() {
        assertEquals("Rain", AmbientStudySoundManager.getCurrentSound())
        assertEquals(0.5f, AmbientStudySoundManager.getVolume(), 0.001f)
        assertFalse(AmbientStudySoundManager.isSoundPlaying())
    }

    @Test
    fun testSetVolumeWithinBounds() {
        AmbientStudySoundManager.setVolume(0.8f)
        assertEquals(0.8f, AmbientStudySoundManager.getVolume(), 0.001f)

        AmbientStudySoundManager.setVolume(0.1f)
        assertEquals(0.1f, AmbientStudySoundManager.getVolume(), 0.001f)

        AmbientStudySoundManager.setVolume(0.0f)
        assertEquals(0.0f, AmbientStudySoundManager.getVolume(), 0.001f)

        AmbientStudySoundManager.setVolume(1.0f)
        assertEquals(1.0f, AmbientStudySoundManager.getVolume(), 0.001f)
    }

    @Test
    fun testSetVolumeClampingLowerBound() {
        AmbientStudySoundManager.setVolume(-0.5f)
        assertEquals(0.0f, AmbientStudySoundManager.getVolume(), 0.001f)
    }

    @Test
    fun testSetVolumeClampingUpperBound() {
        AmbientStudySoundManager.setVolume(1.5f)
        assertEquals(1.0f, AmbientStudySoundManager.getVolume(), 0.001f)
    }

    @Test
    fun testStartSoundUpdatesCurrentSoundAndVolume() {
        AmbientStudySoundManager.startSound("Brown Noise", 0.7f)
        assertEquals("Brown Noise", AmbientStudySoundManager.getCurrentSound())
        assertEquals(0.7f, AmbientStudySoundManager.getVolume(), 0.001f)

        // Clean up
        AmbientStudySoundManager.stopSound()
    }

    @Test
    fun testStartSoundClampsVolumeLowerBound() {
        AmbientStudySoundManager.startSound("Ocean Waves", -0.2f)
        assertEquals("Ocean Waves", AmbientStudySoundManager.getCurrentSound())
        assertEquals(0.0f, AmbientStudySoundManager.getVolume(), 0.001f)

        AmbientStudySoundManager.stopSound()
    }

    @Test
    fun testStartSoundClampsVolumeUpperBound() {
        AmbientStudySoundManager.startSound("Zen Alpha (432Hz)", 2.0f)
        assertEquals("Zen Alpha (432Hz)", AmbientStudySoundManager.getCurrentSound())
        assertEquals(1.0f, AmbientStudySoundManager.getVolume(), 0.001f)

        AmbientStudySoundManager.stopSound()
    }

    @Test
    fun testStartSoundWithDifferentPresetNames() {
        val soundPresets = listOf("Rain", "Brown Noise", "Ocean Waves", "Zen Alpha (432Hz)", "Custom White Noise")

        for (preset in soundPresets) {
            AmbientStudySoundManager.startSound(preset, 0.6f)
            assertEquals(preset, AmbientStudySoundManager.getCurrentSound())
            assertEquals(0.6f, AmbientStudySoundManager.getVolume(), 0.001f)
            AmbientStudySoundManager.stopSound()
        }
    }

    @Test
    fun testStopSoundResetsPlayingState() {
        AmbientStudySoundManager.startSound("Rain", 0.5f)
        AmbientStudySoundManager.stopSound()

        assertFalse(AmbientStudySoundManager.isSoundPlaying())
    }
}
