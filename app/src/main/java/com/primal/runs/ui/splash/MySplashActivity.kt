package com.primal.runs.ui.splash

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.media.AudioManagerCompat.requestAudioFocus
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.databinding.ActivityMySplashBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.location.LocationHandler
import com.primal.runs.ui.base.location.LocationResultListener
import com.primal.runs.ui.base.permission.PermissionHandler
import com.primal.runs.ui.base.permission.Permissions
import com.primal.runs.ui.dashboard.DashBoardActivity
import com.primal.runs.ui.onboarding.OnBoardingActivity
import com.primal.runs.utils.AudioPlayerHelper
import com.primal.runs.utils.ImageUtils
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class MySplashActivity : BaseActivity<ActivityMySplashBinding>(), LocationResultListener {
    private val viewmodel: MySplashViewModel by viewModels()
    private var locationHandler: LocationHandler? = null
    private var mCurrentLocation: Location? = null
    private lateinit var audioPlayerHelper: AudioPlayerHelper
    override fun getLayoutResource(): Int {
        return R.layout.activity_my_splash
    }

    override fun getViewModel(): BaseViewModel {
        return viewmodel
    }

    override fun onCreateView() {

        audioPlayerHelper = AudioPlayerHelper(this)
        initView()
        initOnClick()

    }

    private fun initView() {
        ImageUtils.statusBarStyleBlack(this)
        ImageUtils.styleSystemBars(this, getColor(R.color.colorAccent))
    }

    private fun initOnClick() {
        if (!sharedPrefManager.getIsFirst()!!) {
            launchAccess.launch(Intent(this, AccessInfoActivity::class.java))
        } else {
            checkLocation()
        }
    }

    private fun checkLocation() {
        val rationale = "We need your location to enhance your experience."
        val options = Permissions.Options()
        Permissions.check(
            this,
            ImageUtils.permissionsForLocationOny,
            rationale,
            options,
            object : PermissionHandler() {
                override fun onGranted() {
                    sharedPrefManager.saveIsFirst(true)
                    //createLocationHandler()
                    notificationPermission()
                }

                override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                    super.onDenied(context, deniedPermissions)
                    finishAffinity()
                    //openActivity()

                }
            })
    }

    private fun notificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notiPermissionResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                createLocationHandler()
            }
        } else {
            createLocationHandler()
        }
    }

    private val notiPermissionResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissions ->

            createLocationHandler()
        }

    private fun createLocationHandler() {
        locationHandler = LocationHandler(this, this)
        locationHandler?.getUserLocation()
        locationHandler?.removeLocationUpdates()
    }

    private fun openActivity() {
        Glide.with(this).asGif().load(R.drawable.man_run_loading).into(binding.ivMan)
        Glide.with(this).asGif().load(R.drawable.bear_run_loading).into(binding.ivElephant)
        binding.llProgress.visibility = View.VISIBLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           // audioPlayerHelper.lowerExternalAudio()
        }

        ObjectAnimator.ofInt(binding.seekbar, "progress", 0, 100).apply {
            duration = 4000 // 3 seconds
            interpolator = android.view.animation.LinearInterpolator()

            doOnEnd {
                if (sharedPrefManager.getCurrentUser() != null) {
                    //sharedPrefManager.saveUserIMage(sharedPrefManager.getCurrentUser()?.profileImage)
                    startActivity(Intent(this@MySplashActivity, DashBoardActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this@MySplashActivity, OnBoardingActivity::class.java))
                    finish()
                }
            }
            start()
        }
    }

    private fun promptUserToEnableLocation(locationRequest: LocationRequest) {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        LocationServices
            .getSettingsClient(this)
            .checkLocationSettings(builder.build())
            .addOnSuccessListener {
                // getLastKnownLocation()
            }
            .addOnFailureListener { e ->
                when ((e as ResolvableApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        //e.startResolutionForResult(this, 10)
                        val intentSenderRequest = IntentSenderRequest.Builder(e.resolution).build()
                        resolutionForResult.launch(intentSenderRequest)
                    } catch (exception: IntentSender.SendIntentException) {
                        exception.printStackTrace()
                    }
                }
            }
    }

    override fun getLocation(location: Location) {
        this.mCurrentLocation = location
        Constants.latitude = location.latitude.toString()
        Constants.longitude = location.longitude.toString()
        openActivity()
    }

    override fun enableLocation(locationRequest: LocationRequest) {
        promptUserToEnableLocation(locationRequest)
    }

    private val launchAccess =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                checkLocation()
            }
        }
    private val resolutionForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Location enabled
                //getLastKnownLocation()

                locationHandler?.getLastKnownLocation()

            } else {
                // User denied
                showToast("Location permission denied")
                finishAffinity()
            }
        }


}