package com.primal.runs

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.primal.runs.ui.base.AppLifecycleListener
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
    class App : Application() {
    private var isRestarting: Boolean = false

    fun onLogout() {
        restartApp()
    }

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener(this@App))
    }

    private fun restartApp() {
        if (!isRestarting) {
            isRestarting = true
            val intent =
                baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
    /* companion object {
         private var instance: App? = null
         fun getInstance(): App? {
             return instance
         }
         fun getToken(): String? {
             return SharedPrefManager.getString(SharedPrefManager.KEY.Token, null)

         }*/


}
