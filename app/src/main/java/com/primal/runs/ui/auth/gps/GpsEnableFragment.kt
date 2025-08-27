package com.primal.runs.ui.auth.gps

import android.content.Intent
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.databinding.FragmentGpsEnableBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.guide.GuideActivity
import com.primal.runs.utils.ImageUtils.isGpsEnabled
import com.primal.runs.utils.Status
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@AndroidEntryPoint
class GpsEnableFragment : BaseFragment<FragmentGpsEnableBinding>() {
    private val viewModel: GpsEnableVM by viewModels()

    private var wasGpsDisabled = false

    override fun getLayoutResource(): Int {
        return R.layout.fragment_gps_enable
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initView()
        initOnClick()
    }

    private fun initView() {
        initObserver()
    }

    override fun onResume() {
        super.onResume()
        if (wasGpsDisabled) {
            if (isGpsEnabled(requireContext())) {
                apiRequestBody()
            }
        }
    }


    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner) {
            when (it?.id) {
                R.id.tvNotNow -> {
                    apiRequestBody()
                }

                R.id.tvSureThing -> {
                    //showToast("work in progress")
                    if (isGpsEnabled(context = requireContext())) {
                        apiRequestBody()
                        wasGpsDisabled = true
                    } else {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        requireContext().startActivity(intent)
                    }

                }
            }
        }

    }


    private fun apiRequestBody() {
        try {
            val data = HashMap<String, RequestBody>()
            data["gender"] = Constants.zender.toRequestBody()// MALE: 1, FEMALE: 2, OTHER: 3
            viewModel.updateUserData(data, Constants.UPDATE_USER_DATA, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun initObserver() {
        viewModel.observeCommon.observe(viewLifecycleOwner) {
            when (it!!.status) {
                Status.LOADING -> {
                    showLoading()
                }

                Status.SUCCESS -> {
                    hideLoading()
                    when (it.message) {
                        "UPDATE" -> {
                            try {
                                val intent = Intent(requireContext(), GuideActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                Status.ERROR -> {
                    showToast(it.message.toString())
                    if (it.message.equals("Unauthorized")) {
                        sharedPrefManager.clear()
                        startActivity(Intent(requireContext(), WelcomeActivity::class.java))
                        requireActivity().finish()
                    }
                    hideLoading()
                }
            }
        }
    }




}