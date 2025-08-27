package com.primal.runs.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoaded = false // Track if the ad is loaded
    private var adUnitId: String = "ca-app-pub-3940256099942544/1033173712" // Test Ad Unit ID

    interface InterstitialAdCallback {
        fun onAdLoaded()
        fun onAdFailedToLoad(error: LoadAdError)
        fun onAdClosed()
        fun onAdShowed()
        fun onAdClicked()
    }

    private var callback: InterstitialAdCallback? = null

    fun loadAd(adUnitId: String? = null, callback: InterstitialAdCallback? = null) {
        this.callback = callback
        adUnitId?.let { this.adUnitId = it }

        if (isAdLoaded) {
            Log.d("InterstitialAdManager", "Ad is already loaded.")
            return
        }

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context,
            this.adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("InterstitialAdManager", "Interstitial Ad loaded successfully")
                    interstitialAd = ad
                    isAdLoaded = true
                    callback?.onAdLoaded()
                    setupAdCallbacks()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(
                        "InterstitialAdManager", "Failed to load Interstitial Ad: ${error.message}"
                    )
                    callback?.onAdFailedToLoad(error)
                }
            })
    }

    private fun setupAdCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d("InterstitialAdManager", "Ad is showing.")
                callback?.onAdShowed()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d("InterstitialAdManager", "Ad was dismissed.")
                callback?.onAdClosed()
                isAdLoaded = false // Reset the loaded state
            }

            override fun onAdClicked() {
                Log.d("InterstitialAdManager", "Ad was clicked.")
                callback?.onAdClicked()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.e("InterstitialAdManager", "Ad failed to show: ${error.message}")
            }
        }
    }

    fun showAd() {
        val activity = when (context) {
            is android.app.Activity -> context as android.app.Activity
            is android.content.ContextWrapper -> (context as android.content.ContextWrapper).baseContext as? android.app.Activity
            else -> null
        }

        if (activity != null && interstitialAd != null) {
            interstitialAd?.show(activity)
        } else {
            Log.e("InterstitialAdManager", "Ad is not ready or context is not an activity.")
        }
    }

    fun isAdReady(): Boolean {
        return isAdLoaded
    }
}
