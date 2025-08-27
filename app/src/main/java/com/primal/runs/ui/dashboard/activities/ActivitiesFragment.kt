package com.primal.runs.ui.dashboard.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.data.model.GetActivities
import com.primal.runs.databinding.FragmentActivitiesBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.common.CommonPageActivity
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.ImageUtils.categorizeResultsByMonth
import com.primal.runs.utils.ImageUtils.formatDuration
import com.primal.runs.utils.ImageUtils.generateInitialsBitmap
import com.primal.runs.utils.ImageUtils.prepareDisplayItemsInOrder
import com.primal.runs.utils.Status
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivitiesFragment : BaseFragment<FragmentActivitiesBinding>() {
    private val viewModel: ActivitiesVM by viewModels()

    override fun getLayoutResource(): Int {
        return R.layout.fragment_activities
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initOnClick()
        initObserver()
        viewModel.getActivities(Constants.ACTIVITIES)

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


    @SuppressLint("SetTextI18n")
    private fun initObserver() {
        viewModel.observeCommon.observe(viewLifecycleOwner) {
            when (it!!.status) {
                Status.LOADING -> {
                    showLoading()
                }

                Status.SUCCESS -> {
                    hideLoading()
                    when (it.message) {
                        "GET_ACTIVITIES" -> {
                            try {

                                val myDataModel: GetActivities? =
                                    ImageUtils.parseJson(it.data.toString())
                                if (myDataModel != null) {
                                    binding.tvValueKm.setText(myDataModel.data?.totalDistance.toString() + " KM")
                                    binding.tvValueActivities.setText(myDataModel.data?.activities.toString())
                                    binding.tvValueLength.setText(formatDuration(myDataModel.data?.totalDuration!!))
                                    val categorizedResults =
                                        categorizeResultsByMonth(myDataModel.data.previousResults!!) // List of activities
                                    val displayItems =
                                        prepareDisplayItemsInOrder(categorizedResults)

                                    val myAdapter = MyAdapter { activityData ->
                                        Constants.activities = activityData
                                        val intent = Intent(requireContext(), CommonPageActivity::class.java)
                                        intent.putExtra("From", "ActivitiesFragment")
                                        startActivity(intent)
                                    }
                                    binding.rvThisMonths.adapter = myAdapter
                                    myAdapter.submitList(displayItems)


                                }
                            } catch (e: Exception) {

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