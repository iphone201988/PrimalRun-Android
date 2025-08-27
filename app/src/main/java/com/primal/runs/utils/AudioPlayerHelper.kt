package com.primal.runs.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.primal.runs.R
import com.primal.runs.ui.running_track.FreeRunNewActivity.Companion.volumeScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.RoundingMode
import kotlin.math.roundToInt

class AudioPlayerHelper(private val context: Context) {
    var mediaPlayer: MediaPlayer? = null
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var audioFocusRequest: AudioFocusRequest? = null

    private val handler = Handler(Looper.getMainLooper())
    private var volumeRunnable: Runnable? = null

    val soundObserver: MutableLiveData<Float> = MutableLiveData()


    //@RequiresApi(Build.VERSION_CODES.O)
    fun playAudio(source: String?) {

        if (mediaPlayer == null) {

            mediaPlayer = MediaPlayer().apply {

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME) // Keeps focus on internal audio
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                try {
                    if (source != null) {
                        val assetFileDescriptor = context.resources.openRawResourceFd(R.raw.wolf_chase_18db)
                        setDataSource(
                            assetFileDescriptor.fileDescriptor,
                            assetFileDescriptor.startOffset,
                            assetFileDescriptor.length
                        )
                        assetFileDescriptor.close()
                        //setDataSource(source)
                    } else {
                        //showToast("background Sound is null")
                        return
                    }
                    setOnPreparedListener {
                        //start()
                    } // Start playback when ready
                    setOnCompletionListener {
                        if (!isPlaying) {
                            //start() // Restart playback when the track completes
                        } else {
                            Log.d("MediaPlayer", "MediaPlayer is already playing, skipping start")
                        }
                    }
                    setVolume(0.05f, 0.05f)
                    prepareAsync() // Prepare the media player asynchronously
                    isLooping = true
                    soundObserver.postValue(0.05f)
                    //playbackParams.audioFallbackMode
                } catch (e: Exception) {
                    Log.e("MediaPlayer", "Error initializing media player", e)
                }
            }
        } else {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
            } else {
                Log.d("MediaPlayer", "MediaPlayer is already playing, skipping start")
            }
        }
    }

    //@RequiresApi(Build.VERSION_CODES.O)
    fun pauseAudio() {
        mediaPlayer?.apply {
            pause()
            /*stop()
            release()*/
        }

    }

    fun resumeAudio() {
        mediaPlayer?.apply {
            start()
        }
    }

    fun stopAudio() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            releaseAudioFocus()
        } // Ensure focus is reset when stopping
    }


    fun adjustInternalVolume(internalVolume: Float) {
        Log.d("internalVolume", "adjustInternalVolume: $internalVolume")

        mediaPlayer?.setVolume(internalVolume, internalVolume) // Only changes internal app volume

        // Control external audio dynamically

        /*if (internalVolume > 0.7f) {
            lowerExternalAudio()

        } else {
            restoreExternalAudio()
        }*/
    }

    fun adjustInternalVolumeByDis(internalVolume: Float) {
        disVolumeJob?.cancel() // Cancel any previous job

        disVolumeJob = CoroutineScope(Dispatchers.Main).launch {
            Log.d("internalVolume", "adjustInternalVolume: $internalVolume")
            mediaPlayer?.setVolume(internalVolume, internalVolume)
            if (internalVolume > 0.5f) {
                lowerExternalAudio()
            } else {
                restoreExternalAudio()
            }
            delay(500)

        }

    }

    private var volumeJob: Job? = null
    private var disVolumeJob: Job? = null

    fun setLowVolume(startVolume: Float, endVolume: Float, duration: Long = 8000L) {
        volumeJob?.cancel() // Cancel any previous job

        volumeJob = CoroutineScope(Dispatchers.Main).launch {
            val steps = 50
            val delayPerStep = duration / steps
            val volumeStep = (endVolume - startVolume) / steps
            var currentVolume = startVolume

            repeat(steps) {
                currentVolume += volumeStep
                val clampedVolume = currentVolume.coerceIn(0.0f, 1.0f)
                adjustInternalVolume(clampedVolume)
                soundObserver.postValue(clampedVolume)
                Log.d("VolumeSmooth", "Volume: $clampedVolume")
                delay(delayPerStep)
            }

            // Final value to ensure it ends exactly at the target
            adjustInternalVolume(endVolume)
            soundObserver.postValue(endVolume)
            volumeScale = endVolume

        }
    }


    /*fun setLowVolume(mediaVolume: Float, targetVolume: Float) {

        volumeScale = mediaVolume
        cancelVolumeAdjustment()

        volumeRunnable = object : Runnable {
            override fun run() {
                if (volumeScale < targetVolume) {
                    volumeScale += 0.05f
                    Log.d("volume", "setLowVolume: $volumeScale")
                    adjustInternalVolume(volumeScale)
                    soundObserver.postValue(volumeScale)
                    handler.postDelayed(this, 300L)
                }else{
                    Log.d("volumeScale", "volumeScale >> $volumeScale")
                }
            }
        }
        handler.post(volumeRunnable!!)

    }*/

    fun setMediaPlayer50() {
        mediaPlayer?.setVolume(0.5f, 0.5f)
        soundObserver.postValue(volumeScale)
    }

    fun setMediaPlayerFull() {
        mediaPlayer?.setVolume(1f, 1f)
        volumeScale = 1f
        soundObserver.postValue(volumeScale)
    }

    fun setMediaVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
        soundObserver.postValue(volumeScale)
    }


    fun setMediumVolume(mediaVolume: Float, targetVolume: Float) {

        volumeScale = mediaVolume
        cancelVolumeAdjustment()

        volumeRunnable = object : Runnable {
            override fun run() {
                Log.d(
                    "volume",
                    "setMediumVolume: $volumeScale roundOff ${volumeScale.roundToInt()},  ${targetVolume.roundToInt()} "
                )
                if ((volumeScale.toBigDecimal()
                        .setScale(2, RoundingMode.HALF_UP)).toFloat() == targetVolume
                ) {
                    handler.removeCallbacks(volumeRunnable!!)
                } else if (volumeScale < targetVolume) {
                    volumeScale += 0.05f

                    adjustInternalVolume(volumeScale)
                    handler.postDelayed(this, 200L)
                } else if (volumeScale > targetVolume) {
                    volumeScale -= 0.05f

                    adjustInternalVolume(volumeScale)
                    handler.postDelayed(this, 200L)
                }
                soundObserver.postValue(volumeScale)

            }
        }
        handler.post(volumeRunnable!!)
    }

    /** Case 3: High Button (Your App: Transition to 10%, Other Apps: Transition to 100%)*/
    /*fun setHighVolume(mediaVolume: Float, targetVolume: Float) {
        Log.d("setHighVolume", "mediaVolume $mediaVolume targetVolume $targetVolume")
        volumeScale = mediaVolume
        cancelVolumeAdjustment()

        val steps = 20
        val interval = 5000L / steps // Match 5000ms animation duration
        val stepSize = (mediaVolume - targetVolume) / steps

        volumeRunnable = object : Runnable {
            override fun run() {
                if (volumeScale > targetVolume + 0.01f) { // Add buffer to avoid overshooting
                    volumeScale -= stepSize
                    volumeScale = volumeScale.coerceIn(0.0f, 1.0f)
                    adjustInternalVolume(volumeScale)
                    soundObserver.postValue(volumeScale)
                    handler.postDelayed(this, interval)
                    Log.d("volume", "setHighVolume: $volumeScale")
                } else {
                    volumeScale = targetVolume
                    adjustInternalVolume(volumeScale)
                    soundObserver.postValue(volumeScale)
                    Log.d("volume", "setHighVolume: done $volumeScale")
                }
            }
        }

        handler.post(volumeRunnable!!)
    }*/

    /** Case 3: High Button (Your App: Transition to 10%, Other Apps: Transition to 100%)*/
    fun setHighVolume(mediaVolume: Float, targetVolume: Float) {
        Log.d("setHighVolume", "setHighVolume: mediaVolume $mediaVolume targetVolume $targetVolume")
        volumeScale = mediaVolume
        cancelVolumeAdjustment()

        volumeRunnable = object : Runnable {
            override fun run() {
                if (volumeScale > targetVolume) {
                    volumeScale -= 0.05f
                    Log.d("volume", "setHighVolume: $volumeScale")
                    adjustInternalVolume(volumeScale)
                    soundObserver.postValue(volumeScale)
                    handler.postDelayed(this, 300L) // Continue adjusting
                } else {
                    Log.d("volume", "setHighVolume: else $volumeScale")
                }
            }
        }
        handler.post(volumeRunnable!!)
    }

    fun cancelVolumeAdjustment() {
        volumeRunnable?.let {
            handler.removeCallbacks(it)
        }
    }




    //@RequiresApi(Build.VERSION_CODES.O)
    fun lowerExternalAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {

                audioFocusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setOnAudioFocusChangeListener { focusChange ->
                            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                                // External audio should lower volume
                            }
                        }
                        .build()
            }
            audioManager.requestAudioFocus(audioFocusRequest!!)
        }
    }

    //@RequiresApi(Build.VERSION_CODES.O)
    fun restoreExternalAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            releaseAudioFocus()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun releaseAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            audioFocusRequest = null
        }
    }

    @SuppressLint("ServiceCast")
    fun pauseExternalMedia(context: Context) {
        val mediaSessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        val controllers = mediaSessionManager.getActiveSessions(
            ComponentName(
                context,
                MyNotificationListenerService::class.java
            )
        )

        for (controller in controllers) {
            try {
                controller.transportControls.pause()
            } catch (e: Exception) {
                Log.e("MediaControl", "Failed to pause: ${controller.packageName}", e)
            }
        }
    }

    @SuppressLint("ServiceCast")
    fun resumeExternalMedia(context: Context) {
        val mediaSessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        val controllers = mediaSessionManager.getActiveSessions(
            ComponentName(
                context,
                MyNotificationListenerService::class.java
            )
        )

        for (controller in controllers) {
            try {
                controller.transportControls.play()
            } catch (e: Exception) {
                Log.e("MediaControl", "Failed to play: ${controller.packageName}", e)
            }
        }
    }


}