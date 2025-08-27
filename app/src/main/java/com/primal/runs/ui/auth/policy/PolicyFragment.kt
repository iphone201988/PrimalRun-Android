package com.primal.runs.ui.auth.policy

import android.view.View
import androidx.fragment.app.viewModels
import com.primal.runs.R
import com.primal.runs.databinding.FragmentPolicyBinding
import com.primal.runs.ui.auth.login.LoginVM
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PolicyFragment : BaseFragment<FragmentPolicyBinding>() {
    private val viewModel: LoginVM by viewModels()

    override fun getLayoutResource(): Int {
        return R.layout.fragment_policy
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initOnClick()
    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner) {
            when (it?.id) {
                R.id.ivCancel, R.id.tvGotIt -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

}