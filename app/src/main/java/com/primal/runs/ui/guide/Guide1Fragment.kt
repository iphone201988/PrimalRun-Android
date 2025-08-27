package com.primal.runs.ui.guide

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.primal.runs.R
import com.primal.runs.databinding.FragmentGuide1Binding
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Guide1Fragment : BaseFragment<FragmentGuide1Binding>() {
    private val viewModel: GuideActivityVM by viewModels()

    override fun getLayoutResource(): Int {
        return R.layout.fragment_guide1
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
            when(it?.id){
                R.id.ivNext->{
               findNavController().navigate(R.id.navigateToGuide2Fragment)
                }
            }
        })

    }


}