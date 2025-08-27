package com.primal.runs.ui.dashboard.settings

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class VoiceOverAudioManager(private val context: Context) {

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    // Request audio focus
    fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use AudioFocusRequest for API 26+
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()

            val result = audioManager.requestAudioFocus(focusRequest!!)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            // For older APIs
            val result = audioManager.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    // Abandon audio focus
    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    // Handle audio focus changes
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("VoiceOver", "Audio focus gained - Resume music playback")
                // Resume playback
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("VoiceOver", "Audio focus lost - Stop playback")
                // Stop playback
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d("VoiceOver", "Temporary loss of focus - Pause playback")
                // Pause playback temporarily
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d("VoiceOver", "Temporary loss with ducking - Lower volume")
                // Lower playback volume
            }
        }
    }
}
