package com.primal.runs.ui.dashboard.start_run

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import com.primal.runs.BR
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.data.api.Constants.SAVE_STAGE_HISTORY
import com.primal.runs.data.model.FreeRunModel
import com.primal.runs.databinding.FragmentStartRunBinding
import com.primal.runs.databinding.ItemPeviousBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.SimpleRecyclerViewAdapter
import com.primal.runs.ui.base.permission.PermissionHandler
import com.primal.runs.ui.base.permission.Permissions
import com.primal.runs.ui.dashboard.start_run.model.DataStartModule
import com.primal.runs.ui.dashboard.start_run.model.PreviousResult
import com.primal.runs.ui.dashboard.start_run.model.StartRunModel
import com.primal.runs.ui.dashboard.start_run.running.StageRunActivity
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.Status
import com.primal.runs.utils.showErrorToast
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StartRunFragment : BaseFragment<FragmentStartRunBinding>() {


    private val viewModel: StartRunVM by viewModels()
    private lateinit var adapterPrevious: SimpleRecyclerViewAdapter<PreviousResult, ItemPeviousBinding>

    private var startRunData: DataStartModule? = null
    private var freeRunData: FreeRunModel? = null
    private var planId: String? = null
    private var type: String? = null
    private var unlocked: Boolean? = false
    private var difficultyLevel: Int? = 1

    private var units = "Km"

    override fun getLayoutResource(): Int {
        return R.layout.fragment_start_run
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        val stageId = arguments?.getString("stage_id")
        planId = arguments?.getString("planId")
        type = arguments?.getString("type")
        unlocked = arguments?.getBoolean("locked", false)
        difficultyLevel = arguments?.getInt("difficultyLevel", 1)

        units = if (sharedPrefManager.getCurrentUser()?.unitOfMeasure == 1) {
            "Km"
        } else {
            "miles"
        }
        initOnClick()
        initObserver()
        initPreviousResult()
        if (stageId != null) {
            viewModel.getPlansById(Constants.STAGE_BY_ID + stageId)
        }
    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner) {
            when (it?.id) {
                R.id.tvStartRun -> {

                    if (unlocked!!) {

                        checkLocation()

                    } else {
                        showToast("Stage is locked")
                    }
                }
                R.id.ivBack -> {
                    onBackPressed()
                }

            }
        }
    }

    private fun checkLocation() {
        val rationale = "We need your location to enhance your experience."
        val options = Permissions.Options()
        Permissions.check(
            requireContext(),
            ImageUtils.permissionsForLocationOny,
            rationale,
            options,
            object : PermissionHandler() {
                override fun onGranted() {
                    val hashMap = HashMap<String, Any>()
                    if (planId != null) {
                        hashMap["planId"] = planId!!
                    }
                    if (type != null) {
                        hashMap["type"] = type!!
                    }
                    viewModel.saveStageHistory(SAVE_STAGE_HISTORY, hashMap)

                    val intent = Intent(requireContext(), StageRunActivity::class.java).apply {
                        putExtra("freeRunData", freeRunData)
                    }
                    startActivity(intent)

                }

                override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                    super.onDenied(context, deniedPermissions)
                    showErrorToast("Please Enable location")

                }
            })

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
                        "STAGE_BY_ID" -> {
                            try {

                                val myDataModel: StartRunModel? =
                                    ImageUtils.parseJson(it.data.toString())

                                if (myDataModel != null) {
                                    if (myDataModel.data != null) {
                                        startRunData = myDataModel.data.copy(measureUnits = units)
                                        Log.d("startRunData", "initObserver:=> $startRunData")
                                        binding.bean = startRunData
                                        sharedPrefManager.saveRunData(startRunData)
                                        /*TrackLocationActivity.completeData = startRunData
                                        TrackLocationActivity.gender =
                                            sharedPrefManager.getCurrentUser()?.gender ?: 1*/
                                        adapterPrevious.list = startRunData!!.previousResults?.map {
                                            it?.copy(measureUnits = units)
                                        }

                                        freeRunData = FreeRunModel(
                                            startRunData?._id,
                                            startRunData?.planId,
                                            startRunData?.badgeId?._id,
                                            startRunData?.distance?.toDouble(),
                                            startRunData?.durationInMin,
                                            sharedPrefManager.getCurrentUser()?.gender ?: 1,
                                            startRunData?.speed,
                                            pace = "",
                                            startRunData?.type
                                                ?: -1,//startRunData.badgeId.budgeType,
                                            //startRunData?.badgeId?.maleAttackSound,
                                            if (sharedPrefManager.getCurrentUser()?.gender == 1) startRunData?.badgeId?.maleAttackSound else startRunData?.badgeId?.femaleAttackSound,
                                            startRunData?.badgeId?.backgroundSound,
                                            true,
                                            startRunData?.badgeId?.startSound,
                                            difficultyLevel
                                        )

                                    } else {
                                        showToast(" No Data Found")
                                    }

                                } else {
                                    Log.d("myDataModel", "initObserver: null ")
                                }


                            } catch (e: Exception) {
                                e.printStackTrace()
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


    private fun initPreviousResult() {
        adapterPrevious = SimpleRecyclerViewAdapter(R.layout.item_pevious, BR.bean) { v, _, _ ->
            when (v.id) {
                R.id.ivUserImage -> {

                }

            }
        }
        binding.rvPrevious.adapter = adapterPrevious

    }

}