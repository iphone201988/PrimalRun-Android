package com.primal.runs.ui.dashboard.start_run.running


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PorterDuff
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
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
import com.primal.runs.data.api.Constants.SAVE_RESULTS
import com.primal.runs.data.model.FreeRunModel
import com.primal.runs.data.model.SaveResultModel
import com.primal.runs.databinding.ActivityStageRunBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.permission.PermissionHandler
import com.primal.runs.ui.base.permission.Permissions
import com.primal.runs.ui.dashboard.start_run.StartRunVM
import com.primal.runs.ui.track_new.LocationService
import com.primal.runs.utils.AudioPlayerHelper
import com.primal.runs.utils.BadgeType
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.ImageUtils.generateInitialsBitmap
import com.primal.runs.utils.ImageUtils.measure
import com.primal.runs.utils.ImageUtils.paceToSpeed
import com.primal.runs.utils.InterstitialAdManager
import com.primal.runs.utils.SpeedZone
import com.primal.runs.utils.Status
import com.primal.runs.utils.showErrorToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@AndroidEntryPoint
class StageRunActivity : BaseActivity<ActivityStageRunBinding>(),
    LocationService.LocationCallbackListener {
    private val viewModel: StartRunVM by viewModels()

    private var attackSoundMediaPLayer: MediaPlayer? = null
    private var startSoundMediaPLayer: MediaPlayer? = null
    private var job: Job? = null
    private var gifAnimJob: Job? = null
    private var mainJob: Job? = null
    private var totalDistanceInKm: Float = 0.0f
    private var totalDistanceInm: Double = 0.0
    private var distanceInMeters: Float = 0f
    private var distanceCovered: Double = 0.0
    private var currentSpeedMps: Float = 0f
    private lateinit var locationService: LocationService
    private var isServiceRunning = false

    private var automateJob: Job? = null

    private var gifMovePoints: List<Float> = ArrayList()

    private var manAnimator: ValueAnimator? = null
    private var elephantAnimator: ValueAnimator? = null
    private var freeRunData: FreeRunModel? = null
    private var resultId: String? = null
    private var pauseTimer: CountDownTimer? = null
    private var progressTimer: CountDownTimer? = null
    private var elapsedProgressTime = 0.0
    private var lifeCount = 3
    private var pauseTime: String? = null
    private var volumeScale: Float = 0.05f
    private var targetVolume: Float = 1.0f

    private var videoPlayer: ExoPlayer? = null
    private var playerPos = 0
    private var revive = false
    private var userProgress: Float = 0f
    private var animalProgress: Float = 0f
    private var isAttackAnimationRunning = false
    private lateinit var animalProgressBadge: Map<BadgeType, Int>
    private var units = "/km"

    private var locationPause: Boolean = false
    private var pauseLocation: Location? = null
    private var stopService = false
    private var initialStop = false
    private var currentSpeedZone: Int? = null
    private var didFinishMoveAnimation = false

    private lateinit var interstitialAdManager: InterstitialAdManager
    private lateinit var audioPlayerHelper: AudioPlayerHelper

    override fun getLayoutResource(): Int {
        return R.layout.activity_stage_run
    }

    override fun onCreateView() {
        initView()
        initVideoPlayer()
        initObserver()
        initOnClick()
        loadInter()
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    private fun initObserver() {
        viewModel.homePlanDetailObserver.observe(this) {
            when (it!!.status) {
                Status.LOADING -> {
                    showLoading("")
                }

                Status.SUCCESS -> {
                    hideLoading()
                    when (it.message) {
                        SAVE_RESULTS -> {
                            stageCompleted()
                            try {
                                val myDataModel: SaveResultModel? =
                                    ImageUtils.parseJson(it.data.toString())

                                if (myDataModel != null) {
                                    if (myDataModel.success!!) {
                                        resultId = myDataModel.resultId
                                    } else {
                                        showToast(myDataModel.message)
                                    }
                                } else {
                                    Log.d("myDataModel", "initObserver: null ")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                Status.ERROR -> {
                    showToast(it.message.toString())
                    hideLoading()
                    /*if (it.message.equals("Unauthorized")) {
                        sharedPrefManager.clear()
                        startActivity(Intent(requireContext(), WelcomeActivity::class.java))
                        requireActivity().finish()
                    }*/
                }
            }
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
                        while (distanceInMeters < 100) {
                            Log.i("ffqwfqwfqw", " dqwqwdw -> distanceInMeters $distanceInMeters")
                            delay(1000)

                        }
                        startProgressTimer()
                        stopService = false
                        initialStop = true
                        Log.i("ffqwfqwfqw", " reached -> distanceInMeters $distanceInMeters")
                    }

                    /*Handler(Looper.getMainLooper()).postDelayed({

                        Handler(Looper.getMainLooper()).postDelayed({
                            startProgressTimer()
                            stopService = false
                            initialStop = true
                        }, 5000)
                    }, 10000)*/
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
                    pauseTimer?.start()
                    manAnimator?.pause()
                    elephantAnimator?.pause()
                    videoPlayer!!.pause()
                    progressTimer.let { time ->
                        time?.cancel()
                    }
                    val durationInMillis = (freeRunData?.durationInMin ?: 0) * 60 * 1000L
                    val totalTimeInMillis = durationInMillis - remainingTimeInMillis

                    binding.apply {
                        /* tvDistanceValuePause.text =
                             String.format(Locale.US, "%.2f", totalDistanceInKm)*/
                        tvDistanceValuePause.text =
                            "${String.format(Locale.US, " %.2f", totalDistanceInKm)} ${
                                measure(
                                    sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1
                                )
                            }"
                        tvLenghtValuePause.text = pauseTime
                        tvAverageValuePause.text = calculateAveragePace(totalTimeInMillis)

                        pauseTimer()
                        stopGifAnimation()
                        pauseMediaPlayerBackground()
                        pauseMediaPlayerAttack()
                        consTopTimeVisible.visibility = View.GONE
                        consPause.visibility = View.VISIBLE
                    }

                    if (gifAnimJob != null && gifAnimJob!!.isActive) {
                        gifAnimJob!!.cancel()
                    }
                    if (job != null && job!!.isActive) {
                        job!!.cancel()
                    }
                }

                R.id.ivBack, R.id.tvGiveUp, R.id.tvGiveUpFailed -> {
                    // soundModulation.audioManager.abandonAudioFocusRequest(soundModulation.audioFocusRequest)
                    pauseTimer?.cancel()
                    finish()
                }
                /*R.id.tvGiveUp->{
                    binding.consPause.visibility = View.GONE
                    binding.consExit.visibility = View.VISIBLE
                }

                R.id.tvExitYes->{
                    finish()
                }

                R.id.tvExitNO->{
                    binding.consPause.visibility = View.VISIBLE
                    binding.consExit.visibility = View.GONE
                }*/

                R.id.tvContinue -> {
                    if (interstitialAdManager.isAdReady()) {
                        interstitialAdManager.loadAd(callback = object :
                            InterstitialAdManager.InterstitialAdCallback {
                            override fun onAdLoaded() {
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                pauseTimer?.cancel()
                                finish()
                            }

                            override fun onAdClosed() {
                                pauseTimer?.cancel()
                                finish()
                            }

                            override fun onAdShowed() {
                            }

                            override fun onAdClicked() {
                            }
                        })
                        interstitialAdManager.showAd()
                    } else {
                        showToast("Ad is not ready yet")
                        pauseTimer?.cancel()
                        finish()
                    }
                }

                R.id.rlReviveFailed -> {

                    if (interstitialAdManager.isAdReady()) {
                        interstitialAdManager.showAd()
                    } else {
                        showToast("Ad is not ready yet")
                        revive()
                    }
                }

                R.id.tvResume -> {
                    if (revive) {
                        reviveResume()
                    } else {

                        locationPause = false
                        lastLocation = pauseLocation
                        pauseTimer?.cancel()
                        videoPlayer!!.play()
                        startProgressTimer()
                        binding.apply {
                            resumeTimer()
                            startGifAnimation()
                            playMediaPlayerBackground()
                            consTopTimeVisible.visibility = View.VISIBLE
                            consPause.visibility = View.GONE
                        }
                    }
                    /*if(playerPos == 2){
                        soundModulation.decreaseOtherAppsVolumeAndIncreaseMyApp()
                    }else{
                        soundModulation.resetVolumes()
                    }*/

                }

                R.id.tvWatchReplay -> {
                    val intent = Intent(this, RunPreviewActivity::class.java).apply {

                        putExtra("resultId", resultId)
                        putExtra("lifeCount", lifeCount)
                        putExtra("timer", timeElapsedInSeconds)
                    }
                    startActivity(intent)
                    finish()
                }

                R.id.tvWatchReplayFailed -> {
                    val intent = Intent(this, RunPreviewActivity::class.java).apply {
                        //putExtra("resultId", resultId)
                        putExtra("videoUrl", intent.getStringExtra("videoUrl"))
                        putExtra("resultId", "6798d852f08e1676066676e0")
                        putExtra("lifeCount", lifeCount)
                        putExtra("timer", timeElapsedInSeconds)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun updateProgress(
        animalProgress: Float?,
        userProgress: Float,
        blinking: Boolean = false
    ) {
        binding.apply {
            customProgressBar.setProgress(animalProgress, userProgress)
            customProgressBar.setBlinkingProgress2(blinking)
            customProgressBar.post {
                val (progress1X, progress2X) = customProgressBar.getProgressXPositions()
                Log.d("ProgressX", "Progress1 X: $progress1X, Progress2 X: $progress2X")

                animalLabel.x = progress1X
                userLabel.x = progress2X
            }
        }
    }

    private fun revive() {
        revive = true
        binding.tvResume.text = getString(R.string.resume)
        val durationInMillis = (freeRunData?.durationInMin ?: 0) * 60 * 1000L
        val totalTimeInMillis = durationInMillis - remainingTimeInMillis
        binding.apply {
            consFailed.visibility = View.GONE
            //tvDistanceValuePause.text = String.format(Locale.US, "%.2f", totalDistanceInKm)
            tvDistanceValuePause.text =
                "${
                    String.format(
                        Locale.US,
                        " %.2f",
                        totalDistanceInKm
                    )
                } ${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}"
            tvLenghtValuePause.text = pauseTime
            tvAverageValuePause.text = calculateAveragePace(totalTimeInMillis)

            pauseTimer()
            stopGifAnimation()
            pauseMediaPlayerBackground()
            pauseMediaPlayerAttack()
            consTopTimeVisible.visibility = View.GONE
            consPause.visibility = View.VISIBLE
            consLife?.visibility = View.VISIBLE
        }
    }

    private fun reviveResume() {
        revive = false
        binding.apply {
            consLife?.visibility = View.GONE
            consPause.visibility = View.GONE
            consTopTimeVisible.visibility = View.VISIBLE
            //val ivManTargetX = (gifMovePoints[4]) - ivMan.width - 0f
            val ivManTargetX = gifMovePoints[200 - 0] - (binding.ivMan.width / 2)


            // val ivElephantTargetX = gifMovePoints[0] + 0f
            val ivElephantTargetX = gifMovePoints[0] - (binding.ivElephant.width / 2)

            ivElephant.x = ivElephantTargetX
            ivMan.x = ivManTargetX
            audioPlayerHelper.setHighVolume(volumeScale, 0.05f)

            resumeTimer()
            startGifAnimation()
            playMediaPlayerBackground()
            if (videoPlayer != null) {
                videoPlayer!!.play()
            }
            lifeCount = 1
            playerPos = 0
            currentIndex = 0
            ivLife1.setImageDrawable(
                ContextCompat.getDrawable(
                    this@StageRunActivity, R.drawable.baseline_favorite_24
                )
            )
            Handler(Looper.getMainLooper()).postDelayed({
                startLocationService()
            }, 10000)
        }

    }

    private fun initView() {
        //audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioPlayerHelper = AudioPlayerHelper(this)
        interstitialAdManager = InterstitialAdManager(this)

        freeRunData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("freeRunData", FreeRunModel::class.java)
        } else {
            intent.getSerializableExtra("freeRunData") as? FreeRunModel
        }
        binding.tvDistanceValueTop.text =
            "0.0 ${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}"
        //binding.tvPaceValue.text = "0.0${unitsOfMeasure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}"
        binding.tvPaceValue.text = calculatePaceFromSpeed(currentSpeedMps.toDouble())
        units = if (sharedPrefManager.getCurrentUser()?.unitOfMeasure == 1) "/km" else "/miles"

        enableFullScreenMode()
        initStartSoundMediaPlayer()


        Log.d("startRunData", "initObserver: ${Gson().toJson(freeRunData)}")
        /*if (freeRunData != null) {
            currentSpeedMps = freeRunData?.speed?.toFloat() ?: 1f
        }*/
        totalDistanceInm = (freeRunData?.distance ?: 0.0) * 1000
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        Log.d("gifMovePoints", "screenWidth: $screenWidth")
        // Divide the screen into 5 points

        gifMovePoints = List(201) { index ->  // 21 points for 20 divisions
            screenWidth * (index / 200f)  // 0%, 5%, 10%, ..., 100%
        }

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

        countDown()

    }

    @OptIn(UnstableApi::class)
    private fun initVideoPlayer() {
        videoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = videoPlayer

        val videoUrl =
            "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/Stage1.mp4"
        //val videoUrl = "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/1080/Stage1.mp4"
        //val videoUrl = "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/45fps/Stage1.mp4"
        val uri = videoUrl.toUri()
        /*val rawResourceId = R.raw.stage1
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


                        //binding.progress.values = listOf(18f, 50f)
                        binding.frameAgeLabel.visibility = View.VISIBLE

                        /*binding.progress.post {
                            val sliderLocation = IntArray(2)
                            binding.progress.getLocationOnScreen(sliderLocation)
                            val sliderX = sliderLocation[0]
                            val sliderY = sliderLocation[1]

                            // Get the track width and paddings
                            val trackWidth =
                                binding.progress.width - binding.progress.paddingStart - binding.progress.paddingEnd

                            // Calculate thumb positions for the minimum and maximum values
                            val thumbLowerPositionX = sliderX + binding.progress.paddingStart // Start position
                            val thumbUpperPositionX =
                                sliderX + binding.progress.paddingStart + trackWidth // End position

                            // Y Position of the thumbs (approximation: center of the slider)
                            val thumbY = sliderY + (binding.progress.height / 2)

                            val lowerThumbPosX = calculateRangeThumbPosition( binding.progress, 18f)
                            val upperThumbPosX = calculateRangeThumbPosition( binding.progress, 50f)

                            agePosition = Pair(lowerThumbPosX, upperThumbPosX)
                            adjustLabelPosition(
                                binding.animalLabel!!,
                                binding.userLabel!!,
                                lowerThumbPosX,
                                upperThumbPosX
                            )
                        }*/

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
                        // bgImage.visibility = View.VISIBLE
                        playerView.visibility = View.VISIBLE
                        clProgress.visibility = View.VISIBLE

                        consTopTimeVisible.visibility = View.VISIBLE
                        consGetReady.visibility = View.GONE
                        ivMan.visibility = View.VISIBLE
                        ivElephant.visibility = View.VISIBLE
                        startGifAnimation()
                        if (freeRunData != null) {
                            startTimer(freeRunData!!.durationInMin!!)
                            //startTimer(1)
                        }
                        playMediaPlayerBackground()
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
                                Glide.with(this@StageRunActivity)
                                    .load(profileImage)
                                    .placeholder(R.drawable.iv_dummy) // Placeholder while loading
                                    .into(binding.ivUser)
                            }
                        }
                        val badgeType = BadgeType.fromType(freeRunData?.budgeType ?: -1)
                        badgeType?.let {
                            val badgeDrawable = animalProgressBadge[it]
                            badgeDrawable?.let { drawable ->
                                Glide.with(this@StageRunActivity)
                                    .load(drawable)
                                    .placeholder(R.drawable.iv_dummy) // Placeholder while loading
                                    .into(binding.ivAnimal)
                                //Glide.with(this).asGif().load(drawable).into(binding.ivElephant)
                            }
                        }

                        /*Glide.with(this@StageRunActivity)
                            .load(sharedPrefManager.getCurrentUser()?.profileImage)
                            .placeholder(R.drawable.iv_dummy).error(R.drawable.iv_dummy)
                            .into(ivUser)*/
                        /*startSoundMediaPLayer?.release()
                        startSoundMediaPLayer = null*/
                    }
                } catch (e: Exception) {
                    showToast(e.message)
                }
            }
        }
        countDownTimer.start()
    }


    private fun startGifAnimation() {
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

        // Reset translations
        /*binding.ivMan.translationX = 0f
        binding.ivElephant.translationX = 0f*/
    }

    private var countDownTimerForTotalTime: CountDownTimer? = null
    private var remainingTimeInMillis: Long = 0L // To track remaining time
    private var timeElapsedInSeconds = 0
    private fun startTimer(totalMinutes: Int) {
        // Calculate total time in milliseconds
        val totalTimeInMillis = totalMinutes * 60 * 1000L
        if (remainingTimeInMillis == 0L) {
            remainingTimeInMillis = totalTimeInMillis
        }
        countDownTimerForTotalTime = object : CountDownTimer(remainingTimeInMillis, 1000) {

            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished // Update the remaining time
                timeElapsedInSeconds++/* val hours = millisUntilFinished / (1000 * 60 * 60) % 24
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
                BadgeType.WOLF to R.drawable.iv_run_wolf,
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
                    // Set the data source to the MP3 URL
                    setOnPreparedListener {
                        Log.d("MediaPlayer", "ready")
                        // start()
                    } // Start playback when ready
                    setOnCompletionListener {
                        Log.d("MediaPlayer", "setOnCompletionListener")
                        if (!isPlaying) {
                            start() // Restart playback when the track completes
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
            if (!mediaPlayerForBgVoice!!.isPlaying) {
                mediaPlayerForBgVoice?.start()
            } else {
                Log.d("MediaPlayer", "MediaPlayer is already playing, skipping start")
            }
        }*/

    }

    private fun pauseMediaPlayerBackground() {
        /* if (mediaPlayerForBgVoice != null) {
             try {
                 if (mediaPlayerForBgVoice?.isPlaying == true) {
                     mediaPlayerForBgVoice?.pause()
                 }
             } catch (e: Exception) {
                 e.printStackTrace()
             }
         }*/
        if (audioPlayerHelper.mediaPlayer != null) {
            try {
                if (audioPlayerHelper.mediaPlayer?.isPlaying == true) {
                    // audioPlayerHelper.mediaPlayer?.pause()
                    audioPlayerHelper.pauseAudio()
                }/*if (mediaPlayerForBgVoice?.isPlaying == true) {
                    mediaPlayerForBgVoice?.pause()
                }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playMediaPlayerBackground() {
        /*if (mediaPlayerForBgVoice != null) {
            try {
                if (mediaPlayerForBgVoice?.isPlaying != true) {
                    mediaPlayerForBgVoice?.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/

        if (audioPlayerHelper.mediaPlayer != null) {
            try {
                if (audioPlayerHelper.mediaPlayer?.isPlaying != true) {
                    // audioPlayerHelper.mediaPlayer?.pause()
                    audioPlayerHelper.resumeAudio()
                    audioPlayerHelper.adjustInternalVolume(volumeScale)
                    /*if (playerPos == 2) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            audioPlayerHelper.lowerExternalAudio()
                        }
                    }*/
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopMediaPlayerBackground() {/*if (mediaPlayerForBgVoice != null) {
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
    }

    private fun resumeTimer() {
        //startTimer(remainingTimeInMillis.toInt() / 60000) // Convert ms to minutes
        val minutes = (timeElapsedInSeconds / (1000 * 60)) % 60

        startTimer(minutes) // Convert ms to minutes
    }


    /********  Attack sound media player**********/
    private fun initAttackSoundMediaPlayer() {
        if (attackSoundMediaPLayer == null) {
            attackSoundMediaPLayer = MediaPlayer().apply {
                try {
                    if (freeRunData?.attackSound != null) {
                        setDataSource(freeRunData?.attackSound)
                    } else {
                        showToast("background Sound is null")
                        return
                    }
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
        binding.apply {
            ivMan.setColorFilter(
                ContextCompat.getColor(
                    this@StageRunActivity, android.R.color.holo_red_dark
                ), // Red tint color
                PorterDuff.Mode.SRC_ATOP // Mode to blend the color
            )

            when (lifeCount) {

                2 -> {
                    ivLife3.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@StageRunActivity, R.drawable.unfav
                        )
                    )
                }

                1 -> {
                    ivLife2.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@StageRunActivity, R.drawable.unfav
                        )
                    )
                }

                0 -> {
                    ivLife1.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@StageRunActivity, R.drawable.unfav
                        )
                    )
                }
            }
        }
    }

    fun animateAttackAndReturn(ivElephant: View, ivMan: View) {
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
                if (attackSoundMediaPLayer?.isPlaying != true) {
                    //pauseMediaPlayerBackground()
                    attackSoundMediaPLayer?.start()

                }
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                lifeCount -= 1
                hitGif()

                // stopLocationService()
                stopProgressTimer()
                stopService = true

                lifecycleScope.launch {
                    delay(10000)
                    stopService = false
                    Log.d("stopLocationService", "onAnimationStarted:->")
                    if (binding.consPause.isGone) {


                    }
                }

                /*Handler(Looper.getMainLooper()).postDelayed({
                    //startLocationService()
                    stopService = false
                    startProgressTimer()
                }, 10000)*/
                addExtraTime(10)

                returnAnimator.start()
            }
        })

        returnAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                Log.d("returnAnimator", "onAnimationEnd: $lifeCount , ")
                unHitGif()
                Handler(Looper.getMainLooper()).postDelayed({
                    audioPlayerHelper.setMediaPlayerFull()
                }, 1000)
                attackSoundMediaPLayer?.pause()
                attackSoundMediaPLayer?.seekTo(0) // Reset to the start
                //playMediaPlayerBackground()
                cancelSoundPlayback()
                isAttackAnimationRunning = false
                try {
                    if (lifeCount == 0) {
                        val durationInMillis = (freeRunData?.durationInMin ?: 0) * 60 * 1000L
                        val totalTimeInMillis = durationInMillis - remainingTimeInMillis
                        binding.apply {
                            consTopTimeVisible.visibility = View.GONE
                            consFailed.visibility = View.VISIBLE

                            /*tvDistanceValuePauseFailed.text =
                                String.format(Locale.US, "%.2f", totalDistanceInKm)*/
                            tvDistanceValuePauseFailed.text =
                                "${String.format(Locale.US, " % .2f", totalDistanceInKm)} ${
                                    measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)
                                }"
                            tvLenghtValueFailed.text = pauseTime
                            tvAverageValueFailed.text = calculateAveragePace(totalTimeInMillis)
                        }

                        pauseMediaPlayerBackground()
                        stopGifAnimation()
                        pauseMediaPlayerAttack()

                        pauseTimer()
                        stopLocationService()
                        stopProgressTimer()
                        if (videoPlayer != null) {
                            videoPlayer!!.pause()
                        }

                        if (gifAnimJob != null && gifAnimJob!!.isActive) {
                            gifAnimJob!!.cancel()
                        }
                        if (job != null && job!!.isActive) {
                            job!!.cancel()
                        }

                    } else {


                        /*addExtraTime(10)
                        stopLocationService()
                        stopProgressTimer()
                        Handler(Looper.getMainLooper()).postDelayed({
                            startLocationService()
                            startProgressTimer()
                        }, 10000)*/
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

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

    private fun countDown() {
        pauseTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvResume.text = "resume (${(millisUntilFinished / 1000)})"
            }

            override fun onFinish() {
                showToast("time ended")

                binding.consPause.visibility = View.GONE
                binding.consFailed.visibility = View.VISIBLE

                val durationInMillis = (freeRunData?.durationInMin ?: 0) * 60 * 1000L
                val totalTimeInMillis = durationInMillis - remainingTimeInMillis

                binding.tvAverageValueFailed.text = calculateAveragePace(totalTimeInMillis)

                stopMediaPlayerBackground()
                stopGifAnimation()
                pauseMediaPlayerAttack()

                pauseTimer()
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
                        Log.d("setOnCompletionListener", "setOnCompletionListener")
                        startSoundMediaPLayer?.release()
                        startSoundMediaPLayer = null

                        if (audioPlayerHelper.mediaPlayer != null) {
                            audioPlayerHelper.mediaPlayer!!.start()

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                audioPlayerHelper.releaseAudioFocus()
                            }
                        }
                    }
                    startSoundMediaPLayer
                    prepareAsync() // Prepare the media player asynchronously
                } catch (e: Exception) {
                    Log.e("MediaPlayer", "Error initializing media player", e)
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopMediaPlayerBackground()
        stopGifAnimation()
        pauseMediaPlayerAttack()
        videoPlayer?.stop()
        videoPlayer?.release()
        startSoundMediaPLayer?.stop()
        startSoundMediaPLayer?.release()
        attackSoundMediaPLayer?.stop()

        audioPlayerHelper.stopAudio()
        audioPlayerHelper.cancelVolumeAdjustment()
        audioPlayerHelper.restoreExternalAudio()
        ///attackSoundMediaPLayer?.release()

        pauseTimer()
        stopProgressTimer()
        stopLocationService()
        if (gifAnimJob != null && gifAnimJob!!.isActive) {
            gifAnimJob!!.cancel()
        }
        if (job != null && job!!.isActive) {
            job!!.cancel()
        }
    }

    /*********** Location related services & handling****************/
    private val locationServiceConnection = object : ServiceConnection {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            locationService.startLocationUpdates(this@StageRunActivity)
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
    private val MIN_DISTANCE_CHANGE: Float = 10f

    @SuppressLint("SetTextI18n")
    override fun onLocationChanged(location: Location) {
        if (!binding.consPause.isVisible) {

            if (lastLocation != null) {

                val distance = lastLocation!!.distanceTo(location)
                if (distance > MIN_DISTANCE_CHANGE) {
                    distanceInMeters += distance

                    //distanceInMeters += distance
                    //val distanceInMeters = lastLocation!!.distanceTo(location)
                    //totalDistanceInKm += (distanceInMeters / 1000) // Convert meters to kilometers and add
                    totalDistanceInKm = if ((sharedPrefManager.getCurrentUser()?.unitOfMeasure
                            ?: 1) == 1
                    ) {
                        (distanceInMeters / 1000)
                    } else {
                        ((distanceInMeters / 1000) * 0.621371).toFloat()
                    }
                } else {
                    Log.i("Progress", "Ignored fluctuation: ${"%.2f".format(distance)}m")
                }

            }
            lastLocation = location
            val dis = String.format(Locale.US, "%.2f", totalDistanceInKm)
            binding.tvDistanceValueTop.text =
                "$dis ${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}"

            userProgress =
                ((distanceInMeters / totalDistanceInm) * 100).coerceIn(0.0, 100.0).toFloat()
            //((totalDistanceInKm / freeRunData?.distance!!) * 100).coerceIn(0.0, 100.0).toFloat()
            updateProgress(null, userProgress)

            Log.i(
                "totalDistanceInKm",
                "totalDistanceInKm: $totalDistanceInKm  , ${freeRunData?.distance}"
            )
            // showToast("${location.speed}")

            binding.tvDistanceUser.text = "Dist cover by user = ${distanceInMeters}m"
            // binding.tvPaceValue.text = "${progress}${unitsOfMeasure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}"
            if (freeRunData != null) {
                if (totalDistanceInKm >= (freeRunData?.distance ?: 0.0)) {
                    // if(totalDistanceInKm >= 2.0){
                    //stageCompleted()

                    val request = HashMap<String, Any>()
                    request["planId"] = freeRunData?.planId.toString()
                    request["stageId"] = freeRunData?.stageId.toString()
                    request["badgeId"] = freeRunData?.badgeId.toString()
                    request["distance"] = freeRunData?.distance.toString()
                    request["duration"] = freeRunData?.durationInMin.toString()
                    request["averageSpeed"] = freeRunData?.speed.toString()
                    request["resultStatus"] = "1"
                    request["resultType"] = "1"

                    if (freeRunData?.difficultyLevel != 1) {
                        request["score"] = lifeCount.toString()
                    }
                    viewModel.saveResultApi(SAVE_RESULTS, request)
                    stopLocationService()
                    return
                }
            }
            if (initialStop) {
                Log.i("speed", "speed: $currentSpeedMps")
                handlingDistanceBetweenGif(location)
            }
            //handlingDistanceBetweenGif(location)

        } else {

            if (locationPause) {
                locationPause = false
                pauseLocation = location
            }

            pauseTimer()
            stopGifAnimation()
            pauseMediaPlayerBackground()
            pauseMediaPlayerAttack()
        }
    }

    override fun onSpeedChanged(speed: Float) {

        //currentSpeedMps = location.speed
        currentSpeedMps = if (speed < 0.19f) 0f else speed
        Log.i("currentSpeedMps", "currentSpeedMps: ${speed})")
        //currentSpeedMps = 2.1f
        binding.tvSpeed.text = "GPS Speed: $speed m/s "
        binding.tvPaceValue.text = calculatePaceFromSpeed(currentSpeedMps.toDouble())
    }

    private fun handlingDistanceBetweenGif(location: Location) {
        if (freeRunData != null) {
            //currentSpeedMps = (BigDecimal(location.speed.toDouble()).setScale(3, RoundingMode.HALF_UP)).toFloat() // Speed in m/s
            currentSpeedMps = if (location.speed < 0.19f) 0f else location.speed
            //currentSpeedMps = 21f
            binding.tvSpeed?.text = (location.speed * 3.6).toString()

            val miles =
                (sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1) != 1

            ///val targetSpeedMps = paceToSpeed(null , miles)
            val targetSpeedMps = paceToSpeed(freeRunData?.speed!!, miles)
            /*val targetSpeedMps = (freeRunData?.speed?.toFloat()
                ?: 1f) / 3.6 // Convert target speed from km/h to m/s*/

            val speedDifference =
                calculateSpeedDifference(targetSpeedMps, currentSpeedMps.toDouble())

            val distanceBetween = distanceInMeters - distanceCovered

            val newSpeedZone = updateSpeedZone(distanceBetween)

            /*targetVolume = when {
                distanceBetween < 0 -> 1.0f
                distanceBetween >= 100 -> 0.0f
                else -> (1.0f - (distanceBetween / 100.0f)).coerceIn(0.0, 1.0).toFloat()
            }*/

            /*targetVolume = when {
                distanceBetween >= 100f -> 0.0f
                distanceBetween in 81f..100f -> {
                    0.0f
                }

                distanceBetween in 61f..80f -> {
                    0.25f
                }

                distanceBetween in 41f..60f -> {
                    0.50f
                }

                distanceBetween in 21f..40f -> {
                    0.75f
                }

                distanceBetween in 0f..20f -> {
                    1.0f
                }

                distanceBetween >= 0f -> 1.0f
                else -> 1.0f
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

                distanceBetween >= 0f -> 1.0f
                else -> 0.7f
            }


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
                /*audioPlayerHelper.setMediaPlayer50()
                //playerPos = 3
                animateAttackAndReturn(binding.ivElephant, binding.ivMan)*/
            }

            /*if (newSpeedZone != currentZone) {
                mainJob?.cancel() // cancel previous running animation
                mainJob = CoroutineScope(Dispatchers.Main).launch {
                    currentZone = newSpeedZone

                    when (newSpeedZone) {
                        SpeedZone.ZONE_CLOSE -> moveGifAnim(gifMovePoints[2])
                        SpeedZone.ZONE_MID -> {
                            forwardGifAnim(gifMovePoints[1], gifMovePoints[3])
                        }
                        SpeedZone.ZONE_FAR -> forwardGifAnim(gifMovePoints[0], gifMovePoints[4])
                    }
                }
            } else if (newSpeedZone == SpeedZone.ZONE_CLOSE && didFinishMoveAnimation) {

                // Same zone, but we want to re-trigger the attack
                if (distanceBetween <= 0) {
                    audioPlayerHelper.setMediaPlayer50()
                    animateAttackAndReturn(binding.ivElephant, binding.ivMan)
                }
                // reset after re-use
            }*/

        }

        calculateDistGif()
    }


    private fun moveGifAnim(gifXPoints: Float = 0f, distance: Float = 0f) {
        // Get screen width

        /*val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels*/

        if (gifAnimJob != null && gifAnimJob!!.isActive) {
            gifAnimJob!!.cancel()
        }
        gifAnimJob = CoroutineScope(Dispatchers.Main).launch {
            val currentJob = coroutineContext.job
            binding.apply {
                // Calculate initial positions
                val ivManStartX = ivMan.x
                val ivElephantStartX = ivElephant.x

                // Calculate the center positions for both images

                Log.d(
                    "moveGifAnim", "moveGifAnim: width ${(gifXPoints)}, moving distance $distance"
                )

                val ivManTargetX = (gifXPoints) + distance // Offset to prevent collision
                val ivElephantTargetX =
                    (gifXPoints) - ivElephant.width - distance  // Offset to prevent collision

                // Animate the man's image
                // ivMan.animate().translationX(ivManTargetX).setDuration(500).start()
                audioPlayerHelper.setLowVolume(volumeScale, 1.0f)

                manAnimator = ValueAnimator.ofFloat(ivManStartX, ivManTargetX)
                manAnimator?.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    ivMan.x = value

                    /* if (volumeScale < 1.0f) {
                         volumeScale += 0.001f
                         updateMediaPlayerVolume(volumeScale)
                     }*/
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
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        // playMediaPlayerAttack()
                        didFinishMoveAnimation = true
                        launch {
                            delay(1000)
                            if (currentJob.isActive && currentJob == gifAnimJob) {
                                playerPos = 2

                                //animateAttackAndReturn(binding.ivElephant, binding.ivMan)

                                animateAttackAndReturn(ivElephant, ivMan)
                            } else {
                                Log.d(
                                    "gifAnimJob",
                                    "Skipped animateAttackAndReturn due to job mismatch or cancellation"
                                )
                            }
                        }

                        // Toast.makeText(context, "Animation ended!", Toast.LENGTH_SHORT).show()
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

        if (gifAnimJob != null && gifAnimJob!!.isActive) {
            gifAnimJob!!.cancel()
        }
        gifAnimJob = CoroutineScope(Dispatchers.Main).launch {
            binding.apply {

                val ivManStartX = ivMan.x
                val ivElephantStartX = ivElephant.x
                didFinishMoveAnimation = false
                Log.d(
                    "moveGifAnim", "moveGifAnim: width ${(gifXEndPoint)}, moving distance $distance"
                )

                val ivManTargetX =
                    (gifXEndPoint) - ivMan.width - distance // Offset to prevent collision
                val ivElephantTargetX = gifXStartPoint + distance  // Offset to prevent collision

                // Animate the man's image
                // ivMan.animate().translationX(ivManTargetX).setDuration(500).start()
                when (gifXStartPoint) {
                    gifMovePoints[1] -> {
                        audioPlayerHelper.setMediumVolume(volumeScale, 0.5f)
                    }

                    gifMovePoints[0] -> {
                        audioPlayerHelper.setHighVolume(volumeScale, 0.05f)
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

            /*if(index == 0){
            points[index] + binding.ivElephant.width
            } else {
            points[index] - binding.ivElephant.width
            }*/

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

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // Define start and mid X range
                    /* val startX = gifMovePoints[0]
                     val midX = gifMovePoints[85] // midpoint of screen

                     // Clamp currentX to [startX, midX] to avoid overshooting
                     val clampedX = currentX.coerceIn(startX, midX)

                     // Map position to volume (0.0 to 1.0)
                     val volumeProgress = (clampedX - startX) / (midX - startX)*/
                    //val volume = volumeProgress.coerceIn(0f, 1f)
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                    val animationProgress = if (ivElephantStartX != clampedTargetX) {
                        (currentX - ivElephantStartX) / (clampedTargetX - ivElephantStartX)
                    } else {
                        1f
                    }

                    // Clamp to avoid going over
                    val clampedProgress = animationProgress.coerceIn(0f, 1f)
                    val interpolatedVolume =
                        volumeScale + (targetVolume - volumeScale) * clampedProgress

                    audioPlayerHelper.adjustInternalVolume(interpolatedVolume)

                    // volumeScale = volumeScale + (targetVolume - volumeScale) * clampedProgress
                    // Set the volume
                    //audioPlayerHelper.adjustInternalVolume(volumeScale)

                    Log.d("ElephantAnimation", "X: $currentX, index $index Volume: $volumeScale")

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

    private var currentIndex = 0


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
                        y2 - (y1 + height1) // Distance between bottom edge of imageView1 and top edge of imageView2
                    } else {
                        y1 - (y2 + height2) // Distance between bottom edge of imageView2 and top edge of imageView1
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
        return BigDecimal(abs(targetSpeed - currentSpeed)).setScale(3, RoundingMode.HALF_UP)
            .toDouble()
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

    private fun calculateAveragePace(totalTime: Long): String {

        val elapsedTimeMinutes = totalTime / (1000 * 60).toFloat()

        if (freeRunData?.distance!!.toInt() == 0) return "0:00"
        /*val distance = if (sharedPrefManager.getCurrentUser()?.unitOfMeasure == 1) {
            freeRunData?.distance!!.toInt()
        } else {
            ((freeRunData?.distance ?: 0.0) * 0.621371).toFloat()
        }*/
        val pace = elapsedTimeMinutes / freeRunData?.distance!!.toInt()
        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()

        Log.d(
            "calculateAveragePace",
            "calculateAveragePace: ${String.format("%d:%02d min/km", minutes, seconds)}"
        )

        return String.format("%d:%02d $units", minutes, seconds)
    }

    private fun calculatePaceFromSpeed(speedMps: Double): String {
        if (speedMps <= 0.00) return "0:0 $units"

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
            "calculatePaceFromSpeed: $speedMps , ${
                String.format(
                    "%d:%02d $units",
                    minutes,
                    seconds
                )
            }"
        )

        return String.format("%d:%02d $units", minutes, seconds)
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


            when (lifeCount) {

                3 -> {
                    runPoint1.setImageResource(R.drawable.baseline_star_24)
                    runPoint2.setImageResource(R.drawable.baseline_star_24)
                    runPoint3.setImageResource(R.drawable.baseline_star_24)
                }

                2 -> {
                    runPoint1.setImageResource(R.drawable.baseline_star_24)
                    runPoint2.setImageResource(R.drawable.baseline_star_24)
                    runPoint3.setImageResource(R.drawable.unselect_star)
                }

                1 -> {
                    runPoint1.setImageResource(R.drawable.baseline_star_24)
                    runPoint2.setImageResource(R.drawable.unselect_star)
                    runPoint3.setImageResource(R.drawable.unselect_star)
                }

                else -> {
                    runPoint1.setImageResource(R.drawable.unselect_star)
                    runPoint2.setImageResource(R.drawable.unselect_star)
                    runPoint3.setImageResource(R.drawable.unselect_star)
                }
            }
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
                revive()
            }

            override fun onAdClosed() {
                revive()
            }

            override fun onAdShowed() {
            }

            override fun onAdClicked() {
            }
        })

    }

    private fun startProgressTimer() {
        /* val speedMps =
             (freeRunData?.speed?.times(1000) ?: 1.0) / 3600 // Convert km/h to meters per second
         val totalTimeSeconds = (freeRunData?.distance!!.times(1000)) / speedMps // Time in seconds*/

        //val pace = convertPaceToSeconds(freeRunData?.pace.toString())
        val totalDistanceInMeters = (freeRunData?.distance ?: 1.0) * 1000
        val duration = (freeRunData?.durationInMin ?: 1).times(60).toDouble()
        Log.d("totalTimeSeconds", "startProgressTimer: totalTimeSeconds  , duration $duration ")
        progressTimer = object : CountDownTimer(
            ((duration - elapsedProgressTime) * 1000).toLong(), 1000
        ) { // Tick every second
            override fun onTick(millisUntilFinished: Long) {
                elapsedProgressTime = (duration - millisUntilFinished / 1000)

                // Distance covered
                //distanceCovered = (elapsedProgressTime / duration) * totalDistanceInMeters
                distanceCovered = (-70.0) + (elapsedProgressTime / duration) * (totalDistanceInMeters + 70.0)


                val distanceBetween = distanceInMeters - distanceCovered
                Log.d(
                    "distanceCovered",
                    "Distance covered: $distanceCovered m , distanceBetween $distanceBetween"
                )

                binding.tvDistanceAnimal.text = "Dist cover by Animal = ${distanceCovered}m"
                /*if(distanceBetween > 50){
                    stopMediaPlayerBackground()
                }else{
                    playMediaPlayerBackground()
                }*/

                /*when {
                    distanceBetween > 0 -> {
                        Log.d(
                            "distanceBetween",
                            "Distance between runners: $distanceBetween meters"
                        )
                        stopService = true

                    }

                    distanceBetween < 0 -> {
                        Log.d(
                            "distanceBetween",
                            "Distance between runners: $distanceBetween meters"
                        )
                        stopService = false
                    }

                    else -> {
                        Log.d(
                            "distanceBetween",
                            "Both are at the same position: $distanceBetween meters"
                        )
                        stopService = false
                    }
                }*/
                val blinking = if (distanceBetween <= 100) true else false
                animalProgress =
                    ((elapsedProgressTime / duration) * 100).coerceIn(0.0, 100.0).toFloat()
                Log.d("elapsedTime", "elapsedTime: $elapsedProgressTime , $animalProgress")

                val (progress1X, progress2X) = binding.customProgressBar.getProgressXPositions()
                Log.d("ProgressX", "Progress1 X: $progress1X, Progress2 X: $progress2X")

                updateProgress(animalProgress, userProgress, blinking)
                // customProgressBar.setProgress(progress, 100f)

            }

            override fun onFinish() {
                /*updateProgress(
                    animalProgress, userProgress
                )*/
                // Ensure progress is complete at the end

                stageFailed()
            }
        }
        progressTimer!!.start()
    }

    private fun stopProgressTimer() {
        progressTimer.let { it?.cancel() }
    }

    override fun onResume() {
        super.onResume()
        // simulateLocationUpdates(this, LocationManager.GPS_PROVIDER)
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
            val meterInDegrees = 0.0000089 // ~1m in degrees


            val distanceInMeters = 10
            val totalOffset = distanceInMeters * meterInDegrees

            var totalDistance = 0.0

            // 🚀 Wait for 50 seconds before starting movement
            delay(10_000)

            for (i in 0 until durationInSeconds) {
                totalDistance += distanceInMeters

                if (totalDistance <= 70) {
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
                        speed = 1.19f // Simulate 1 m/s
                    }

                    locationManager.setTestProviderLocation(providerName, location)
                }


                delay(1000) // ⏱️ Simulate 1 second per movement
            }

            locationManager.removeTestProvider(providerName)
        }
    }
    ////////////////////////////////////////////////////

}