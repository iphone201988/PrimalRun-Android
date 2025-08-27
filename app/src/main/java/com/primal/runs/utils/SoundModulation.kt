package com.primal.runs.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat.registerReceiver
import com.primal.runs.ui.running_track.FreeRunNewActivity.Companion.volumeScale
import java.math.RoundingMode
import kotlin.math.roundToInt

class SoundModulation(context: Context) {

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val handler = Handler(Looper.getMainLooper())
    private var volumeRunnable: Runnable? = null


    val audioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->

                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        // Another app started playing, pause your music
                        Log.d("AudioFocus", "Lost focus to another app, pausing music")
                        val intent = Intent("com.vibeMatch.CALL_TIMER_UPDATED")
                        context.sendBroadcast(intent)
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // Regained focus, resume playing
                        Log.d("AudioFocus", "Gained focus, resuming music")
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // Temporary loss (e.g., notification sound), pause music
                        Log.d("AudioFocus", "Temporary focus loss, pausing")
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // Lower volume instead of stopping (e.g., notification sound)
                        Log.d("AudioFocus", "Focus lost transiently, lowering volume")
                    }
                }
            }
            .build()

    fun decreaseOtherAppsVolumeAndIncreaseMyApp() {
        // Request audio focus with ducking (reduces other apps' volume)
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

        }
    }

    /**
     * Restore other apps' volume and decrease my app's volume
     */
    fun resetVolumes() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    fun setLowVolume(mediaVolume: Float, targetVolume: Float, mediaPlayer: MediaPlayer?) {

        volumeScale = mediaVolume
        cancelVolumeAdjustment()

        volumeRunnable = object : Runnable {
            override fun run() {
                if (volumeScale < targetVolume) {
                    volumeScale += 0.05f
                    Log.d("volume", "volume: $volumeScale")
                    mediaPlayer?.setVolume(volumeScale, volumeScale)
                    handler.postDelayed(this, 300L)
                }
            }
        }
        handler.post(volumeRunnable!!)

        /*val result = audioManager.requestAudioFocus(audioFocusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            volumeScale = mediaVolume
            cancelVolumeAdjustment()

            volumeRunnable = object : Runnable {
                override fun run() {
                    if (volumeScale < targetVolume) {
                        volumeScale += 0.05f
                        Log.d("volume", "volume: $volumeScale")
                        mediaPlayer?.setVolume(volumeScale, volumeScale)
                        handler.postDelayed(this, 300L)
                    }
                }
            }
            handler.post(volumeRunnable!!)
        }*/
    }

    // Case 2: Medium Button (Your App & Other Apps: Transition to 50%)
    fun setMediumVolume(mediaVolume: Float, targetVolume: Float, mediaPlayer: MediaPlayer?) {
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            volumeScale = mediaVolume
            cancelVolumeAdjustment()

            volumeRunnable = object : Runnable {
                override fun run() {
                    Log.d(
                        "volume",
                        "volume: $volumeScale roundOff ${volumeScale.roundToInt()},  ${targetVolume.roundToInt()} "
                    )
                    if ((volumeScale.toBigDecimal().setScale(2, RoundingMode.HALF_UP)).toFloat() == targetVolume) {
                        handler.removeCallbacks(volumeRunnable!!)
                    } else if (volumeScale < targetVolume) {
                        volumeScale += 0.05f

                        mediaPlayer?.setVolume(volumeScale, volumeScale)
                        handler.postDelayed(this, 200L)
                    } else if (volumeScale > targetVolume) {
                        volumeScale -= 0.05f

                        mediaPlayer?.setVolume(volumeScale, volumeScale)
                        handler.postDelayed(this, 200L)
                    }

                }
            }
            handler.post(volumeRunnable!!)

            /*handler.post(object : Runnable {
                override fun run() {

                    if (volumeScale < targetVolume) {
                        volumeScale += 0.05f
                        Log.d("volume", "volume: $volumeScale")
                        mediaPlayer?.setVolume(volumeScale, volumeScale)
                        handler.postDelayed(this, 200L)
                    } else if (volumeScale > targetVolume) {
                        volumeScale -= 0.05f
                        Log.d("volume", "volume: $volumeScale")
                        mediaPlayer?.setVolume(volumeScale, volumeScale)
                        handler.postDelayed(this, 200L)
                    } else {

                    }

                }
            })*/
        }
    }

    /** Case 3: High Button (Your App: Transition to 10%, Other Apps: Transition to 100%)*/
    fun setHighVolume(mediaVolume: Float, targetVolume: Float, mediaPlayer: MediaPlayer?) {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        Log.d("volume", "volume: $mediaVolume, target $targetVolume")

        var volume = mediaVolume
        cancelVolumeAdjustment()

        volumeRunnable = object : Runnable {
            override fun run() {
                if (volume > targetVolume) {
                    volume -= 0.05f
                    Log.d("volume", "volume: $volume")
                    mediaPlayer?.setVolume(volume, volume)
                    handler.postDelayed(this, 300L) // Continue adjusting
                }
            }
        }
        handler.post(volumeRunnable!!)

        /*handler.post(object : Runnable {
            override fun run() {

                if (volume > targetVolume) {
                    volume -= 0.05f
                    Log.d("volume", "volume: $volume")
                    mediaPlayer?.setVolume(volume, volume)
                    handler.postDelayed(this, 300L) // Continue adjusting
                }

            }
        })*/
    }


    private fun smoothVolumeChange(increaseAppVolume: Boolean, mediaPlayer: MediaPlayer?) {
        val interval = 200L // 200ms delay between steps
        var myAppVolume = if (increaseAppVolume) 0.1f else 1.0f
        val targetVolume = if (increaseAppVolume) 1.0f else 0.1f

        handler.post(object : Runnable {
            override fun run() {
                if ((increaseAppVolume && myAppVolume < targetVolume) ||
                    (!increaseAppVolume && myAppVolume > targetVolume)
                ) {
                    myAppVolume = if (increaseAppVolume) {
                        (myAppVolume + 0.05f).coerceAtMost(1.0f)
                    } else {
                        (myAppVolume - 0.05f).coerceAtLeast(0.1f)
                    }
                    mediaPlayer?.setVolume(myAppVolume, myAppVolume)
                    handler.postDelayed(this, interval) // Continue adjusting
                }
            }
        })
    }

    fun cancelVolumeAdjustment() {
        volumeRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    /**Extension function to get media player volume*/
    private val MediaPlayer.volume: Float
        get() = try {
            val field = MediaPlayer::class.java.getDeclaredField("mVolume")
            field.isAccessible = true
            (field.get(this) as FloatArray)[0] // Get left channel volume
        } catch (e: Exception) {
            0.1f // Default value if inaccessible
        }


    fun smoothVolumeTransition(
        streamType: Int,
        startVolume: Float,
        endVolume: Float,
        duration: Long,
    ) {
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val steps = 50 // Number of steps for smooth transition
        val stepDuration = duration / steps
        val volumeIncrement = (endVolume - startVolume) / steps

        var currentStep = 0
        handler.post(object : Runnable {
            override fun run() {
                if (currentStep <= steps) {
                    val newVolume = startVolume + (volumeIncrement * currentStep)
                    val volumeLevel = (newVolume * maxVolume).toInt()

                    Log.d("new_Volume", "smoothVolumeTransition: $volumeLevel")
                    audioManager.setStreamVolume(streamType, volumeLevel, 0)
                    currentStep++
                    handler.postDelayed(this, stepDuration)
                }
            }
        })
    }

    fun smoothMediaPlayerVolumeTransition(
        mediaPlayer: MediaPlayer?,
        startVolume: Float,
        endVolume: Float,
        duration: Long,
    ) {
        val steps = 50 // Number of steps for smooth transition
        val stepDuration = duration / steps
        val volumeIncrement = (endVolume - startVolume) / steps


        var currentStep = 0

        volumeRunnable = object : Runnable {


            override fun run() {
                if (currentStep <= steps) {
                    val newVolume = startVolume + (volumeIncrement * currentStep)
                    Log.d("new_Volume", "run: $newVolume")
                    mediaPlayer?.setVolume(newVolume, newVolume)
                    currentStep++
                    handler.postDelayed(this, stepDuration)
                }
            }
        }
        handler.post { volumeRunnable }

    }

}

