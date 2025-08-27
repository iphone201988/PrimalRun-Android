package com.primal.runs.ui.dashboard.home

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.databinding.FragmentHomePlansBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.common.CommonPageActivity
import com.primal.runs.ui.dashboard.home.model.GetAllPlans
import com.primal.runs.ui.dashboard.home.model.GroupedData
import com.primal.runs.ui.guide.GuideActivity
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.ImageUtils.generateInitialsBitmap
import com.primal.runs.utils.Status
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomePlansFragment : BaseFragment<FragmentHomePlansBinding>() {
    private val viewModel: HomePlansVM by viewModels()
    private var planList = ArrayList<GroupedData>()
    private lateinit var adapter: HomePlanAdapter
    private var units  = "km"

    companion object {
        var planId: String? = null
    }

    override fun getLayoutResource(): Int {
        return R.layout.fragment_home_plans
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initView()
        initObserver()
        adapter = HomePlanAdapter(context = requireContext(),
            list = planList,
            units = units,
            clickListener = object : HomePlanAdapter.OnPlanClickListener {
                override fun onPlanClicked(position: Int, plan: String) {
                    if (plan == "LastPlan") {
                        val intent = Intent(requireContext(), GuideActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    else {
                        planId = plan
                        Log.i("planId", "$plan")
                        val intent = Intent(requireContext(), CommonPageActivity::class.java)
                        intent.putExtra("From", "Plans")
                        startActivity(intent)
                    }
                }
            })
        binding.rvPlans.adapter = adapter




        initOnClick()
        binding.bean = sharedPrefManager.getUserImage()

    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer {
            when (it?.id) {
                R.id.ivUserImage -> {
                    val intent = Intent(requireContext(), CommonPageActivity::class.java)
                    intent.putExtra("From", "Profile")
                    startActivity(intent)
                }

            }
        })
    }

    private fun initView() {
        units   = if(sharedPrefManager.getCurrentUser()?.unitOfMeasure == 1){
            "km"
        }else{
            "miles"
        }
        viewModel.getAllPlans(Constants.GET_ALL_PLANS)
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
                        "ALL_PLANS" -> {
                            try {
                                val myDataModel: GetAllPlans? =
                                    ImageUtils.parseJson(it.data.toString())
                                Log.d("AllPlansData", "initObserver: ${Gson().toJson(myDataModel?.data)}")
                                if (myDataModel != null) {
                                    if (myDataModel.data != null) {
                                        planList =
                                            (myDataModel.data.groupedData?.map { it.copy(measureUnits = units)  } )as ArrayList<GroupedData>

                                        planList.add(
                                            GroupedData(
                                                null, null, null, null, units
                                            )
                                        )
                                        adapter.list = planList
                                        binding.rvPlans.adapter?.notifyDataSetChanged()
                                    } else {
                                        showToast("No Data Found")
                                    }

                                } else {
                                }


                            } catch (e: Exception) {

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

    override fun onResume() {
        super.onResume()
        Log.d("getUserImage", "onResume: ${sharedPrefManager.getCurrentUser()?.profileImage.toString()} ")
        val profileImage  = sharedPrefManager.getCurrentUser()?.profileImage.toString()

        if(profileImage != null){
            val data1 = profileImage.replace("https://primalrunbucket.s3.us-east-1.amazonaws.com/", "")

            if(data1.isEmpty() || data1.equals("null")){
                val initialsBitmap = generateInitialsBitmap(sharedPrefManager.getCurrentUser()?.name.toString())
                binding.ivUserImage.setImageBitmap(initialsBitmap)
            }else{
                Glide.with(requireActivity())
                    .load(profileImage)
                    .placeholder(R.drawable.iv_dummy) // Placeholder while loading
                    .into(  binding.ivUserImage)
            }
        }

    }

}