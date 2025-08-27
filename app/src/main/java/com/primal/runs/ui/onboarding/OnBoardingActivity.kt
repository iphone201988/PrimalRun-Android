package com.primal.runs.ui.onboarding

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.primal.runs.R
import com.primal.runs.data.model.OnBoardPage
import com.primal.runs.databinding.ActivityOnBoardingBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.ImageUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnBoardingActivity : BaseActivity<ActivityOnBoardingBinding>() {
    private val viewModel: OnBoardingActivityVM by viewModels()
    private lateinit var viewPager: ViewPager2

    override fun getLayoutResource(): Int {
        return R.layout.activity_on_boarding
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView() {
        initOnClick()
         ImageUtils.statusBarStyleWhite(this)
        ImageUtils.styleSystemBars(this, getColor(R.color.black))
        viewPager = binding.viewPager
        viewPager.isUserInputEnabled = true

        val pages = listOf(
            OnBoardPage(R.drawable.onbaord_first),
            OnBoardPage(R.drawable.iv_onboard2),
            OnBoardPage(R.drawable.iv_onboard3)
        )


        // Initialize the adapter
        val adapter = OnBoardAdapter(pages, this)

        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        binding.tvHeading.setText(getString(R.string.gamify_your_nruns))
                        binding.tvSubHeading.setText(getString(R.string.turn_your_training_into_an_epic_game_nlevel_up_and_win))
                        binding.ivThreeDot.setImageResource(R.drawable.three_dot1)
                        binding.tvLetsGet.visibility = View.GONE
                        binding.ivNext.visibility = View.VISIBLE
                    }

                    1 -> {
                        binding.tvHeading.setText(getString(R.string.on_boarding))
                        binding.tvSubHeading.setText(getString(R.string.des_onboard))
                        binding.ivThreeDot.setImageResource(R.drawable.iv_onbaorddot2)
                        binding.tvLetsGet.visibility = View.GONE
                        binding.ivNext.visibility = View.VISIBLE
                    }

                    2 -> {
                        binding.tvHeading.setText(getString(R.string.on_boarding3))
                        binding.tvSubHeading.setText(getString(R.string.des_onboard3))
                        binding.ivThreeDot.setImageResource(R.drawable.dot3)
                        binding.tvLetsGet.visibility = View.VISIBLE
                        binding.ivNext.visibility = View.GONE
                    }

                }
            }
        })
    }

    private fun initOnClick() {
        viewModel.onClick.observe(this) {
            when (it?.id) {
                R.id.ivNext -> {
                    viewPager.currentItem = viewPager.currentItem + 1
                }

                R.id.tvLetsGet -> {
                    startActivity(Intent(this, WelcomeActivity::class.java))
                    finish()

                }
            }
        }
    }
}