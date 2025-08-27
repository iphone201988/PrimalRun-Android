package com.primal.runs.ui.guide

import android.content.Intent
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.primal.runs.R
import com.primal.runs.databinding.FragmentGuide7Binding
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.dashboard.DashBoardActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Guide7Fragment : BaseFragment<FragmentGuide7Binding>() {
    private val viewModel: GuideActivityVM by viewModels()

    override fun getLayoutResource(): Int {
        return R.layout.fragment_guide7
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initView()
        initOnClick()
    }

    private fun initView() {

    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer {
            when (it?.id) {
                R.id.tvLetsGet -> {
                    startActivity(Intent(requireContext(), DashBoardActivity::class.java))
                    requireActivity().finish()
                }
            }
        })

    }


}