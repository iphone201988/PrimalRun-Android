package com.primal.runs.ui.auth.gender

import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.databinding.FragmentSelectGenderBinding
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectGenderFragment : BaseFragment<FragmentSelectGenderBinding>() {
    private val viewModel: SelectGenderVM by viewModels()

    override fun getLayoutResource(): Int {
        return R.layout.fragment_select_gender
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initView()
        initOnClick()
    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner) {
            when (it?.id) {
                R.id.tvNext -> {
                    findNavController().navigate(R.id.navigateToGpsFragment)
                }

                R.id.ivMain -> {

                    Constants.zender = "1"
                    binding.ivMain.setImageResource(R.drawable.iv_men_gender)
                    binding.ivFemale.setImageResource(R.drawable.iv_female_gender)
                }

                R.id.ivFemale -> {
                    Constants.zender = "2"
                    binding.ivFemale.setImageResource(R.drawable.iv_selected_female)
                    binding.ivMain.setImageResource(R.drawable.unslect_man_zander)

                }
            }
        }

    }

    private fun initView() {

    }

}