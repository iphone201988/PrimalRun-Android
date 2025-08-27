package com.primal.runs.ui.dashboard.home_details

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.primal.runs.BR
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.databinding.FragmentHomePageDetailsBinding
import com.primal.runs.databinding.ItemActivitiesDetailsBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.SimpleRecyclerViewAdapter
import com.primal.runs.ui.dashboard.home.HomePlansFragment
import com.primal.runs.ui.dashboard.home.HomePlansFragment.Companion.planId
import com.primal.runs.ui.dashboard.home_details.model.EasyStage
import com.primal.runs.ui.dashboard.home_details.model.PlanDetailsModel
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.ImageUtils.measure
import com.primal.runs.utils.Status
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomePageDetailsFragment : BaseFragment<FragmentHomePageDetailsBinding>() {
    private val viewModel: HomePlansDetailsVM by viewModels()
    private var planDetailListEasy = ArrayList<EasyStage>()
    private var planDetailListNormal = ArrayList<EasyStage>()
    private var planDetailListHard = ArrayList<EasyStage>()
    private var difficultyLevel = 1 ///0 for easy ///1 for normal ////2 for hard

    private var units  = "km"

    private lateinit var adapterDetails: SimpleRecyclerViewAdapter<EasyStage, ItemActivitiesDetailsBinding>
    override fun getLayoutResource(): Int {
        return R.layout.fragment_home_page_details
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        val planId = HomePlansFragment.planId
        Log.i("xx", "$planId")
        if (planId != null) {
            viewModel.getPlansById(Constants.GET_PLAN_BY_ID + planId)
        }
        units   = if(sharedPrefManager.getCurrentUser()?.unitOfMeasure == 1){
            "km"
        }else{
            "miles"
        }
        initOnClick()
        initObserver()
        initGaolsAdapter()
    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer {
            when (it?.id) {
                R.id.ivBack -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }

                R.id.tvEasy -> {
                    adapterDetails.list = planDetailListEasy
                    adapterDetails.notifyDataSetChanged()
                    handleClicks(1)
                }

                R.id.tvNormal -> {
                    adapterDetails.list = planDetailListNormal
                    adapterDetails.notifyDataSetChanged()
                    handleClicks(2)
                }

                R.id.tvHard -> {
                    adapterDetails.list = planDetailListHard
                    adapterDetails.notifyDataSetChanged()
                    handleClicks(3)
                }
            }
        })

    }

    private fun initGaolsAdapter() {
        adapterDetails = SimpleRecyclerViewAdapter(R.layout.item_activities_details, BR.bean) { v, m, pos ->
                when (v.id) {
                    R.id.mainCons -> {
                        val bundle = Bundle()
                        bundle.putString("stage_id", m._id.toString())
                        //bundle.putString("planId", binding.bean?._id)
                        bundle.putString("planId", planId)
                        bundle.putString("type", difficultyLevel.toString())
                        bundle.putBoolean("locked", m.stageLock ?: false)
                        bundle.putInt("difficultyLevel", m.difficultyLevel ?: 1)
                        findNavController().navigate(R.id.fragmentStartRun, bundle)
                    }
                }
            }

        binding.rvActivities.adapter = adapterDetails

    }

    private fun initObserver() {
        viewModel.homePlanDetailObserver.observe(viewLifecycleOwner) {
            when (it!!.status) {
                Status.LOADING -> {
                    showLoading()
                }

                Status.SUCCESS -> {
                    hideLoading()
                    when (it.message) {
                        "PLAN_BY_ID" -> {
                            try {

                                val myDataModel: PlanDetailsModel? =
                                    ImageUtils.parseJson(it.data.toString())

                                Log.d("PLAN_BY_ID", "getPlansById: ${myDataModel?.data}")

                                if (myDataModel != null) {
                                    if (myDataModel.data != null) {
                                        Log.d("PLAN_BY_ID", "getPlansById: ${Gson().toJson(myDataModel.data)}")
                                        /*planDetailListEasy =
                                            myDataModel.data.easyStages as ArrayList<EasyStage>
                                        planDetailListNormal =
                                            myDataModel.data.normalStages as ArrayList<EasyStage>
                                        planDetailListHard =
                                            myDataModel.data.hardStages as ArrayList<EasyStage>
                                        binding.bean = myDataModel.data*/

                                        planDetailListEasy = myDataModel.data.easyStages?.map { it?.copy(difficultyLevel = 1, measureUnits = units) } as ArrayList<EasyStage>
                                        planDetailListNormal = myDataModel.data.normalStages?.map { it?.copy(difficultyLevel = 2,  measureUnits = units) } as ArrayList<EasyStage>
                                        planDetailListHard = myDataModel.data.hardStages?.map { it?.copy(difficultyLevel = 3,  measureUnits = units) } as ArrayList<EasyStage>


                                        adapterDetails.list = planDetailListEasy
                                        adapterDetails.notifyDataSetChanged()
                                        binding.seekbarAudioEffect.progress = myDataModel.data.progress ?: 0
                                        binding.tvProgressValue.text = "${myDataModel.data.progress ?: 0}%"
                                        binding.tvPlan.text = "${myDataModel.data.distancePlan} ${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)} plan"
                                        handleClicks(myDataModel.data.stageType ?: 1)
                                    }
                                    else {
                                        showToast("No Data Found")
                                    }
                                } else {
                                    showToast("No Data Found")
                                }

                            } catch (e: Exception) {
                                Log.d("exception", "initObserver: ${e.message}")
                            }
                        }
                    }
                }

                Status.ERROR -> {
                    showToast(it.message.toString())
                    hideLoading()
                    if (it.message.equals("Unauthorized")) {
                        sharedPrefManager.clear()
                        startActivity(Intent(requireContext(), WelcomeActivity::class.java))
                        requireActivity().finish()
                    }
                }
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun handleClicks(clicked: Int) {
        difficultyLevel = 1
        when (clicked) {
            1 -> {
                binding.tvEasy.setBackgroundResource(R.drawable.corner_radius_setting)
                binding.tvEasy.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.tvNormal.background = null
                binding.tvNormal.setTextColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.white
                    )
                )
                binding.tvHard.background = null
                binding.tvHard.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            }
            2 -> {
                binding.tvNormal.setBackgroundResource(R.drawable.corner_radius_setting)
                binding.tvNormal.setTextColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.black
                    )
                )
                binding.tvEasy.background = null
                binding.tvEasy.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.tvHard.background = null
                binding.tvHard.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            3 -> {
                binding.tvHard.setBackgroundResource(R.drawable.corner_radius_setting)
                binding.tvHard.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.tvEasy.background = null
                binding.tvEasy.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.tvNormal.background = null
                binding.tvNormal.setTextColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.white
                    )
                )
            }
        }

    }

}