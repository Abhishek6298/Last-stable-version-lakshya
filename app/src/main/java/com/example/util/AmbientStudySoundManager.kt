package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.util.Random
import kotlin.math.sin

/**
 * Native, zero-dependency audio synthesizer for calming student study ambiances:
 * - Gentle Rain (filtered noise drops)
 * - Brown Noise (deep focus warmth)
 * - Zen Alpha Waves / Singing Bowl (432Hz harmonic tone)
 * - Ocean Waves (swell modulated noise)
 */
object AmbientStudySoundManager {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var currentSound = "Rain"
    private var volume = 0.5f
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun getCurrentSound(): String = currentSound
    fun isSoundPlaying(): Boolean = isPlaying
    fun getVolume(): Float = volume

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startSound(soundName: String, vol: Float = volume) {
        stopSound()
        currentSound = soundName
        volume = vol.coerceIn(0f, 1f)
        isPlaying = true

        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 2)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(volume)
            track.play()
            audioTrack = track

            synthJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                val random = Random()
                var phase1 = 0.0
                var phase2 = 0.0
                var lastBrown = 0.0
                var swellPhase = 0.0

                while (isActive && isPlaying) {
                    for (i in buffer.indices) {
                        val sample: Double = when (currentSound) {
                            "Rain" -> {
                                // Filtered white/pink noise with subtle raindrop transients
                                val white = random.nextDouble() * 2.0 - 1.0
                                lastBrown = (lastBrown + (0.05 * white)) / 1.05
                                val drop = if (random.nextInt(300) == 0) (random.nextDouble() * 0.4) else 0.0
                                (lastBrown * 0.7 + drop) * 16000.0
                            }
                            "Brown Noise" -> {
                                // Deep brownian motion noise for focus
                                val white = random.nextDouble() * 2.0 - 1.0
                                lastBrown = (lastBrown + (0.02 * white)) / 1.02
                                lastBrown * 22000.0
                            }
                            "Ocean Waves" -> {
                                // Modulated noise swells
                                swellPhase += 0.00008
                                val swell = (sin(swellPhase * 2 * Math.PI) + 1.0) * 0.5
                                val white = random.nextDouble() * 2.0 - 1.0
                                lastBrown = (lastBrown + (0.04 * white)) / 1.04
                                (lastBrown * (0.3 + 0.7 * swell)) * 20000.0
                            }
                            "Zen Alpha (432Hz)" -> {
                                // 432 Hz + 440 Hz binaural beat producing soothing 8Hz alpha rhythm
                                phase1 += (432.0 / sampleRate) * 2 * Math.PI
                                phase2 += (440.0 / sampleRate) * 2 * Math.PI
                                val s1 = sin(phase1) * 0.5
                                val s2 = sin(phase2) * 0.5
                                (s1 + s2) * 12000.0
                            }
                            else -> {
                                val white = random.nextDouble() * 2.0 - 1.0
                                lastBrown = (lastBrown + (0.03 * white)) / 1.03
                                lastBrown * 18000.0
                            }
                        }
                        buffer[i] = sample.coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
                    }
                    try {
                        track.write(buffer, 0, buffer.size)
                    } catch (e: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    fun stopSound() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }
}
