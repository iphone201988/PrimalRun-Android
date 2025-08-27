package com.primal.runs.ui.dashboard.free_run

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.gms.ads.LoadAdError
import com.primal.runs.BR
import com.primal.runs.BuildConfig
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.data.model.FreeRunModel
import com.primal.runs.databinding.FragmentFreeRunBinding
import com.primal.runs.databinding.ItemFreeRunBinding
import com.primal.runs.databinding.ItemFreeRunStagesBinding
import com.primal.runs.databinding.ProDialogBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.SimpleRecyclerViewAdapter
import com.primal.runs.ui.base.permission.PermissionHandler
import com.primal.runs.ui.base.permission.Permissions
import com.primal.runs.ui.common.CommonPageActivity
import com.primal.runs.ui.dashboard.free_run.model.BadgesData
import com.primal.runs.ui.dashboard.free_run.model.GetAllBadgesModel
import com.primal.runs.ui.dashboard.free_run.model.StagesData
import com.primal.runs.ui.running_track.FreeRunNewActivity
import com.primal.runs.ui.running_track.FreeRunNewActivity.Companion.freeRunData
import com.primal.runs.utils.AdManager
import com.primal.runs.utils.BaseCustomDialog
import com.primal.runs.utils.DistanceProgressBar
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.ImageUtils.generateInitialsBitmap
import com.primal.runs.utils.ImageUtils.measure
import com.primal.runs.utils.ImageUtils.paceToSpeed
import com.primal.runs.utils.ImageUtils.unitsOfMeasure
import com.primal.runs.utils.InterstitialAdManager
import com.primal.runs.utils.RunProgressBar
import com.primal.runs.utils.Status
import com.primal.runs.utils.showErrorToast
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class FreeRunFragment : BaseFragment<FragmentFreeRunBinding>() {

    private val viewModel: FreeRunVm by viewModels()

    private lateinit var adapterAchievement: SimpleRecyclerViewAdapter<BadgesData, ItemFreeRunBinding>
    private lateinit var adapterStages: SimpleRecyclerViewAdapter<StagesData, ItemFreeRunStagesBinding>
    private lateinit var receiptDialog: BaseCustomDialog<ProDialogBinding>
    private lateinit var interstitialAdManager: InterstitialAdManager

    private var badgesList :ArrayList<BadgesData?>? = ArrayList()
    private var stagesData  :ArrayList<StagesData?>? = ArrayList()
    var budgeType = 1
    var attackSound = ""
    var backgroundSound = ""
    var startSound = ""

    override fun getLayoutResource(): Int {
        return R.layout.fragment_free_run
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initOnClick()
        initView()
        initAchievementAdapter()
        receiptDialog()
        initObserver()
      //  loadAds()
        binding.bean = sharedPrefManager.getUserImage()
        viewModel.getAllBadges(Constants.GET_ALL_BADGES)

        binding.arcProgress.setProgress(7.00f)
        binding.tvVersion.setText("v" + BuildConfig.VERSION_NAME)

    }

    private fun loadAds() {
        binding.adManager.loadAd(object : AdManager.AdManagerCallback {
            override fun onAdLoaded() {
                binding.consAds.visibility = View.VISIBLE
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                binding.consAds.visibility = View.GONE
            }

            override fun onAdClicked() {

            }
        })
    }

    private fun loadInter() {
        interstitialAdManager.loadAd(callback = object :
            InterstitialAdManager.InterstitialAdCallback {
            override fun onAdLoaded() {
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                goRunActivity()
            }

            override fun onAdClosed() {
                goRunActivity()
            }

            override fun onAdShowed() {
            }

            override fun onAdClicked() {
            }
        })
    }

    @SuppressLint("DefaultLocale")
    fun goRunActivity() {

        try {
            //val progress = String.format(Locale.US, "%.2f", binding.arcProgress.getProgress()).toDouble()
            //val progress = String.format(Locale.US, "%.2f", binding.arcProgress.getProgress()).toDouble()
            binding.arcProgress.getProgress()
            binding.arcProgress.getFormattedTime()
            val miles = if((sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1) == 1) false else true
            val speed = paceToSpeed(binding.arcProgress.getMin(), binding.arcProgress.getSec(), miles)
            val duration = calculateTimeRequired(binding.arcDisProgress.getDistance()!!.toDouble(),
                binding.arcProgress.getMin(),
                binding.arcProgress.getSec())


            val selectedStage = adapterAchievement.list.find { it.isSelected == true }
           /* budgeType = adapterAchievement.list[pos].badgeType!!
            attackSound = adapterAchievement.list[pos].maleAttackSound ?: ""
            backgroundSound = adapterAchievement.list[pos].backgroundSound ?: ""
            startSound = adapterAchievement.list[pos].startSound ?: ""*/

            Log.d("speed", "goRunActivity: speed=> $speed ")
            //val progress = paceToSpeed(1, 0)
            val request = FreeRunModel(
                null,
                null,
                null,
                binding.arcDisProgress.getDistance()!!.toDouble(),
                duration,
                sharedPrefManager.getCurrentUser()?.gender ?: 1,
                speed = speed,
                pace = binding.arcProgress.getFormattedTime(),
                selectedStage?.badgeType ?: 1,
                if(sharedPrefManager.getCurrentUser()?.gender == 1)selectedStage?.maleAttackSound else selectedStage?.femaleAttackSound,
                selectedStage?.backgroundSound,
                true,
                selectedStage?.startSound
            )

            Log.d("FreeRunNewActivity.freeRunData", "request: => $request ")
            freeRunData = request


            /*val intent = Intent(requireContext(), FreeRunNewActivity::class.java).apply {
                putExtra("videoUrl", adapterStages.list.find { it.isSelected == true }?.videoLink)
                //putExtra("stage_Thumbnail", adapterStages.list.find { it.isSelected == true }?.thumbnail)
            }
            startActivity(intent)*/
            checkLocation()
        }
        catch (e: Exception) {
            showToast(e.message.toString())
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
                    val intent = Intent(requireContext(), FreeRunNewActivity::class.java).apply {
                        putExtra("videoUrl", adapterStages.list.find { it.isSelected == true }?.videoLink)
                    }
                    startActivity(intent)

                }

                override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                    super.onDenied(context, deniedPermissions)
                    showErrorToast("Please Enable location")

                }
            })

    }

    private fun initView() {
        //binding.arcProgress.setProgress(40f)
    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer {
            when (it?.id) {
                R.id.tvStartRun -> {
                    if (adapterAchievement.list.find { it.isSelected }?.isSelected == true) {
                        if(adapterStages.list.find { it.isSelected }?.isSelected == true){
                            receiptDialog.show()
                        }else{
                            showToast("Please select stage")
                        }
                    }
                    else {
                        showToast("pls select Pursuer")
                    }
                }

                R.id.ivUserImage -> {
                    val intent = Intent(requireContext(), CommonPageActivity::class.java)
                    intent.putExtra("From", "Profile")
                    startActivity(intent)

                }
            }
        })
        binding.arcProgress.setProgressListener(object : RunProgressBar.ProgressListener {
            override fun onProgressChanged(progress: String) {
                binding.progressValue.setText("${progress}${unitsOfMeasure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}")
            }

        } )
        binding.arcDisProgress.setProgressListener(object : DistanceProgressBar.ProgressListener {
            override fun onProgressChanged(progress: Float) {
                binding.arcDistanceValue.setText("${progress}${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}")
            }
        })

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
                        "GET_ALL_BADGES" -> {
                            try {
                                val myDataModel: GetAllBadgesModel? =
                                    ImageUtils.parseJson(it.data.toString())
                                if (myDataModel != null) {
                                    //badgesList = myDataModel.data
                                    badgesList = (myDataModel.data ?: arrayListOf()).mapIndexed { index, badge ->
                                        badge?.apply { isSelected = index == 0 }
                                    } as ArrayList<BadgesData?>
                                    //stagesData = myDataModel.videos

                                    stagesData = (myDataModel.videos ?: arrayListOf()).mapIndexed { index, badge ->
                                        badge?.apply { isSelected = index == 0 }
                                    } as ArrayList<StagesData?>

                                    adapterAchievement.list = badgesList
                                    adapterAchievement.notifyDataSetChanged()

                                    adapterStages.list =stagesData
                                    adapterStages.notifyDataSetChanged()
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
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


    private fun receiptDialog() {
        receiptDialog = BaseCustomDialog<ProDialogBinding>(
            requireContext(), R.layout.pro_dialog
        ) {
            when (it?.id) {
                R.id.tvGoPro -> {
                    val intent = Intent(requireContext(), CommonPageActivity::class.java)
                    intent.putExtra("From", "GoPro")
                    startActivity(intent)
                    receiptDialog.dismiss()
                }

                R.id.tvMayBeLater -> {/*    if (interstitialAdManager.isAdReady()) {
                            interstitialAdManager.showAd()
                        } else {
                            showToast("Ad is not ready yet")
                            goRunActivity()
                        }*/

                    goRunActivity()
                    receiptDialog.dismiss()
                }
            }
        }
        receiptDialog.create()
        receiptDialog.setCancelable(true)
    }

    /*private fun paceToSpeed(paceMinutes: Int, paceSeconds: Int): Double {
        val paceInSecondsPerKm = (paceMinutes * 60) + paceSeconds
        return 1000.0 / paceInSecondsPerKm // Speed in m/s
    }*/

    /*private fun paceToSpeed(
        paceMinutes: Int,
        paceSeconds: Int,
        isPacePerMile: Boolean = false
    ): Double {
        val paceInSeconds = (paceMinutes * 60) + paceSeconds
        val distanceInMeters = if (isPacePerMile) 1609.34 else 1000.0
        return distanceInMeters / paceInSeconds // Speed in m/s
    }*/

    private fun calculateTimeRequired(
        distanceKm: Double,
        paceMinutes: Int,
        paceSeconds: Int
    ): Int {
        val paceInMinutes = paceMinutes + (paceSeconds / 60.0)
        val headStartDistanceKm = 70.0 / 1000
        val totalTime = (distanceKm + headStartDistanceKm) * paceInMinutes
        Log.d("calculateTimeRequired", "calculateTimeRequired: $totalTime")
        return totalTime.toInt()
    }

    private fun initAchievementAdapter() {
        adapterAchievement =
            SimpleRecyclerViewAdapter(R.layout.item_free_run, BR.bean) { v, m, pos ->
                when (v.id) {
                    R.id.ivUserImage -> {
                        try {
                            adapterAchievement.list.find { it.isSelected }?.isSelected = false
                            adapterAchievement.list[pos].isSelected = true
                            /*budgeType = adapterAchievement.list[pos].badgeType!!
                            attackSound = adapterAchievement.list[pos].maleAttackSound ?: ""
                            backgroundSound = adapterAchievement.list[pos].backgroundSound ?: ""
                            startSound = adapterAchievement.list[pos].startSound ?: ""*/
                            adapterAchievement.notifyDataSetChanged()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        binding.rvPursuer.adapter = adapterAchievement

        adapterStages =
            SimpleRecyclerViewAdapter(R.layout.item_free_run_stages, BR.bean) { v, m, pos ->
                when (v.id) {
                    R.id.ivStage -> {
                        try {
                            adapterStages.list.find { it.isSelected }?.isSelected = false
                            adapterStages.list[pos].isSelected = true
                            adapterStages.notifyDataSetChanged()

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        binding.rvStages.adapter = adapterStages
    }

    override fun onResume() {
        super.onResume()

        interstitialAdManager = InterstitialAdManager(requireActivity())
       // loadInter()

        binding.arcProgress.setProgress(7.00f)
        binding.arcDisProgress.setProgress(5.0f)
        binding.progressValue.setText("7:00${unitsOfMeasure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}")
        binding.arcDistanceValue.setText("5.0${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}")
        /*binding.arcProgress.setProgress(14.00f)

        binding.arcDisProgress.setProgress(0.8f)
        binding.progressValue.setText("14:00${unitsOfMeasure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}")
        binding.arcDistanceValue.setText("0.8${measure(sharedPrefManager.getCurrentUser()?.unitOfMeasure ?: 1)}")*/

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