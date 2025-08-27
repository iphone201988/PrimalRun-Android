package com.primal.runs.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

class AudioFocusReceiver(private val callback: () -> Unit) :BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            Log.d("AudioNoisyReceiver", "Another app resumed music!")
            callback()
        }

    }
}