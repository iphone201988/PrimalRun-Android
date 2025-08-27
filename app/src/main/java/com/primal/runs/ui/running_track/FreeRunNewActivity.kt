package com.primal.runs.ui.running_track

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PorterDuff
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.LoadAdError
import com.google.gson.Gson
import com.primal.runs.R
import com.primal.runs.data.model.FreeRunModel
import com.primal.runs.databinding.ActivityFreeRunNewBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.permission.PermissionHandler
import com.primal.runs.ui.base.permission.Permissions
import com.primal.runs.ui.track_new.LocationService
import com.primal.runs.utils.AudioPlayerHelper
import com.primal.runs.utils.BadgeType
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.ImageUtils.convertPaceToSeconds
import com.primal.runs.utils.ImageUtils.generateInitialsBitmap
import com.primal.runs.utils.ImageUtils.measure
import com.primal.runs.utils.InterstitialAdManager
import com.primal.runs.utils.SpeedZone
import com.primal.runs.utils.showErrorToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@AndroidEntryPoint
class FreeRunNewActivity : BaseActivity<ActivityFreeRunNewBinding>(),
    LocationService.LocationCallbackListener {

    private val viewModel: RunTrackVm by viewModels()


    private var attackSoundMediaPLayer: MediaPlayer? = null
    private var startSoundMediaPLayer: MediaPlayer? = null
    private var job: Job? = null
    private var gifAnimJob: Job? = null
    private var mainJob: Job? = null
    private var automateJob: Job? = null
    private var distanceCovered: Double = 0.0
    private var totalDistanceInKm: Float = 0.0f
    private var totalDistanceInm: Double = 0.0
    private var distanceInMeters: Float = 0f
    private var randomDistanceInMeters: Float = 0f

    //private var totalDistanceInMiles: Double = 0.0
    private var currentSpeedMps: Float = 0f
    private lateinit var locationService: LocationService
    private var isServiceRunning = false

    private var locationPause: Boolean = false
    private var pauseLocation: Location? = null
    private var pauseTime: String? = null
    private var progressTimer: CountDownTimer? = null
    private var userProgress: Float = 0f
    private var animalProgress: Float = 0f
    private var elapsedProgressTime = 0.0
    private var gifMovePoints: List<Float> = ArrayList()

    private lateinit var animalProgressBadge: Map<BadgeType, Int>
    private var units = "/km"

    private var videoPlayer: ExoPlayer? = null
    private var playerPos = 0
    private var hitCount = 0
    private var manAnimator: ValueAnimator? = null
    private var elephantAnimator: ValueAnimator? = null
    private var isAttackAnimationRunning = false

    //private var currentSpeedZone: Int? = null
    private var stopService = false
    private var initialStop = false
    private var didFinishMoveAnimation = false

    private lateinit var audioPlayerHelper: AudioPlayerHelper

    private lateinit var interstitialAdManager: InterstitialAdManager

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var keyguardLock: KeyguardManager.KeyguardLock

    companion object {
        var freeRunData: FreeRunModel? = null
        var volumeScale: Float = 0.00f
        var targetVolume: Float = 0.7f
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_free_run_new
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d("freeRunData", "onCreateView: ${Gson().toJson(freeRunData)}")
        Log.d("FreeRun", "onCreate")
        //keepAwake()

        initView()
        initOnClick()
        loadInter()

        gifMovePoints.forEachIndexed { index, point ->
            //moveGifAnim(gifXEndPoint = point, duration = 1000L)
            Log.d("GifAnimation", "Moved to point $index: $point")
            // 1s between movements
        }
        // showGifMovePointsDots()

    }

    private fun showGifMovePointsDots() {
        val screenHeight = resources.displayMetrics.heightPixels
        val dotY = screenHeight / 2f  // middle of the screen vertically

        gifMovePoints.forEachIndexed { _, xPos ->
            val dot = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(10, 10).apply {
                    leftMargin = xPos.toInt() - 10  // center the dot horizontally
                    topMargin = dotY.toInt() - 10   // center the dot vertically
                }
                setBackgroundResource(R.drawable.dot_background)
            }

            binding.dotContainer.addView(dot)
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
                    startLocationService()
                    stopService = true

                    lifecycleScope.launch {
                        delay(2000)
                        Log.d("duration", "onGranted: ${freeRunData?.durationInMin}")
                        startProgressTimer((freeRunData?.durationInMin ?: 1).times(60).toDouble())
                        initialStop = true
                        delay(30000)
                        stopService = false
                    }
                }

                override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                    super.onDenied(context, deniedPermissions)
                    showErrorToast("Please Enable location")
                }
            })

    }

    private fun initOnClick() {
        viewModel.onClick.observe(this) {
            when (it?.id) {
                R.id.ivPause -> {
                    manAnimator?.pause()
                    elephantAnimator?.pause()
                    videoPlayer?.pause()

                    progressTimer.let { time ->
                        time?.cancel()
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioPlayerHelper.restoreExternalAudio()
                    }

                    val durationInMillis = (freeRunData?.durationInMin ?: 0) * 60 * 1000L
                    val totalTimeInMillis = durationInMillis - remainingTimeInMillis

                    binding.apply {
                        tvDistanceValuePause.text =
                            "${String.format(Locale.US, " %.2f", totalDistanceInKm)} ${
                                measure(
                                    sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1
                                )
                            }"
                        tvLenghtValuePause.text = pauseTime
                        tvAverageValuePause.text = calculateAveragePace(totalTimeInMillis)
                        consTopTimeVisible.visibility = View.GONE
                        consPause.visibility = View.VISIBLE
                    }
                    pauseTimer()
                    stopGifAnimation()
                    pauseMediaPlayerBackground()
                    pauseMediaPlayerAttack()
                }

                R.id.ivBack, R.id.tvGiveUpFailed -> {
                    finish()
                }

                R.id.tvGiveUp -> {
                    binding.consPause.visibility = View.GONE
                    binding.consExit.visibility = View.VISIBLE
                }

                R.id.tvExitYes -> {
                    finish()
                }

                R.id.tvExitNO -> {
                    binding.consPause.visibility = View.VISIBLE
                    binding.consExit.visibility = View.GONE
                }

                R.id.rlRestartFailed -> {
                    showToast("under working")
                }

                R.id.tvContinue -> {
                    if (interstitialAdManager.isAdReady()) {

                        interstitialAdManager.showAd()
                    } else {
                        showToast("Ad is not ready yet")
                        finish()
                    }
                }

                R.id.tvWatchReplay -> {
                    val intent = Intent(this, PreviewFreeRunActivity::class.java).apply {
                        putExtra("videoUrl", intent.getStringExtra("videoUrl"))
                        putExtra("resultId", "67caa9bea6f27fab02e866f2")
                        putExtra("lifeCount", hitCount)
                    }
                    startActivity(intent)
                    finish()
                }

                R.id.tvReviveFailed -> {
                    showToast("will implemented ads")
                }

                R.id.tvResume -> {

                    locationPause = false
                    lastLocation = pauseLocation
                    videoPlayer?.play()

                    binding.apply {

                        resumeTimer()
                        startGifAnimation()
                        startProgressTimer((freeRunData?.durationInMin ?: 1).times(60).toDouble())
                        playMediaPlayerBackground()

                        consTopTimeVisible.visibility = View.VISIBLE
                        consPause.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun initView() {

        audioPlayerHelper = AudioPlayerHelper(this)
        interstitialAdManager = InterstitialAdManager(this)
        //musicController = MusicController(this)
        /*val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        //val filter = IntentFilter("ACTION_AUDIO_BECOMING_NOISY")
        registerReceiver(audioFocusReceiver, filter)*/
        //registerAudioNoisyReceiver()

        enableFullScreenMode()
        initStartSoundMediaPlayer()
        initVideoPlayer()
        /* if (freeRunData != null) {
             currentSpeedMps = freeRunData?.speed?.toFloat() ?: 1f
         }*/
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        Log.d("gifMovePoints", "screenWidth: $freeRunData")
        // Divide the screen into 5 points
        /* gifMovePoints = listOf(
             0f,                  // Point 0% (start of the screen)
             screenWidth * 0.25f, // Point 25%
             screenWidth * 0.45f, // Point 45%
             screenWidth * 0.5f,  // Point 50% (center of the screen)
             screenWidth * 0.55f,  // Point 55% (center of the screen)
             screenWidth * 0.75f, // Point 75%
             screenWidth * 1.0f   // Point 100% (end of the screen)
         )*/

        gifMovePoints = List(201) { index ->  // 21 points for 20 divisions
            screenWidth * (index / 200f)  // 0%, 5%, 10%, ..., 100%
        }

        totalDistanceInm = (freeRunData?.distance ?: 0.0) * 1000
        volumeScale = 0.05f

        animalProgressBadge = mapOf(
            BadgeType.ELEPHANT to R.drawable.dummy_elephant,
            BadgeType.BULL to R.drawable.dummy_bull,
            BadgeType.GORILLA to R.drawable.dummy_gorilla,
            BadgeType.BEAR to R.drawable.dummy_bear,
            BadgeType.DEER to R.drawable.dummy_dear,
            BadgeType.TIGER to R.drawable.dummy_tiger,
            BadgeType.WOLF to R.drawable.dummy_wolf,
            BadgeType.RHINO to R.drawable.dummy_rahino,
            BadgeType.LION to R.drawable.dummy_lion,
            BadgeType.RAPTOR to R.drawable.dummy_raptor
        )
        units =
            if (sharedPrefManager.getCurrentUser()?.unitOfMeasure == 1) "/km" else "/miles"

        audioPlayerHelper.soundObserver.observe(this) {
            Log.d("audioPlayerHelper", "initView: $it")
        }
    }

    @OptIn(UnstableApi::class)
    private fun initVideoPlayer() {
        videoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = videoPlayer

        //val videoUrl = "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/Stage1.mp4"

        //val videoUrl = "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/45fps/Stage1.mp4"
        //val videoUrl = "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/1080/Stage1.mp4"
        val videoUrl = intent.getStringExtra("videoUrl")
        Log.d("videoUrl", "initVideoPlayer: $videoUrl")
        val uri = (videoUrl
            ?: "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/Stage1.mp4").toUri()
        /*val rawResourceId = R.raw.stage1_1080p
        val uri = RawResourceDataSource.buildRawResourceUri(rawResourceId)*/
        val mediaItem = MediaItem.fromUri(uri)
        videoPlayer!!.setMediaItem(mediaItem)
        videoPlayer!!.prepare()
        videoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
        videoPlayer!!.playWhenReady = true

        videoPlayer!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                when (playbackState) {
                    Player.STATE_READY -> {

                    }

                    Player.STATE_ENDED -> {

                    }

                    Player.STATE_BUFFERING -> {

                    }

                    Player.STATE_IDLE -> {

                    }
                }
            }

            @OptIn(UnstableApi::class)
            override fun onPlayerError(error: PlaybackException) {
                showErrorToast("Device not supported this video")
                if (error.cause is MediaCodecRenderer.DecoderInitializationException) {
                    val codecError =
                        error.cause as MediaCodecRenderer.DecoderInitializationException
                    //  showErrorToast("Device not supported 2D video")
                    Log.e("ExoPlayer", "Decoder error: ${codecError.diagnosticInfo}")
                }

            }
        })

    }

    @Suppress("DEPRECATION")
    private fun enableFullScreenMode() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    private fun tenSecondTimerInStarting() {
        val countDownTimer = object : CountDownTimer(4000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft =
                    (millisUntilFinished / 1000) + 1 // Add 1 to make it stop at 1 instead of 0
                binding.tvDownTimer.text = "THE RUN IS STARTING IN $secondsLeft"
            }

            override fun onFinish() {
                try {
                    checkLocation()
                    binding.apply {
                        clProgress.visibility = View.VISIBLE
                        //bgImage.visibility = View.VISIBLE
                        playerView.visibility = View.VISIBLE
                        consTopTimeVisible.visibility = View.VISIBLE
                        consGetReady.visibility = View.GONE
                        ivMan.visibility = View.VISIBLE
                        ivElephant.visibility = View.VISIBLE
                        startGifAnimation()
                        if (freeRunData != null) {
                            startTimer(freeRunData!!.durationInMin!!)
                        }
                        //playMediaPlayerBackground()
                        calculateDistGif()
                        updateProgress(0f, 0f)
                        audioPlayerHelper.adjustInternalVolume(0.0f)

                        val profileImage =
                            sharedPrefManager.getCurrentUser()?.profileImage.toString()

                        if (profileImage != null) {
                            val data1 = profileImage.replace(
                                "https://primalrunbucket.s3.us-east-1.amazonaws.com/",
                                ""
                            )

                            if (data1.isEmpty() || data1 == "null") {
                                val initialsBitmap =
                                    generateInitialsBitmap(sharedPrefManager.getCurrentUser()?.name.toString())
                                binding.ivUser.setImageBitmap(initialsBitmap)
                            } else {
                                Glide.with(this@FreeRunNewActivity)
                                    .load(profileImage)
                                    .placeholder(R.drawable.iv_dummy) // Placeholder while loading
                                    .into(binding.ivUser)
                            }


                        }
                        val badgeType = BadgeType.fromType(freeRunData?.budgeType ?: -1)
                        badgeType?.let {
                            val badgeDrawable = animalProgressBadge[it]
                            badgeDrawable?.let { drawable ->
                                Glide.with(this@FreeRunNewActivity)
                                    .load(drawable)
                                    .placeholder(R.drawable.iv_dummy) // Placeholder while loading
                                    .into(binding.ivAnimal)
                                //Glide.with(this).asGif().load(drawable).into(binding.ivElephant)
                            }
                        }
                        //playMediaPlayerAttack()
                        //soundModulation.audioManager.abandonAudioFocusRequest(soundModulation.audioFocusRequest)

                        /*Handler(Looper.getMainLooper()).postDelayed({
                            startSoundMediaPLayer?.release()
                            startSoundMediaPLayer = null
                            soundModulation.audioManager.abandonAudioFocusRequest(soundModulation.audioFocusRequest)
                        }, 2000)*/
                    }

                } catch (e: Exception) {
                    showToast(e.message)
                }
            }
        }
        countDownTimer.start()
    }

    private fun startGifAnimation() {
        binding.ivMan.setImageDrawable(null)
        binding.ivElephant.setImageDrawable(null)
        val badgeResourceMap = mapOf(
            BadgeType.ELEPHANT to R.drawable.iv_elephant_run,
            BadgeType.BULL to R.drawable.iv_run_bull,
            BadgeType.GORILLA to R.drawable.iv_run_gorilla,
            BadgeType.BEAR to R.drawable.iv_run_beer,
            BadgeType.DEER to R.drawable.iv_run_dear,
            BadgeType.TIGER to R.drawable.iv_run_tiger,
            BadgeType.WOLF to R.drawable.iv_run_wolf,
            BadgeType.RHINO to R.drawable.iv_run_rahino,
            BadgeType.LION to R.drawable.iv_run_lion,
            BadgeType.RAPTOR to R.drawable.iv_run_raptor
        )

        // Load the appropriate badge GIF
        val badgeType = BadgeType.fromType(freeRunData?.budgeType ?: -1)
        badgeType?.let {
            val badgeDrawable = badgeResourceMap[it]
            badgeDrawable?.let { drawable ->
                Glide.with(this).asGif().load(drawable).into(binding.ivElephant)
            }
        }

        // Load the appropriate gender GIF
        val genderGif =
            if (freeRunData?.gender == 1) R.drawable.iv_man_run else R.drawable.run_women
        Glide.with(this).asGif().load(genderGif).into(binding.ivMan)


        //Reset translations
        //binding.ivMan.translationX = 0f
        //binding.ivElephant.translationX = 0f

    }

    private var countDownTimerForTotalTime: CountDownTimer? = null
    private var remainingTimeInMillis: Long = 0L // To track remaining time
    private var timeElapsedInSeconds = 0

    private fun startTimer(totalMinutes: Int) {
        // Calculate total time in milliseconds
        Log.d("startTimer", "startTimer: totalMinutes => $totalMinutes ")
        val totalTimeInMillis = totalMinutes * 60 * 1000L
        if (remainingTimeInMillis == 0L) {
            remainingTimeInMillis = totalTimeInMillis
        }
        countDownTimerForTotalTime = object : CountDownTimer(remainingTimeInMillis, 1000) {
            @SuppressLint("SetTextI18n", "DefaultLocale")
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished // Update the remaining time
                timeElapsedInSeconds++

                /*val hours = millisUntilFinished / (1000 * 60 * 60) % 24
                val minutes = millisUntilFinished / (1000 * 60) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                binding.tvTimeValue.text = timeFormatted*/

                val hours = timeElapsedInSeconds / 3600
                val minutes = (timeElapsedInSeconds % 3600) / 60
                val seconds = timeElapsedInSeconds % 60
                pauseTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                binding.tvTimeValue.text = pauseTime

            }

            override fun onFinish() {
                //  if (totalDistanceInKm >= freeRunData?.distance!!) {
                val durationInMillis = (freeRunData?.durationInMin ?: 0) * 60 * 1000L
                val totalTimeInMillis1 = durationInMillis - remainingTimeInMillis
                binding.apply {

                    //tvDistanceValue.text = String.format(Locale.US, "%.2f", totalDistanceInKm)
                    /*tvDistanceValue.text =
                        "${String.format(Locale.US, " % .2f", totalDistanceInKm)} ${
                            measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)
                        }"

                    tvAverageValue.text = calculateAveragePace(totalTimeInMillis1)

                    consTopTimeVisible.visibility = View.GONE
                    consPause.visibility = View.GONE
                    consPauseAndCompleted.visibility = View.VISIBLE
                    remainingTimeInMillis = 0L // Reset remaining time*/

                    //////////////////
                    consTopTimeVisible.visibility = View.GONE
                    consFailed.visibility = View.VISIBLE

                    /* tvDistanceValuePauseFailed.text =
                         String.format(Locale.US, "%.2f", totalDistanceInKm)*/
                    tvDistanceValuePauseFailed.text =
                        "${String.format(Locale.US, " % .2f", totalDistanceInKm)} ${
                            measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)
                        }"
                    tvLenghtValueFailed.text = pauseTime
                    tvAverageValueFailed.text = calculateAveragePace(totalTimeInMillis1)
                    /////////////////


                    pauseTimer()
                    stopGifAnimation()
                    stopMediaPlayerBackground()
                    stopProgressTimer()
                    stopLocationService()
                    if (videoPlayer != null) {
                        videoPlayer!!.stop()
                        videoPlayer!!.release()
                    }
                    if (gifAnimJob != null && gifAnimJob!!.isActive) {
                        gifAnimJob!!.cancel()
                    }
                    if (job != null && job!!.isActive) {
                        job!!.cancel()
                    }
                }
            }
        }
        countDownTimerForTotalTime?.start()
    }

    private fun pauseTimer() {
        countDownTimerForTotalTime?.cancel() // Stop the timer
    }

    private fun addExtraTime(time: Int) {
        // Add 30 seconds (30,000 milliseconds) to the remaining time
        remainingTimeInMillis += time * 1000L
        // Cancel the current timer
        countDownTimerForTotalTime?.cancel()

        // Restart the timer with the updated time
        startTimer((remainingTimeInMillis / 1000 / 60).toInt()) // Convert back to minutes
    }

    private fun stopGifAnimation() {
        try {
            // Load the appropriate GIF for gender
            val genderGif =
                if (freeRunData?.gender == 1) R.drawable.iv_man_run else R.drawable.run_women
            Glide.with(this).asGif().load(genderGif).diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true).into(object : SimpleTarget<GifDrawable>() {
                    override fun onResourceReady(
                        resource: GifDrawable, transition: Transition<in GifDrawable>?,
                    ) {
                        resource.stop()
                        binding.ivMan.setImageDrawable(resource) // Set the first frame as static image
                    }
                })

            val badgeResourceMap = mapOf(
                BadgeType.ELEPHANT to R.drawable.iv_elephant_run,
                BadgeType.BULL to R.drawable.iv_run_bull,
                BadgeType.GORILLA to R.drawable.iv_run_gorilla,
                BadgeType.BEAR to R.drawable.iv_run_beer,
                BadgeType.DEER to R.drawable.iv_run_dear,
                BadgeType.TIGER to R.drawable.iv_run_tiger,
                BadgeType.RHINO to R.drawable.iv_run_rahino,
                BadgeType.LION to R.drawable.iv_run_lion,
                BadgeType.RAPTOR to R.drawable.iv_run_raptor,
                BadgeType.WOLF to R.drawable.iv_run_wolf
            )
            val badgeType = BadgeType.fromType(freeRunData?.budgeType ?: -1)
            badgeType?.let {
                val badgeDrawable = badgeResourceMap[it]
                badgeDrawable?.let { drawable ->
                    Glide.with(this).asGif().load(drawable).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(true).into(object : SimpleTarget<GifDrawable>() {
                            override fun onResourceReady(
                                resource: GifDrawable, transition: Transition<in GifDrawable>?,
                            ) {
                                resource.stop()
                                binding.ivElephant.setImageDrawable(resource) // Set the first frame as static image
                            }
                        })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*****background voice play**************/
    private fun runMediaPlayerBackground() {
        if (freeRunData?.backgroundSound != null) {
            audioPlayerHelper.playAudio(freeRunData?.backgroundSound!!)
        } else {
            showToast("background Sound is null")
            return
        }

        /*if (mediaPlayerForBgVoice == null) {

            mediaPlayerForBgVoice = MediaPlayer().apply {
                try {
                    if (freeRunData?.backgroundSound != null) {
                        setDataSource(freeRunData?.backgroundSound)
                    } else {
                        showToast("background Sound is null")
                        return
                    }
                    //setAudioStreamType(AudioManager.STREAM_ALARM)
                    //setAudioStreamType(AudioManager.STREAM_MUSIC)
                    // Set the data source to the MP3 URL
                    setOnPreparedListener {
                        Log.d("MediaPlayer", "ready")
                        // start()
                    } // Start playback when ready
                    setOnCompletionListener {
                        Log.d("MediaPlayer", "setOnCompletionListener")
                        if (!isPlaying) {
                            //start() // Restart playback when the track completes
                        } else {
                            Log.d("MediaPlayer", "MediaPlayer is already playing, skipping start")
                        }
                    }
                    setVolume(0.05f, 0.05f)
                    prepareAsync() // Prepare the media player asynchronously
                    //playbackParams.audioFallbackMode
                } catch (e: Exception) {
                    Log.e("MediaPlayer", "Error initializing media player", e)
                }
            }
            } else {
            if (!mediaPlayerForBgVoice!!.isPlaying) {
               // mediaPlayerForBgVoice?.start()
            } else {
                Log.d("MediaPlayer", "MediaPlayer is already playing, skipping start")
            }
        }*/
    }

    private fun pauseMediaPlayerBackground() {
        if (audioPlayerHelper.mediaPlayer != null) {
            try {
                if (audioPlayerHelper.mediaPlayer?.isPlaying == true) {
                    // audioPlayerHelper.mediaPlayer?.pause()
                    audioPlayerHelper.pauseAudio()
                }
                /*if (mediaPlayerForBgVoice?.isPlaying == true) {
                    mediaPlayerForBgVoice?.pause()
                }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playMediaPlayerBackground() {
        if (audioPlayerHelper.mediaPlayer != null) {
            try {
                if (audioPlayerHelper.mediaPlayer?.isPlaying != true) {
                    // audioPlayerHelper.mediaPlayer?.pause()
                    audioPlayerHelper.resumeAudio()

                    if (playerPos == 3) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            audioPlayerHelper.lowerExternalAudio()
                        }
                    }
                }

                /*if (mediaPlayerForBgVoice?.isPlaying != true) {
                    mediaPlayerForBgVoice?.start()
                }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopMediaPlayerBackground() {
        if (audioPlayerHelper.mediaPlayer != null) {
            try {
                if (audioPlayerHelper.mediaPlayer!!.isPlaying) {
                    audioPlayerHelper.mediaPlayer?.stop()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                audioPlayerHelper.mediaPlayer = null
                Log.d("MediaPlayer", "MediaPlayer stopped and set to null")
            }
        }

        /*if (mediaPlayerForBgVoice != null) {
            try {
                if (mediaPlayerForBgVoice!!.isPlaying) {
                    mediaPlayerForBgVoice!!.stop()
                }
                mediaPlayerForBgVoice!!.reset()
                mediaPlayerForBgVoice!!.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mediaPlayerForBgVoice = null
                Log.d("MediaPlayer", "MediaPlayer stopped and set to null")
            }
        }*/
    }

    private fun resumeTimer() {
        val minutes = (timeElapsedInSeconds / (1000 * 60)) % 60
        startTimer(minutes)

    }

    /********  Attack sound media player**********/
    private fun initAttackSoundMediaPlayer() {
        if (attackSoundMediaPLayer == null) {
            attackSoundMediaPLayer = MediaPlayer().apply {
                try {
                    if (freeRunData?.attackSound != null) {
                        setDataSource(freeRunData?.attackSound)
                    } else setAudioStreamType(AudioManager.STREAM_ALARM)
                    //setAudioStreamType(AudioManager.STREAM_MUSIC)
                    // Set the data source to the MP3 URL
                    setOnPreparedListener {
                        //  start()
                    } // Start playback when ready
                    setOnCompletionListener {
                        if (!isPlaying) {
                            //  start() // Restart playback when the track completes
                        } else {
                            Log.d("MediaPlayer", "MediaPlayer is already playing, skipping start")
                        }
                    }
                    prepareAsync() // Prepare the media player asynchronously
                } catch (e: Exception) {
                    Log.e("MediaPlayer", "Error initializing media player", e)
                }
            }

        } else {
            if (!attackSoundMediaPLayer!!.isPlaying) {
                attackSoundMediaPLayer?.start()
            } else {
                Log.d("MediaPlayer", "MediaPlayer is already playing, skipping start")
            }
        }
    }

    private fun pauseMediaPlayerAttack() {
        if (attackSoundMediaPLayer != null) {
            try {
                if (attackSoundMediaPLayer?.isPlaying == true) {
                    attackSoundMediaPLayer?.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Call this to cancel any running job if needed
    private fun cancelSoundPlayback() {
        job?.cancel()
        attackSoundMediaPLayer?.pause()
        attackSoundMediaPLayer?.seekTo(0) // Reset to the start

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioPlayerHelper.releaseAudioFocus()
        }
    }

    private fun hitGif() {
        binding.ivMan.setColorFilter(
            ContextCompat.getColor(this, android.R.color.holo_red_dark), // Red tint color
            PorterDuff.Mode.SRC_ATOP // Mode to blend the color
        )
    }

    fun animateAttackAndReturn(ivElephant: View, ivMan: View) {
        Log.d("isAttackAnimationRunning", "isAttackAnimationRunning: $isAttackAnimationRunning")
        if (isAttackAnimationRunning) return
        isAttackAnimationRunning = true
        // Initial and target positions
        val startX = ivElephant.x
        val targetX = ivMan.x - ivMan.width // Adjust to simulate "attacking"

        Log.d("animateAttackAndReturn", "animateAttackAndReturn: ${startX},  $targetX")
        // Move the elephant forward to attack
        val forwardAnimator = ObjectAnimator.ofFloat(ivElephant, "x", startX, targetX).apply {
            duration = 1000L // Duration to reach the man
        }

        // Return the elephant to its initial position
        val returnAnimator = ObjectAnimator.ofFloat(ivElephant, "x", targetX, startX).apply {
            duration = 1000L // Duration to return
        }

        // Set up listener for forward animation
        forwardAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator, isReverse: Boolean) {
                super.onAnimationStart(animation, isReverse)
                try {
                    if (attackSoundMediaPLayer?.isPlaying != true) {
                        //pauseMediaPlayerBackground()
                        attackSoundMediaPLayer?.start()
                        hitCount += 1
                    }
                } catch (ex: Exception) {
                    Log.d("ex", "onAnimationStart: ${ex.message}")
                    ex.printStackTrace()
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)

                hitGif()
                stopService = true
                stopProgressTimer()

                lifecycleScope.launch {
                    delay(10000)
                    if (binding.consPause.isGone) {
                        //stopService = false
                        progressTimer?.cancel()
                        //startProgressTimer(((freeRunData?.durationInMin ?: 1).times(60) + 10.0))
                        startProgressTimer((freeRunData?.durationInMin ?: 1).times(60).toDouble())
                    }
                    delay(4000)
                    stopService = false
                    Log.d("stopLocationService", "onAnimationStarted:-> $stopService")
                }
                /*Handler(Looper.getMainLooper()).postDelayed({
                    stopService = false
                    if(binding.consPause.isGone)  addProgressTimer(10.0)
                    Log.d("stopLocationService", "onAnimationStarted:->  ")
                }, 10000)*/
                if (binding.consPause.isGone) addExtraTime(10)
                returnAnimator.start()
            }

        })

        returnAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)

                //stopLocationService()
                // stopProgressTimer()

                Log.d("stopLocationService", "onAnimationEnd: ")

            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                Log.d("animateAttackAndReturn", "onAnimationEnd: ")
                unHitGif()
                Handler(Looper.getMainLooper()).postDelayed({
                    audioPlayerHelper.setMediaPlayerFull()
                }, 1000)
                /*attackSoundMediaPLayer?.pause()
                attackSoundMediaPLayer?.seekTo(0)*/ // Reset to the start
                if (binding.consPause.isGone) playMediaPlayerBackground()
                cancelSoundPlayback()
                isAttackAnimationRunning = false

            }
        })
        // Start the forward animation
        forwardAnimator.start()

    }

    private fun unHitGif() {
        binding.ivMan.setColorFilter(
            ContextCompat.getColor(this, android.R.color.black), // Red tint color
            PorterDuff.Mode.SRC_ATOP // Mode to blend the color
        )
        binding.apply {
            val layoutParams = ivElephant.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.marginEnd = 20
            ivElephant.layoutParams = layoutParams
        }
    }


    /********* Start sound play  ********/
    private fun initStartSoundMediaPlayer() {
        if (startSoundMediaPLayer == null) {
            startSoundMediaPLayer = MediaPlayer().apply {
                try {
                    if (freeRunData?.startSound != null) {
                        setDataSource(freeRunData?.startSound)
                    } else {
                        showToast("background Sound is null")
                        return
                    }
                    setOnPreparedListener {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            audioPlayerHelper.lowerExternalAudio()
                        }
                        start()
                        tenSecondTimerInStarting()
                        runMediaPlayerBackground()
                        initAttackSoundMediaPlayer()
                        if (freeRunData != null) {
                            binding.bean = freeRunData
                        }
                    }

                    setOnCompletionListener {
                        Log.d("setOnCompletionListener", "setOnCompletionListener -> ")

                        startSoundMediaPLayer?.release()
                        startSoundMediaPLayer = null

                        if (audioPlayerHelper.mediaPlayer != null) {
                            audioPlayerHelper.mediaPlayer!!.start()

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                audioPlayerHelper.releaseAudioFocus()
                            }
                        }

                    }
                    setOnSeekCompleteListener {
                        Log.d("setOnCompletionListener", "setOnSeekCompleteListener")
                    }
                    setVolume(1.0f, 1.0f)
                    prepareAsync() // Prepare the media player asynchronously
                } catch (e: Exception) {
                    Log.e("MediaPlayer", "Error initializing media player", e)
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        /*stopMediaPlayerBackground()
        stopGifAnimation()
        pauseMediaPlayerBackground()*/
        stopLocationService()
        stopProgressTimer()
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("FreeRun", "onDestroy")
        /* window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
         if (::wakeLock.isInitialized && wakeLock.isHeld) {
             wakeLock.release()
         }*/
        try {

            videoPlayer?.stop()
            videoPlayer?.release()
            stopMediaPlayerBackground()
            stopGifAnimation()
            pauseMediaPlayerAttack()

            startSoundMediaPLayer?.stop()
            startSoundMediaPLayer?.release()
            startSoundMediaPLayer = null
            attackSoundMediaPLayer?.stop()
            attackSoundMediaPLayer = null
            //attackSoundMediaPLayer?.release()

            pauseTimer()
            stopLocationService()
            stopProgressTimer()
            if (gifAnimJob != null && gifAnimJob!!.isActive) {
                gifAnimJob!!.cancel()
            }
            if (job != null && job!!.isActive) {
                job!!.cancel()
            }

            audioPlayerHelper.stopAudio()
            audioPlayerHelper.cancelVolumeAdjustment()
            audioPlayerHelper.restoreExternalAudio()
            freeRunData = null
            progressTimer = null

        } catch (ex: Exception) {
            Log.d("onDestroy", "onDestroy Exception: ${ex.message}")
            ex.printStackTrace()
        }

        automateJob.let {
            it?.cancel()
        }
        /*if(soundModulation.runnable !=null){
            soundModulation.handler.removeCallbacks(soundModulation.runnable!!)
        }*/
        //unregisterReceiver(audioFocusReceiver)
    }

    /*********** Location related services & handling****************/
    private val locationServiceConnection = object : ServiceConnection {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            locationService.startLocationUpdates(this@FreeRunNewActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    private fun startLocationService() {
        if (!isServiceRunning) {
            val intent = Intent(this, LocationService::class.java)
            startService(intent)
            bindService(intent, locationServiceConnection, BIND_AUTO_CREATE)
            isServiceRunning = true
            Log.d("locationServiceConnection", "startLocationService: ")
        }
    }

    private fun stopLocationService() {
        if (isServiceRunning) {
            unbindService(locationServiceConnection)
            val intent = Intent(this, LocationService::class.java)
            stopService(intent)
            isServiceRunning = false
            Log.d("locationServiceConnection", "stopLocationService:  ")
        }
    }

    private var lastLocation: Location? = null
    private val MIN_DISTANCE_CHANGE: Float = 3f

    override fun onLocationChanged(location: Location) {
        Log.i("testSpeed", "speed ->  ${location.speed}")
        if (!binding.consPause.isVisible) {
            if (lastLocation != null) {
                val distance = lastLocation!!.distanceTo(location)
                Log.i("userProgress", "distance $distance")
                val acc = location.accuracy
                val speed = location.speed // in m/s

                val isAccurate = acc <= 10f
                val isLikelyMoving = speed > 0.3f // very slow walk = ~0.5 m/s

                if (distance > MIN_DISTANCE_CHANGE) {
                    distanceInMeters += distance
                    totalDistanceInKm =
                        if ((sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1) == 1) {
                            (distanceInMeters / 1000)
                        } else {
                            ((distanceInMeters / 1000) * 0.621371).toFloat()
                        }
                } else if (distance > 0.5f && isAccurate && isLikelyMoving) {
                    // Small movement, but real
                    distanceInMeters += distance
                    totalDistanceInKm =
                        if ((sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1) == 1) {
                            (distanceInMeters / 1000)
                        } else {
                            ((distanceInMeters / 1000) * 0.621371).toFloat()
                        }
                    Log.e(
                        "LocationService",
                        "Distance Added: $distance m, Total = $distanceInMeters"
                    )
                    Log.d(
                        "Distance",
                        "Added (small real move): $distance m [speed=$speed, acc=$acc]"
                    )
                } else {
                    Log.i("Progress", "Ignored fluctuation: ${"%.2f".format(distance)}m")
                }
            }

            lastLocation = location
            Log.i("distanceInMeters", "distanceInMeters $distanceInMeters  , $distanceCovered m")
            userProgress =
                ((distanceInMeters / totalDistanceInm) * 100).coerceIn(0.0, 100.0).toFloat()
            //((totalDistanceInKm / freeRunData?.distance!!) * 100).coerceIn(0.0, 100.0).toFloat()
            //Log.i("userProgress", "speed: $currentSpeedMps userProgress $userProgress")

            binding.tvDistanceUser.text = "Dist cover by user = ${distanceInMeters}m"

            updateProgress(null, userProgress)

            val dis = String.format(Locale.US, "%.2f", totalDistanceInKm)

            binding.tvDistanceValueTop.text =
                "$dis ${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}"
            Log.i("totalDistanceInKm", "totalDistanceInKm: $totalDistanceInKm")

            if (freeRunData != null) {
                if (totalDistanceInKm >= (freeRunData?.distance ?: 0.0)) {
                    stageCompleted()
                    return
                }
            }
            //handlingDistanceBetweenGif(location)
            if (initialStop) {
                Log.i("stopService", "stopService:->  $stopService")
                handlingDistanceBetweenGif(location)
            }

        } else {

            if (locationPause) {
                locationPause = false
                pauseLocation = location
            }
            /* if (lastLocation != null) {
                 val distance = lastLocation!!.distanceTo(location)
                 randomDistanceInMeters += distance
             }
             lastLocation = location
             Log.i("randomDistanceInMeters", "randomDistanceInMeters: $randomDistanceInMeters")*/
            pauseTimer()
            //stopGifAnimation()

            pauseMediaPlayerBackground()
            pauseMediaPlayerAttack()
        }
    }

    override fun onSpeedChanged(speed: Float) {
        //currentSpeedMps = if (speed < 0.19f) 0f else speed
        currentSpeedMps = speed
        Log.i(
            "currentSpeedMps",
            "currentSpeedMps: ${speed} => $currentSpeedMps "
        )
        binding.tvSpeed.text = "GPS Speed: $speed m/s "
        binding.tvPaceValue.text = calculatePaceFromSpeed(currentSpeedMps.toDouble())
    }

    private fun handlingDistanceBetweenGif(location: Location) {
        if (freeRunData != null) {
            currentSpeedMps = location.speed
            val distanceBetween = distanceInMeters - distanceCovered
            //val newSpeedZone = updateSpeedZone(distanceBetween)


            /*targetVolume = when (distanceBetween) {
                in 91f..100f -> 0.0f
                in 81f..90f -> 0.08f
                in 71f..80f -> 0.15f
                in 61f..70f -> 0.22f
                in 51f..60f -> 0.29f
                in 41f..50f -> 0.36f
                in 31f..40f -> 0.43f
                in 21f..30f -> 0.48f
                in 11f..20f -> 0.57f
                in 0f..10f -> 0.70f
                else -> 0.0f
            }*/

            targetVolume = when {
                distanceBetween >= 100f -> 0.0f
                distanceBetween in 81f..100f -> {
                    0.0f
                }

                distanceBetween in 61f..80f -> {
                    0.175f
                }

                distanceBetween in 41f..60f -> {
                    0.35f
                }

                distanceBetween in 21f..40f -> {
                    0.525f
                }

                distanceBetween in 0f..20f -> {
                    0.7f
                }
                //distanceBetween >= 0f -> 1.0f
                else -> 0.7f
            }
            Log.d(
                "targetVolume", "targetVolume $targetVolume"
            )
            val newIndex = distanceToIndex(distanceBetween)
            Log.d(
                "targetVolumeWithDistance",
                "newIndex $newIndex , currentIndex -> $currentIndex"
            )
            if (newIndex != currentIndex) {
                //moveToIndex(newIndex, binding.ivMan) // or ivElephant, adjust as needed
                val duration1 = abs(newIndex - currentIndex) * 1000L
                val duration2 = abs(newIndex - currentIndex) * 100L

                val duration = if (duration2 < duration1) {
                    duration2
                } else {
                    duration1
                }
                Log.d(
                    "newSpeedZone",
                    "newIndex $newIndex currentIndex $currentIndex , duration $duration "
                )
                moveGifAnimByIndex(newIndex, gifMovePoints[200 - newIndex], duration = 1500L)

            } else if (newIndex == 85) {
                if (!stopService && playerPos == 3) {
                    audioPlayerHelper.setMediaPlayer50()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioPlayerHelper.lowerExternalAudio()
                    }
                    animateAttackAndReturn(binding.ivElephant, binding.ivMan)
                }
            }
        }
        calculateDistGif()
    }

    @SuppressLint("NewApi")
    private fun moveGifAnim(gifXPoints: Float = 0f, distance: Float = 0f) {
        // Get screen width
        gifAnimJob.let {
            Log.d("gifAnimJob", "gifAnimJob: cancel $playerPos")
            it?.cancel()
            /*if(playerPos == 2){
                animateAttackAndReturn(binding.ivElephant, binding.ivMan)
                return
            }*/
        }

        gifAnimJob = CoroutineScope(Dispatchers.Main).launch {
            val currentJob = coroutineContext.job
            Log.d("gifAnimJob", "gifAnimJob: called ")
            binding.apply {
                // Calculate initial positions
                val ivManStartX = ivMan.x
                val ivElephantStartX = ivElephant.x

                // Calculate the center positions for both images

                val ivManTargetX = (gifXPoints) + distance // Offset to prevent collision
                val ivElephantTargetX =
                    (gifXPoints) - ivElephant.width - distance  // Offset to prevent collision
                Log.d("volumeScale", "volumeScale: $volumeScale")
                audioPlayerHelper.setLowVolume(volumeScale, 1.0f)


                manAnimator = ValueAnimator.ofFloat(ivManStartX, ivManTargetX)
                manAnimator?.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    ivMan.x = value
                }

                // Animate the elephant's image
                elephantAnimator = ValueAnimator.ofFloat(ivElephantStartX, ivElephantTargetX)
                elephantAnimator?.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    ivElephant.x = value
                }

                // Set animation duration and start together
                manAnimator?.duration = 8000L
                elephantAnimator?.duration = 8000L

                elephantAnimator?.addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationStart(animation: Animator, isReverse: Boolean) {

                    }

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        Log.d(
                            "gifAnimJob", "reached to end"
                        )
                        didFinishMoveAnimation = true

                        launch {
                            //delay(1000)
                            if (currentJob.isActive && currentJob == gifAnimJob) {
                                audioPlayerHelper.setMediaPlayer50()
                                playerPos = 3

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    audioPlayerHelper.lowerExternalAudio()
                                }

                                animateAttackAndReturn(ivElephant, ivMan)
                            } else {
                                Log.d(
                                    "gifAnimJob",
                                    "Skipped animateAttackAndReturn due to job mismatch or cancellation"
                                )
                            }
                        }
                        /*audioPlayerHelper.setMediaPlayer50()
                        playerPos = 3
                        didFinishMoveAnimation = true
                        animateAttackAndReturn(binding.ivElephant, binding.ivMan)*/
                    }
                })
                // Start both animations
                manAnimator?.start()
                elephantAnimator?.start()
            }
        }
    }

    private fun forwardGifAnim(
        gifXStartPoint: Float = 0f,
        gifXEndPoint: Float = 0f,
        distance: Float = 0f,
    ) {
        if (gifAnimJob != null) {
            gifAnimJob!!.cancel()
            manAnimator.let { it?.cancel() }
            elephantAnimator.let { it?.cancel() }
        }
        gifAnimJob = CoroutineScope(Dispatchers.Main).launch {
            binding.apply {

                val ivManStartX = ivMan.x
                val ivElephantStartX = ivElephant.x
                didFinishMoveAnimation = false
                Log.d(
                    "moveGifAnim",
                    "moveGifAnim: width ${(gifXEndPoint)}, moving distance $distance"
                )

                val ivManTargetX =
                    (gifXEndPoint) - ivMan.width - distance // Offset to prevent collision
                val ivElephantTargetX = gifXStartPoint + distance  // Offset to prevent collision

                // Animate the man's image
                // ivMan.animate().translationX(ivManTargetX).setDuration(500).start()


                when (gifXStartPoint) {
                    gifMovePoints[1] -> {
                        // audioPlayerHelper.setMediumVolume(volumeScale, 0.5f)
                    }

                    gifMovePoints[0] -> {
                        // audioPlayerHelper.setHighVolume(volumeScale, 0.05f)
                    }
                }

                manAnimator = ValueAnimator.ofFloat(ivManStartX, ivManTargetX)
                manAnimator?.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    ivMan.x = value

                }
                // Animate the elephant's image
                elephantAnimator = ValueAnimator.ofFloat(ivElephantStartX, ivElephantTargetX)
                elephantAnimator?.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    ivElephant.x = value
                }

                elephantAnimator?.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        playerPos = if (gifXStartPoint == gifMovePoints[1]) {
                            1
                        } else {
                            0
                        }
                    }
                })

                manAnimator?.duration = 5000L
                elephantAnimator?.duration = 5000L

                manAnimator?.start()
                elephantAnimator?.start()
            }
        }
    }

    private fun moveGifAnimByIndex(index: Int, manTarget: Float, duration: Long = 5000L) {
        Log.e("index", "moveGifAnimByIndex: $index targetVolume $targetVolume")
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        manAnimator?.cancel()
        elephantAnimator?.cancel()
        gifAnimJob?.cancel()
        gifAnimJob = CoroutineScope(Dispatchers.Main).launch {
            val currentJob = coroutineContext.job

            val points = gifMovePoints

            if (index !in points.indices) return@launch

            val ivManStartX = binding.ivMan.x
            val ivElephantStartX = binding.ivElephant.x

            val targetElephantX = points[index] - (binding.ivElephant.width / 2)
            val minX = 0f
            val maxX = (screenWidth - binding.ivElephant.width).toFloat()
            val clampedTargetX = targetElephantX.coerceIn(minX, maxX)

            val targetManX = manTarget - (binding.ivMan.width / 2)
            val manMaxX = (screenWidth - binding.ivMan.width).toFloat()
            val clampedManTargetX = targetManX.coerceIn(minX, manMaxX)


            manAnimator = ValueAnimator.ofFloat(
                ivManStartX,
                clampedManTargetX /*(manTarget - (binding.ivMan.width/2))*/
            )
            manAnimator?.addUpdateListener { animation ->
                binding.ivMan.x = animation.animatedValue as Float

            }
            Log.e(
                "targetVolume",
                "targetVolume $targetVolume"
            )
            elephantAnimator = ValueAnimator.ofFloat(ivElephantStartX, clampedTargetX).apply {
                interpolator = LinearInterpolator()
                this.duration = duration

                addUpdateListener { animation ->

                    //binding.ivElephant.x = animation.animatedValue as Float

                    val currentX = animation.animatedValue as Float
                    binding.ivElephant.x = currentX

                    val animationProgress = if (ivElephantStartX != clampedTargetX) {
                        (currentX - ivElephantStartX) / (clampedTargetX - ivElephantStartX)
                    } else {
                        1f
                    }
                    val clampedProgress = animationProgress.coerceIn(0f, 1f)

                    // Gradually interpolate the volume (step-by-step)
                    val interpolatedVolume =
                        volumeScale + (targetVolume - volumeScale) * clampedProgress

                    audioPlayerHelper.adjustInternalVolume(interpolatedVolume)

                    Log.w("clampedProgress", "clampedProgress: $clampedProgress  , interpolatedVolume $interpolatedVolume")

                   // binding.tvVolume.text =  "Volume ${(String.format("%.2f", interpolatedVolume).toFloat() * 100)} %"
                    binding.tvVolume.text = "Volume ${(String.format(Locale.US, "%.2f", interpolatedVolume).toFloat() * 100)} %"


                }
            }
            manAnimator?.duration = duration
            elephantAnimator?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    volumeScale = targetVolume

                    if (index == 85) {
                        playerPos = 3
                    } else {
                        playerPos = 0
                    }
                    /*didFinishMoveAnimation = true
                    launch {
                        if (currentJob.isActive && currentJob == gifAnimJob) {
                            audioPlayerHelper.setMediaPlayer50()
                            playerPos = 3
                            animateAttackAndReturn(binding.ivElephant, binding.ivMan)
                        }
                    }*/
                }
            })

            manAnimator?.start()
            elephantAnimator?.start()
        }
        currentIndex = index
    }

    var currentAnimator: ValueAnimator? = null
    private var currentIndex = 0
    var currentX = 0f


    private fun calculateDistGif(): Float {
        var distance = 0f
        binding.apply {
            ivElephant.post {
                ivMan.post {

                    val position1 = IntArray(2)
                    val position2 = IntArray(2)

                    ivElephant.getLocationOnScreen(position1)
                    ivMan.getLocationOnScreen(position2)

                    // Coordinates
                    val x1 = position1[0].toFloat()
                    val y1 = position1[1].toFloat()
                    val x2 = position2[0].toFloat()
                    val y2 = position2[1].toFloat()

                    // Calculate Euclidean distance
                    distance = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))

                    Log.d("distance", "Distance: $distance pixels")

                    // Coordinates of imageView1
                    val width1 = ivElephant.width
                    val height1 = ivElephant.height

                    // Coordinates of imageView2
                    val width2 = ivMan.width
                    val height2 = ivMan.height

                    // Calculate horizontal and vertical margins
                    val horizontalMargin = if (x2 > x1) {
                        x2 - (x1 + width1) // Distance between right edge of imageView1 and left edge of imageView2
                    } else {
                        x1 - (x2 + width2) // Distance between right edge of imageView2 and left edge of imageView1
                    }

                    val verticalMargin = if (y2 > y1) {
                        y2 - (y1 + height1)
                        // Distance between bottom edge of imageView1 and top edge of imageView2
                    } else {
                        y1 - (y2 + height2)
                        // Distance between bottom edge of imageView2 and top edge of imageView1
                    }

                    // Ensure non-negative margins
                    val horizontalMarginAbs = abs(horizontalMargin)
                    val verticalMarginAbs = abs(verticalMargin)
                    Log.d(
                        "distance",
                        "MARGIN: horizontal margin-> $horizontalMarginAbs, vertical margin-> $verticalMarginAbs"
                    )

                }
            }
        }
        return distance
    }

    private fun calculateSpeedDifference(targetSpeed: Double, currentSpeed: Double): Double {
        return abs(targetSpeed - currentSpeed)
    }

    @SuppressLint("DefaultLocale")
    private fun calculateAveragePace(totalTime: Long): String {
        val elapsedTimeMinutes = totalTime / (1000 * 60).toFloat()  // covert to minute
        if (freeRunData?.distance!!.toInt() == 0) return "0:00"
        val distance = if (sharedPrefManager.getCurrentUser()?.unitOfMeasure == 1) {
            freeRunData?.distance!!.toInt()
        } else {
            ((freeRunData?.distance ?: 0.0) * 0.621371).toFloat()
        }
        val pace = elapsedTimeMinutes / distance.toInt()
        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()

        Log.d(
            "calculateAveragePace",
            "calculateAveragePace: ${String.format("%d:%02d $units", minutes, seconds)}"
        )
        return String.format("%d:%02d $units", minutes, seconds)

    }

    private fun calculatePaceFromSpeed(speedMps: Double): String {
        if (speedMps <= 0.00) {

            return "0:0 $units"
        }

        val disIn = if ((sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1) == 1) {
            1000.0
        } else {
            1609.34
        }
        val paceSecondsPerKm = disIn / speedMps
        val minutes = (paceSecondsPerKm / 60).toInt()
        val seconds = (paceSecondsPerKm % 60).toInt()

        Log.d(
            "calculatePaceFromSpeed",
            "calculatePaceFromSpeed: ${String.format("%d:%02d $units", minutes, seconds)}"
        )

        //if (speedMps <= 0) return "0:00"
        val paceMinutes = disIn / (speedMps * 60)
        val minutes1 = paceMinutes.toInt() - 3
        val seconds1 = ((paceMinutes - minutes1) * 60).toInt()
        Log.d(
            "calculatePaceFromSpeed",
            "calculatePaceFromSpeed1: ${String.format("%d:%02d min/km", minutes1, seconds1)}"
        )
        //showToast("pace ${String.format("%d:%02d min/km", minutes1, seconds1)}")
        return String.format("%d:%02d $units", minutes1, seconds1)
        //return String.format("%d:%02d $units", minutes, seconds)
    }

    private var currentZone = SpeedZone.ZONE_FAR
    private val hysteresisBuffer = 5.0
    private fun updateSpeedZone(distanceBetween: Double): SpeedZone {
        Log.d("currentZone", "updateSpeedZone: $currentZone")
        return when {
            distanceBetween <= 0 -> SpeedZone.ZONE_CLOSE
            distanceBetween in (45.0 - hysteresisBuffer)..(55.0 + hysteresisBuffer) -> SpeedZone.ZONE_MID
            distanceBetween >= 100 -> SpeedZone.ZONE_FAR
            else -> currentZone
        }
    }

    private fun distanceToIndex(distance: Double): Int {
        Log.d("distanceToIndex", "distanceToIndex: ${(100 - distance).toInt().coerceIn(0, 100)}")
        return when {
            distance >= 100f -> 0
            distance <= 0f -> 85
            else -> if ((100 - distance).toInt().coerceIn(0, 100) in 86..99) {
                85
            } else {
                (100 - distance).toInt().coerceIn(0, 100)
            }

        }

    }

    private fun stageCompleted() {

        binding.apply {
            consTopTimeVisible.visibility = View.GONE
            consPauseAndCompleted.visibility = View.VISIBLE

            val durationInMillis = (freeRunData?.durationInMin ?: 0) * 60 * 1000L
            val totalTimeInMillis = durationInMillis - remainingTimeInMillis

            //val differenceInSeconds = totalTimeInMillis / 1000
            val differenceInMinutes = totalTimeInMillis / (1000 * 60)
            //val differenceInHours = totalTimeInMillis / (1000 * 60 * 60)

            Log.d(
                "totalTimeInMillis", "stageCompleted:  $totalTimeInMillis , $differenceInMinutes"
            )

            tvAverageValue.text = calculateAveragePace(totalTimeInMillis)
            //tvDistanceValue.text = String.format(Locale.US, "%.2f", totalDistanceInKm)
            tvDistanceValue.text = String.format(
                Locale.US,
                "%.2f",
                totalDistanceInKm
            ) + "${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}"
            tvLenghtValue.text = pauseTime

        }

        stopMediaPlayerBackground()
        stopGifAnimation()
        pauseMediaPlayerAttack()

        pauseTimer()
        stopLocationService()
        stopProgressTimer()
        if (videoPlayer != null) {
            videoPlayer!!.stop()
            videoPlayer!!.release()
        }
        if (gifAnimJob != null && gifAnimJob!!.isActive) {
            gifAnimJob!!.cancel()
        }
        if (job != null && job!!.isActive) {
            job!!.cancel()
        }
    }

    private fun stageFailed() {

        val durationInMillis = (freeRunData?.durationInMin ?: 0) * 60 * 1000L
        val totalTimeInMillis = durationInMillis - remainingTimeInMillis

        binding.apply {
            consTopTimeVisible.visibility = View.GONE
            consPause.visibility = View.GONE
            consFailed.visibility = View.VISIBLE

            /* tvDistanceValuePauseFailed.text =
                 String.format(Locale.US, "%.2f", totalDistanceInKm)*/
            tvDistanceValuePauseFailed.text =
                "${String.format(Locale.US, " % .2f", totalDistanceInKm)} ${
                    measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)
                }"
            tvLenghtValueFailed.text = pauseTime
            tvAverageValueFailed.text = calculateAveragePace(totalTimeInMillis)
        }
        stopMediaPlayerBackground()
        stopGifAnimation()
        pauseMediaPlayerAttack()
        pauseTimer()
        stopLocationService()
        stopProgressTimer()
        if (videoPlayer != null) {
            videoPlayer!!.stop()
            videoPlayer!!.release()
        }

        if (gifAnimJob != null && gifAnimJob!!.isActive) {
            gifAnimJob!!.cancel()
        }
        if (job != null && job!!.isActive) {
            job!!.cancel()
        }
    }

    private fun loadInter() {
        interstitialAdManager.loadAd(callback = object :
            InterstitialAdManager.InterstitialAdCallback {
            override fun onAdLoaded() {
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                finish()
            }

            override fun onAdClosed() {
                finish()
            }

            override fun onAdShowed() {
            }

            override fun onAdClicked() {
            }
        })

    }

    private fun updateProgress(
        animalProgress: Float?,
        userProgress: Float,
        blinking: Boolean = false
    ) {
        //pending calculation errors


        binding.apply {
            customProgressBar.setProgress(animalProgress, userProgress)
            customProgressBar.setBlinkingProgress2(blinking)
            customProgressBar.post {
                val (progress1X, progress2X) = customProgressBar.getProgressXPositions()
                Log.d(
                    "updateProgress",
                    "updateProgress Progress1 X: $progress1X, Progress2 X: $progress2X"
                )

                animalLabel.x = progress1X
                userLabel.x = progress2X
            }
        }
    }

    private var lastTickTime: Long = System.currentTimeMillis()
    private var activeAnimalTime: Double = 0.0

    private var lastActiveTime = 0L
    private var totalActiveTime = 0L

    private fun startProgressTimer(duration: Double) {
        /*val speedMps =
            (freeRunData?.speed?.times(1000) ?: 1.0) / 3600 // Convert km/h to meters per second
        val totalTimeSeconds = (freeRunData?.distance!!.times(1000)) / speedMps // Time in seconds*/
        //val animalStartOffset = -70.0

       // val pace = convertPaceToSeconds(freeRunData?.pace.toString())
        val totalDistanceInMeters = (freeRunData?.distance ?: 1.0) * 1000
        // val duration = (freeRunData?.durationInMin ?: 1).times(60).toDouble()
        Log.d(
            "totalTimeSeconds",
            "startProgressTimer: totalTimeSeconds $duration, $elapsedProgressTime ,  ${((duration - elapsedProgressTime)).toLong()}"
        )
        progressTimer = object : CountDownTimer(
            ((duration - elapsedProgressTime) * 1000).toLong(),
            1000
        ) { // Tick every second
            override fun onTick(millisUntilFinished: Long) {
                elapsedProgressTime = (duration - millisUntilFinished / 1000)
                activeAnimalTime = (duration - millisUntilFinished / 1000)
                // Distance covered
                //distanceCovered = (elapsedProgressTime / duration) * totalDistanceInMeters
                distanceCovered = (-70.0) + (elapsedProgressTime / duration) * (totalDistanceInMeters + 70.0)

                val distanceBetween = distanceInMeters - distanceCovered

                val blinking = if (distanceBetween <= 100) true else false

                Log.e("distanceCovered", "distance covered by animal ==> $distanceCovered $distanceBetween")
                if (animalProgress < userProgress) {
                    animalProgress =
                        ((activeAnimalTime / duration) * 100).coerceIn(0.0, 100.0).toFloat()

                    updateProgress(animalProgress, userProgress, blinking)
                } else {
                    activeAnimalTime + (millisUntilFinished / 1000)
                }
                binding.tvDistanceAnimal.text = "Dist cover by Animal = ${distanceCovered}m"

                //val (progress1X, progress2X) = binding.customProgressBar.getProgressXPositions()

            }

            override fun onFinish() {
                stageFailed()
            }
        }
        progressTimer!!.start()
    }

    private fun stopProgressTimer() {
        progressTimer.let {
            Log.d("progressTimer", "stopProgressTimer: ")
            it?.cancel()
            progressTimer = null
        }

    }


    override fun onResume() {
        super.onResume()
        //simulateLocationUpdates(this, LocationManager.GPS_PROVIDER)
    }

    ////////////////////////////////////////////////////
    private fun simulateLocationUpdates(context: Context, providerName: String) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        automateJob = CoroutineScope(Dispatchers.Default).launch {
            locationManager.addTestProvider(
                providerName,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(providerName, true)


            val durationInSeconds = 600
            val startLatitude = 30.709082
            val startLongitude = 76.692683
            val meterInDegrees = 0.0000081 // ~1m in degrees


            val distanceInMeters = 10
            val totalOffset = distanceInMeters * meterInDegrees

            var totalDistance = 0.0

            // 🚀 Wait for 50 seconds before starting movement
            delay(10_000)

            for (i in 0 until durationInSeconds) {
                totalDistance += distanceInMeters

                if (totalDistance <= 100) {
                    Log.d(
                        "totalDistance",
                        "simulatePreciseMovementFor11Meters: totalDistance $totalDistance "
                    )
                    val location = Location(providerName).apply {
                        latitude = startLatitude + (i * totalOffset)
                        longitude = startLongitude
                        time = System.currentTimeMillis()
                        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        accuracy = 1.0f
                        speed = 1.51f // Simulate 1 m/s
                    }
                    locationManager.setTestProviderLocation(providerName, location)
                }

                delay(1000) // ⏱️ Simulate 1 second per movement
            }
            locationManager.removeTestProvider(providerName)
        }
    }
}

