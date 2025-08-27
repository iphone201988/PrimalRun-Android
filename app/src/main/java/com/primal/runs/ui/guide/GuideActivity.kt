package com.primal.runs.ui.guide

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.primal.runs.R
import com.primal.runs.data.model.OnBoardPage
import com.primal.runs.databinding.ActivityGuideBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.dashboard.DashBoardActivity
import com.primal.runs.utils.ImageUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuideActivity : BaseActivity<ActivityGuideBinding>() {
    private val viewModel: GuideActivityVM by viewModels()
    private lateinit var viewPager: ViewPager2

    override fun getLayoutResource(): Int {
        return R.layout.activity_guide
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView() {
        ImageUtils.statusBarStyleWhite(this)
        ImageUtils.styleSystemBars(this, getColor(R.color.black))
        initOnClick()
        viewPager = binding.viewPager
        viewPager.isUserInputEnabled = true

        val pages = listOf(
            OnBoardPage(R.drawable.iv_guide1),
            OnBoardPage(R.drawable.guide2),
            OnBoardPage(R.drawable.iv_guide3),
            OnBoardPage(R.drawable.iv_guide4),
            OnBoardPage(R.drawable.iv_guide5),
            OnBoardPage(R.drawable.iv_guide6),
            OnBoardPage(R.drawable.iv_guide7)
        )
        val adapter = OnGuideAdapter(pages, this)

        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        binding.tvHeading.setText(getString(R.string.heading_guide1))
                        binding.tvTerms.setText(getString(R.string.des_guide1))
                        binding.ivThreeDot.setImageResource(R.drawable.dot_guide1)
                        binding.ivNext.visibility = View.VISIBLE
                        binding.tvLetsGet.visibility = View.GONE
                        binding.ivLogo.visibility = View.VISIBLE

                    }

                    1 -> {
                        binding.apply {
                            ivLogo.visibility = View.GONE
                            tvHeading.setText(getString(R.string.heading_guide2))
                            tvTerms.setText(getString(R.string.des_guide2))
                            ivThreeDot.setImageResource(R.drawable.dot_guide2)
                            ivNext.visibility = View.VISIBLE
                            tvLetsGet.visibility = View.GONE

                        }


                    }

                    2 -> {
                        binding.apply {
                            ivLogo.visibility = View.GONE
                            tvHeading.setText(getString(R.string.heading_guide3))
                            tvTerms.setText(getString(R.string.des_guide3))
                            ivThreeDot.setImageResource(R.drawable.dot_guide3)
                            ivNext.visibility = View.VISIBLE
                            tvLetsGet.visibility = View.GONE

                        }
                    }

                    3 -> {
                        binding.apply {
                            ivLogo.visibility = View.GONE
                            tvHeading.setText(getString(R.string.heading_guide4))
                            tvTerms.setText(getString(R.string.des_guide4))
                            ivThreeDot.setImageResource(R.drawable.dot_guide4)
                            ivNext.visibility = View.VISIBLE
                            tvLetsGet.visibility = View.GONE

                        }
                    }

                    4 -> {
                        binding.apply {
                            ivLogo.visibility = View.GONE
                            tvHeading.setText(getString(R.string.heading_guide5))
                            tvTerms.setText(getString(R.string.des_guide5))
                            ivThreeDot.setImageResource(R.drawable.dot_guide5)
                            ivNext.visibility = View.VISIBLE
                            tvLetsGet.visibility = View.GONE

                        }
                    }

                    5 -> {
                        binding.apply {
                            ivLogo.visibility = View.GONE
                            tvHeading.setText(getString(R.string.heading_guide6))
                            tvTerms.setText(getString(R.string.des_guide6))
                            ivThreeDot.setImageResource(R.drawable.dot_guide6)
                            ivNext.visibility = View.VISIBLE
                            tvLetsGet.visibility = View.GONE

                        }
                    }

                    6 -> {
                        binding.apply {
                            ivLogo.visibility = View.GONE
                            tvHeading.setText(getString(R.string.heading_guide7))
                            tvTerms.setText(getString(R.string.des_guide7))
                            ivThreeDot.setImageResource(R.drawable.dot_guide7)
                            ivNext.visibility = View.GONE
                            tvLetsGet.visibility = View.VISIBLE

                        }
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

                R.id.tvLetsGet , R.id.ivCancel-> {
                    startActivity(Intent(this, DashBoardActivity::class.java))
                    finish()
                }
            }
        }
    }



}