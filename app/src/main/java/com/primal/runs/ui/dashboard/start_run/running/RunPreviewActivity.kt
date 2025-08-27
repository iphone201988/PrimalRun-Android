package com.primal.runs.ui.dashboard.start_run.running

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import com.primal.runs.R
import com.primal.runs.data.api.Constants.SAVE_RESULT_VIDEO
import com.primal.runs.data.model.FreeRunModel
import com.primal.runs.data.model.SaveVideoModel
import com.primal.runs.databinding.ActivityRunPreviewBinding
import com.primal.runs.databinding.CancelDialogBinding
import com.primal.runs.databinding.ProDialogBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.permission.PermissionHandler
import com.primal.runs.ui.base.permission.Permissions
import com.primal.runs.ui.common.CommonPageActivity
import com.primal.runs.ui.dashboard.start_run.StartRunVM
import com.primal.runs.utils.BadgeType
import com.primal.runs.utils.BaseCustomDialog
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.Status
import com.primal.runs.utils.showErrorToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@AndroidEntryPoint
class RunPreviewActivity : BaseActivity<ActivityRunPreviewBinding>(), HBRecorderListener {
    private val viewModel: StartRunVM by viewModels()
    private var lifeCount: Int = 0
    private var gifAnimJob: Job? = null
    private var manAnimator: ValueAnimator? = null
    private var elephantAnimator: ValueAnimator? = null
    private var forwardAnimator: ObjectAnimator? = null
    private var returnAnimator: ObjectAnimator? = null
    private lateinit var hbRecorder: HBRecorder

    private var hasPermissions = false
    private var filePath: String? = null
    private var freeRunData: FreeRunModel? = null
    private var resultId: String? = null
    private var videoPlayer: ExoPlayer? = null
    var mediaPlayer: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var attackRunnable: Runnable? = null

    private lateinit var cancelDialog: BaseCustomDialog<CancelDialogBinding>
    companion object {
        private const val SCREEN_RECORD_REQUEST_CODE = 100
        private const val PERMISSION_REQ_ID_RECORD_AUDIO = 101
        //private const val PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 102
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_run_preview
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView() {
        iniMediaSound()
        initVideoPlayer()
        initView()
        initObserver()
        initOnClick()
        cancelDialog()
    }

    private fun iniMediaSound() {
        mediaPlayer = MediaPlayer().apply {

            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME) // Keeps focus on internal audio
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            try {
                //if (source != null) {
                val assetFileDescriptor = resources.openRawResourceFd(R.raw.chase)
                setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                assetFileDescriptor.close()
                //setDataSource(source)
                /*} else {
                    //showToast("background Sound is null")
                    return
                }*/
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
                setVolume(1f, 1f)
                prepareAsync() // Prepare the media player asynchronously
                isLooping = true
                //playbackParams.audioFallbackMode
            } catch (e: Exception) {
                Log.e("MediaPlayer", "Error initializing media player", e)
            }
        }
    }

