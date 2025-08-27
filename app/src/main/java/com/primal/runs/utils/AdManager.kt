package com.primal.runs.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class AdManager(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val adView: AdView
    private var isAdLoaded = false  // Flag to track if the ad is loaded
    var adSize: AdSize? = null  // AdSize will be set once and reused

    interface AdManagerCallback {
        fun onAdLoaded()
        fun onAdFailedToLoad(error: LoadAdError)
        fun onAdClicked()
    }

    private var callback: AdManagerCallback? = null

    init {
        // Initialize AdView with required properties
        adView = AdView(context).apply {
            adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Ad Unit ID
        }
        this.addView(adView) // Add the AdView to the layout
    }

    fun loadAd(callback: AdManagerCallback? = null) {
        this.callback = callback

        // Set the ad size only if it's not already set
        if (adSize == null) {
            adSize = calculateAdSize()
            adView.setAdSize(adSize!!)

            // Set the background shape for the ad view
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f // corner radius in dp
                setColor(Color.WHITE) // background color
            }
            adView.background = shape
        }

        // Only load the ad if it has not been loaded before
        if (!isAdLoaded) {
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            // Set AdListener for handling callbacks
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("AdManager", "Ad loaded successfully")
                    isAdLoaded = true
                    callback?.onAdLoaded()
                }


                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdManager", "Ad failed to load: ${error.message}")
                    callback?.onAdFailedToLoad(error)
                }

                override fun onAdClicked() {
                    Log.d("AdManager", "Ad clicked")
                    callback?.onAdClicked()
                }
            }
        }
    }

    private fun calculateAdSize(): AdSize {
        // Calculate the appropriate ad size (Adaptive Banner)
        val displayMetrics = context.resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }
}
