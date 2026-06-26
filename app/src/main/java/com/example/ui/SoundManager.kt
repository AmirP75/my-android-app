package com.example.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundManager {
    fun playSound(type: SoundType) {
        CoroutineScope(Dispatchers.IO).launch {
            when (type) {
                SoundType.COMPLETE_QUEST -> playFrequencies(listOf(880.0 to 100, 1108.0 to 200)) // A5, C#6
                SoundType.COIN -> playFrequencies(listOf(1567.98 to 100, 2093.0 to 200)) // G6, C7
                SoundType.LEVEL_UP -> playFrequencies(listOf(440.0 to 100, 554.37 to 100, 659.25 to 100, 880.0 to 400)) // A4, C#5, E5, A5
                SoundType.SPIN_WHEEL_TICK -> playFrequencies(listOf(800.0 to 20))
            }
        }
    }

    private fun playFrequencies(notes: List<Pair<Double, Int>>) {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val audioTrack = android.media.AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(if (bufferSize > 0) bufferSize * 2 else 4096)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack.play()

        for ((freq, durationMs) in notes) {
            val numSamples = Math.round(durationMs * sampleRate / 1000.0).toInt()
            val sampleArray = ShortArray(numSamples)
            var angle = 0.0
            val angleIncrement = 2.0 * Math.PI * freq / sampleRate
            for (i in 0 until numSamples) {
                // simple envelope to avoid clicks
                val envelope = when {
                    i < 200 -> i / 200.0
                    i > numSamples - 200 -> (numSamples - i) / 200.0
                    else -> 1.0
                }
                sampleArray[i] = (sin(angle) * Short.MAX_VALUE * 0.15 * envelope).toInt().toShort()
                angle += angleIncrement
            }
            audioTrack.write(sampleArray, 0, numSamples)
            Thread.sleep(5)
        }
        audioTrack.stop()
        audioTrack.release()
    }
}

enum class SoundType { COMPLETE_QUEST, COIN, LEVEL_UP, SPIN_WHEEL_TICK }