    private fun initObserver() {
        viewModel.homePlanDetailObserver.observe(this) {

            when (it!!.status) {
                Status.LOADING -> {
                    showLoading("Loading....")
                }

                Status.SUCCESS -> {
                    hideLoading()
                    when (it.message) {
                        SAVE_RESULT_VIDEO -> {
                            try {

                                val myDataModel: SaveVideoModel? =
                                    ImageUtils.parseJson(it.data.toString())

                                if (myDataModel != null) {

                                    if (myDataModel.success!!) {
                                        showToast(myDataModel.message)
                                        finish()
                                    } else {
                                        showToast(myDataModel.message)
                                        finish()
                                    }
                                }
                                else {
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

    private fun initOnClick() {
        viewModel.onClick.observe(this) {
            when (it?.id) {
                R.id.ivCancel -> {
                    if(hbRecorder.isBusyRecording){
                        hbRecorder.pauseScreenRecording()
                    }
                    else{
                        finish()
                    }
                }
            }
        }
    }

    private fun initView() {
        enableFullScreenMode()
        hbRecorder = HBRecorder(this, this)
        hbRecorder.setVideoEncoder("H264")
        hbRecorder.setAudioSource("DEFAULT") // This ensures internal audio recording only
        hbRecorder.setAudioBitrate(0) // Disables external au

        val timeElapsedInSeconds = intent.getIntExtra("timer", 68)
        lifeCount = intent.getIntExtra("lifeCount", 0)
        resultId = intent.getStringExtra("resultId")


        val hours = timeElapsedInSeconds / 3600
        val minutes = (timeElapsedInSeconds % 3600) / 60
        val seconds = timeElapsedInSeconds % 60

        Log.d(
            "timeElapsedInSeconds",
            "initView: $timeElapsedInSeconds -> hours $hours, minutes $minutes, seconds $seconds"
        )
        startGifAnimation()

        binding.clMain.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.clMain.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // First check if permissions were granted

                val rationale =
                    "We need your location to enhance your experience."
                val options = Permissions.Options()
                Permissions.check(this@RunPreviewActivity,
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO
                    ),
                    rationale,
                    options,
                    object : PermissionHandler() {

                        override fun onGranted() {
                            Handler(Looper.getMainLooper()).postDelayed({

                                if (resultId != null) {
                                    startRecordingScreen()
                                } else {
                                    videoPlayer.let { it?.play() }
                                    mediaPlayer.let { it?.start() }
                                    forwardGifAnim()
                                }
                            }, 3000)

                        }

                        override fun onDenied(
                            context: Context?,
                            deniedPermissions: ArrayList<String>?
                        ) {
                            super.onDenied(context, deniedPermissions)
                            finish()
                        }
                    })

                /*if (checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO,
                        PERMISSION_REQ_ID_RECORD_AUDIO
                    )

                ) {
                    hasPermissions = true
                   // startRecordingScreen()
                    //forwardGifAnim()

                }*/
                /*if (hasPermissions) {
                    startRecordingScreen()
                    forwardGifAnim()
                }*/

            }
        })
    }

    @Suppress("DEPRECATION")
    private fun enableFullScreenMode() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }


    @OptIn(UnstableApi::class)
    private fun initVideoPlayer() {
        videoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView?.player = videoPlayer

        val videoUrl = "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/Stage1.mp4"
        //val videoUrl = "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/1080/Stage1.mp4"
        //val videoUrl = "https://primalrunbucket.s3.us-east-1.amazonaws.com/videos/stageVideos/45fps/Stage1.mp4"
        val uri = Uri.parse(videoUrl)
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

        Glide.with(this).asGif().load(R.drawable.iv_elephant_run).into(binding.ivElephant)
        // Load the appropriate gender GIF
        /* val genderGif =
             if (freeRunData?.gender == 1) R.drawable.iv_man_run else R.drawable.run_women*/
        Glide.with(this).asGif().load(R.drawable.iv_man_run).into(binding.ivMan)

    }

    private fun stopGifAnimation() {
        try {
            // Load the appropriate GIF for gender
            Glide.with(this).asGif().load(R.drawable.iv_man_run)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true).into(object : SimpleTarget<GifDrawable>() {
                    override fun onResourceReady(
                        resource: GifDrawable, transition: Transition<in GifDrawable>?,
                    ) {
                        resource.stop()
                        binding.ivMan.setImageDrawable(resource) // Set the first frame as static image
                    }
                })
            /* val badgeResourceMap = mapOf(
                 BadgeType.ELEPHANT to R.drawable.iv_elephant_run,
                 BadgeType.BULL to R.drawable.iv_run_bull,
                 BadgeType.GORILLA to R.drawable.iv_run_gorilla,
                 BadgeType.BEAR to R.drawable.iv_run_beer,
                 BadgeType.DEER to R.drawable.iv_run_dear,
                 BadgeType.TIGER to R.drawable.iv_run_tiger,
                 BadgeType.RHINO to R.drawable.iv_run_rahino,
                 BadgeType.LION to R.drawable.iv_run_lion,
                 BadgeType.RAPTOR to R.drawable.iv_run_raptor
             )*/
            Glide.with(this).asGif().load(R.drawable.iv_elephant_run)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true).into(object : SimpleTarget<GifDrawable>() {
                    override fun onResourceReady(
                        resource: GifDrawable, transition: Transition<in GifDrawable>?,
                    ) {
                        resource.stop()
                        binding.ivElephant.setImageDrawable(resource) // Set the first frame as static image
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hitGif() {
        binding.apply {

            ivMan.setColorFilter(
                ContextCompat.getColor(
                    this@RunPreviewActivity,
                    android.R.color.holo_red_dark
                ), // Red tint color
                PorterDuff.Mode.SRC_ATOP // Mode to blend the color
            )

        }
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

    private fun forwardGifAnim(duration: Long = 20000) {
        binding.consGetReady?.visibility = View.GONE
        val parentWidth = binding.clMain.width

        if (gifAnimJob != null && gifAnimJob!!.isActive) {
            gifAnimJob!!.cancel()
        }
        gifAnimJob = CoroutineScope(Dispatchers.Main).launch {
            binding.apply {

                // Target positions for the views
                val targetManX = parentWidth.toFloat() - ivMan.width// Adjust margin as needed
                val targetElephantX = parentWidth.toFloat() - ivElephant.width - ivMan.width

                Log.d(
                    "forwardGifAnim",
                    "forwardGifAnim: $targetManX , $targetElephantX , ${ivMan.x}"
                )
                // Animate ivMan

                manAnimator = ValueAnimator.ofFloat(ivElephant.x, targetElephantX)
                manAnimator?.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    ivMan.translationX = value
                }

                manAnimator?.addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationStart(animation: Animator, isReverse: Boolean) {
                        super.onAnimationStart(animation, isReverse)

                        /*Handler(Looper.getMainLooper()).postDelayed({
                            elephantAnimator?.pause()
                            manAnimator?.pause()
                            animateAttackAndReturn(ivElephant, ivMan)
                        }, 4000)*/

                        attackRunnable = Runnable {
                            elephantAnimator?.pause()
                            manAnimator?.pause()
                            animateAttackAndReturn(ivElephant, ivMan)
                        }
                        handler.postDelayed(attackRunnable!!, 4000)
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        stopGifAnimation()
                        elephantAnimator?.pause()
                        if (resultId != null) {
                            hbRecorder.stopScreenRecording()
                        } else {
                            if (videoPlayer != null) {
                                videoPlayer!!.stop()
                                videoPlayer!!.release()
                            }
                            if (mediaPlayer != null) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                            }


                            showToast("Completed")
                            finish()
                        }
                    }
                })
                // Animate ivElephant
                elephantAnimator = ValueAnimator.ofFloat(ivElephant.x, targetElephantX)
                elephantAnimator?.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    ivElephant.translationX = value
                }
                elephantAnimator?.addListener(object : AnimatorListenerAdapter() {

                })

                manAnimator?.duration = duration
                elephantAnimator?.duration = duration
                manAnimator?.start()
                elephantAnimator?.start()
            }
        }
        // Configure and start animations
    }

    private fun cancelDialog() {
        cancelDialog = BaseCustomDialog<CancelDialogBinding>(
            this, R.layout.cancel_dialog
        ) {
            when (it?.id) {
                R.id.tv_done -> {
                    cancelDialog.dismiss()
                    hbRecorder.stopScreenRecording()
                }

                R.id.tv_cancel -> {
                    cancelDialog.dismiss()
                    hbRecorder.resumeScreenRecording()
                }
            }
        }
        cancelDialog.create()
        cancelDialog.setCancelable(false)
    }

    fun animateAttackAndReturn(ivElephant: View, ivMan: View) {
        // Initial and target positions
        val startX = ivElephant.x
        val targetX = ivMan.x - (ivMan.width * 2) // Adjust to simulate "attacking"

        Log.d("animateAttackAndReturn", "animateAttackAndReturn: $startX,  $targetX")

        // Move the elephant forward to attack
        forwardAnimator = ObjectAnimator.ofFloat(ivElephant, "x", startX, targetX).apply {
            duration = 1000L // Duration to reach the man
        }

        // Return the elephant to its initial position
        returnAnimator = ObjectAnimator.ofFloat(ivElephant, "x", targetX, startX).apply {
            duration = 1000L // Duration to return
        }

        // Set up listener for forward animation
        forwardAnimator?.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                lifeCount += 1
                hitGif()
                returnAnimator?.start()
            }
        })

