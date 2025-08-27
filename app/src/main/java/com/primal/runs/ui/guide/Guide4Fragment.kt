package com.primal.runs.ui.guide

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.primal.runs.R
import com.primal.runs.databinding.FragmentGuide4Binding
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Guide4Fragment : BaseFragment<FragmentGuide4Binding>() {
    private val viewModel: GuideActivityVM by viewModels()

    override fun getLayoutResource(): Int {
        return R.layout.fragment_guide4
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
                R.id.ivNext -> {
                    findNavController().navigate(R.id.navigateToGuide5Fragment)
                }
            }
        })

    }


}