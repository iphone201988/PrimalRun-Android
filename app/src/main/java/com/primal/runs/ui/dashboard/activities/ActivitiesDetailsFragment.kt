package com.primal.runs.ui.dashboard.activities

import android.annotation.SuppressLint
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.databinding.FragmentActivitiesDetailsBinding
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.ImageUtils.formatDate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivitiesDetailsFragment : BaseFragment<FragmentActivitiesDetailsBinding>() {
    private val viewModel: ActivitiesVM by viewModels()
    private var player: ExoPlayer? = null
    override fun getLayoutResource(): Int {
        return R.layout.fragment_activities_details
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(view: View) {
        initOnClick()
        if (Constants.activities != null) {
            Constants.activities?.apply {
                binding.tvDate.text = formatDate(createdAt.toString())
                binding.ratingBar.rating = score?.toFloat() ?: 0.0f
                binding.tvDistanceValue.text = distance.toString() + " km"
                binding.tvMinValue.text = duration.toString() + " min"
                binding.tvAveragePaceValue.text = averageSpeed.toString() + " km"
            }

        }

        player = ExoPlayer.Builder(requireActivity()).build()
        binding.playerView.player = player
        if (!Constants.activities?.videoLink.isNullOrEmpty()) {
            val mediaItem = MediaItem.fromUri(Constants.activities?.videoLink!!)
            // Prepare the player with the media item
            player!!.setMediaItem(mediaItem)
            player!!.prepare()
            player!!.playWhenReady = false
            // player?.play()
        }

        player!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_READY -> {
                        binding.ivThumb.visibility = View.GONE
                    }

                    Player.STATE_ENDED -> {

                    }
                }
            }
        })

    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer {
            when (it?.id) {
                R.id.tvDone -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onStop() {
        player?.release()
        player = null
        super.onStop()

    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()

    }

}