        returnAnimator?.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                unHitGif()
                elephantAnimator?.resume()
                manAnimator?.resume()

                if (lifeCount < 3) {
                    /*Handler(Looper.getMainLooper()).postDelayed({
                        elephantAnimator?.pause()
                        manAnimator?.pause()
                        animateAttackAndReturn(ivElephant, ivMan)
                    }, 4000)*/
                    attackRunnable = Runnable {
                        elephantAnimator?.pause()
                        manAnimator?.pause()
                        animateAttackAndReturn(ivElephant, ivMan)
                    }
                    handler.postDelayed(attackRunnable!!, 4000)
                }

            }
        })

        // Start the forward animation
        forwardAnimator?.start()
    }

    private fun startRecordingScreen() {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (videoPlayer != null) {
            videoPlayer!!.stop()
            videoPlayer!!.release()
        }
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }

        gifAnimJob.let {
            it?.cancel()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_REQUEST_CODE && resultCode == RESULT_OK) {

            videoPlayer.let { it?.play() }
            mediaPlayer.let { it?.start() }

            hbRecorder.startScreenRecording(data, resultCode)
        } else {
            finish()
            //showToast("Recording not found")
        }
        if (requestCode == PERMISSION_REQ_ID_RECORD_AUDIO && resultCode == RESULT_OK) {
            startRecordingScreen()
        }
    }

    override fun HBRecorderOnStart() {

        forwardGifAnim()
    }

    override fun HBRecorderOnComplete() {
        if (videoPlayer != null) {
            videoPlayer!!.stop()
            videoPlayer!!.release()
        }
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }

        showToast("Completed")
        if (hbRecorder.wasUriSet()) {
            Log.d("hbRecorder", "HBRecorderOnComplete: ")
            //updateGalleryUri()
        } else {
            Log.d("hbRecorder", "failed: ")

            MediaScannerConnection.scanFile(
                this,
                arrayOf(hbRecorder.filePath), null
            ) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
                filePath = path
                if (filePath != null) {
                    val file = File(filePath!!)
                    val videoLink = MultipartBody.Part.createFormData(
                        "video",
                        file.name,
                        file.asRequestBody("video/mp4".toMediaType())
                    )
                    val hashMap = HashMap<String, RequestBody>()
                    if (resultId != null) {
                        hashMap["resultId"] = resultId.toString().toRequestBody()
                    } else {
                        hashMap["resultId"] = "6798d852f08e1676066676e0".toRequestBody()
                    }
                    Log.d(
                        "hasRequest",
                        "sendPreviewApi: ${resultId.toString()}, ${
                            resultId.toString().toRequestBody()
                        }"
                    )
                    viewModel.sendPreviewApi(SAVE_RESULT_VIDEO, hashMap, videoLink)

                } else {
                    showToast("Recording not found")
                }
            }
        //refreshGalleryFile()
        }

    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        if (videoPlayer != null) {
            videoPlayer!!.stop()
            videoPlayer!!.release()
        }

        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    override fun HBRecorderOnPause() {
        cancelDialog.show()
        attackRunnable?.let {
            handler.removeCallbacks(it)
        }
        if (mediaPlayer != null) {
            mediaPlayer?.pause()
        }
        if (videoPlayer != null) {
            videoPlayer!!.pause()
        }

        stopGifAnimation()
        returnAnimator?.pause()
        forwardAnimator?.pause()
        manAnimator?.pause()
        elephantAnimator?.pause()

        gifAnimJob.let {
            it?.cancel()
        }

    }

    override fun HBRecorderOnResume() {
        unHitGif()
        cancelDialog.dismiss()
        if (mediaPlayer != null) {
            mediaPlayer?.start()
        }

        if (videoPlayer != null) {
            videoPlayer!!.play()
        }
        startGifAnimation()
        manAnimator.let { it?.resume() }
        elephantAnimator.let { it?.resume() }
        attackRunnable = Runnable {
            elephantAnimator?.pause()
            manAnimator?.pause()
            animateAttackAndReturn(binding.ivElephant, binding.ivMan)
        }
        handler.postDelayed(attackRunnable!!, 4000)
    }
}