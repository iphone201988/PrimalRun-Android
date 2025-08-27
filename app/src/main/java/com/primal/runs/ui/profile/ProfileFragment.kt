package com.primal.runs.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.primal.runs.BR
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.data.model.GetMyActivities
import com.primal.runs.data.model.GetMyActivitiesData
import com.primal.runs.data.model.LoginResponseAPI
import com.primal.runs.databinding.FragmentProfileBinding
import com.primal.runs.databinding.ItemAchievementBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.SimpleRecyclerViewAdapter
import com.primal.runs.ui.base.permission.PermissionHandler
import com.primal.runs.ui.base.permission.Permissions
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.ImageUtils.generateInitialsBitmap
import com.primal.runs.utils.Status
import com.primal.runs.utils.showErrorToast
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.RequestBody

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {
    private lateinit var adapterAchievement: SimpleRecyclerViewAdapter<GetMyActivitiesData, ItemAchievementBinding>
    private val viewModel: ProfileVm by viewModels()
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    var myFinalList: ArrayList<GetMyActivitiesData> = ArrayList()
    var limitedItems: List<GetMyActivitiesData> = listOf()
    private var isExpanded = false
    override fun getLayoutResource(): Int {
        return R.layout.fragment_profile
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initAchievementAdapter()
        initOnClick()
        initObserver()
        viewModel.getUserData(Constants.GET_USER_DATA)
        viewModel.getUserDataActivities(Constants.GET_ACHIEVEMENTS)
        galleryLauncher()


    }

    private fun galleryLauncher() {
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    val imageUri: Uri? = data?.data
                    if (imageUri != null) {
                        val imageFile =
                            viewModel.convertImageToMultipart(imageUri, requireActivity())
                        val requestBody = HashMap<String, RequestBody>()
                        viewModel.updateUserData(requestBody, Constants.UPDATE_USER_DATA, imageFile)
                        binding.ivUserImage.setImageURI(imageUri)


                    }
                }
            }
    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer {
            when (it?.id) {
                R.id.ivCancel -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    // startActivity(Intent(requireContext(), CommonPageActivity::class.java))
                }

                R.id.tvLogout -> {
                    viewModel.logout(Constants.LOGOUT)

                }

                R.id.tvGoPro -> {
                    findNavController().navigate(R.id.navigateToProFragment)
                }

                R.id.tvRestoreSub -> {

                }

                R.id.ivEdit -> {
                    checkPermission()
                }

                R.id.tvLoadMore -> {
                    val bundle = Bundle()
                    bundle.putSerializable("achievements", myFinalList)
                    findNavController().navigate(R.id.action_fragmentProfile_to_achievementsFragment, bundle)
                    /*if (isExpanded) {
                        // Collapse layout
                        adapterAchievement.list = myFinalList

                        binding.tvLoadMore.text = "Show Less"
                        adapterAchievement.notifyDataSetChanged()
                    } else {
                        // Expand layout
                        adapterAchievement.list = limitedItems
                        adapterAchievement.notifyDataSetChanged()
                        binding.tvLoadMore.text = "Load More"
                    }
                    isExpanded = !isExpanded*/
                }

            }
        })
    }


    private fun initAchievementAdapter() {
        adapterAchievement =
            SimpleRecyclerViewAdapter(R.layout.item_achievement, BR.bean) { v, m, pos ->
                when (v.id) {
                    R.id.ivCancel -> {
                        //  requireActivity().onBackPressedDispatcher.onBackPressed()
                        // startActivity(Intent(requireContext(), CommonPageActivity::class.java))
                    }

                }
            }
        binding.rvAchievements.adapter = adapterAchievement

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
                        "LOGOUT" -> {
                            try {

                                sharedPrefManager.clear()
                                startActivity(Intent(requireContext(), WelcomeActivity::class.java))
                                requireActivity().finish()

                            } catch (_: Exception) {

                            }
                        }

                        "USER_DATA" -> {
                            try {

                                val myDataModel: LoginResponseAPI? =
                                    ImageUtils.parseJson(it.data.toString())
                                if (myDataModel != null) {
                                    binding.bean = myDataModel.user
                                }

                            } catch (_: Exception) {

                            }
                        }

                        "GET_ACTIVITIES" -> {
                            try {

                                val myDataModel: GetMyActivities? =
                                    ImageUtils.parseJson(it.data.toString())
                                if (myDataModel != null) {
                                    myFinalList =
                                        (myDataModel.data as ArrayList<GetMyActivitiesData>?)!!

                                    if (myFinalList.size > 6) {
                                        adapterAchievement.list = myFinalList.take(6)
                                        limitedItems = myFinalList.take(6)
                                        binding.tvLoadMore.visibility = View.VISIBLE
                                    } else {
                                        adapterAchievement.list = myFinalList
                                        binding.tvLoadMore.visibility = View.GONE
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        "UPDATE" -> {
                            try {

                                val myDataModel: LoginResponseAPI? =
                                    ImageUtils.parseJson(it.data.toString())
                                if (myDataModel != null) {
                                    val data = sharedPrefManager.getCurrentUser()
                                    data?.profileImage = myDataModel.user?.profileImage
                                    sharedPrefManager.saveUser(data)

                                    //sharedPrefManager.saveUserIMage(myDataModel.user?.profileImage)
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

    private fun checkPermission() {
        if (!ImageUtils.hasPermissions(requireContext(), ImageUtils.storagePermissions)) {
            //permissionResultLauncher.launch(ImageUtils.storagePermissions)
            checkLocation()
        } else {
            selectImage()
        }
    }

    private fun checkLocation() {
        val rationale = "This app needs access to your storage and camera to function properly."
        val options = Permissions.Options()
        Permissions.check(
            requireContext(),
            ImageUtils.storagePermissions,
            rationale,
            options,
            object : PermissionHandler() {
                override fun onGranted() {
                    selectImage()
                }

                override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                    super.onDenied(context, deniedPermissions)
                   // showErrorToast("Please Enable location")
                }
            })
    }

    private var allGranted = false
    private val permissionResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            for (it in permissions.entries) {
                it.key
                val isGranted = it.value
                allGranted = isGranted
            }
            when {
                allGranted -> {
                    selectImage()
                }

            }
        }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }


    override fun onResume() {
        super.onResume()
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