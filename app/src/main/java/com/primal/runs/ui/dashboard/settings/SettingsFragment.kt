package com.primal.runs.ui.dashboard.settings

import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.data.model.LoginResponseAPI
import com.primal.runs.data.model.UserUpdateResponse
import com.primal.runs.databinding.FragmentSettingsBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.common.CommonPageActivity
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.ImageUtils.generateInitialsBitmap
import com.primal.runs.utils.Status
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsVM by viewModels()
    private var gender = 1
    private var measure = 1
    private lateinit var audioManager: AudioManager
    private lateinit var voiceOverManager: VoiceOverAudioManager
    override fun getLayoutResource(): Int {
        return R.layout.fragment_settings
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        audioManager = context?.getSystemService(AUDIO_SERVICE) as AudioManager
        initOnClick()
        initView()

        voiceOverManager = VoiceOverAudioManager(requireContext())
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Set SeekBar max and current progress
        binding.seekbarAudioEffect.max = maxVolume
        binding.seekbarAudioEffect.progress = currentVolume
        Log.i("ewfewg", "onCreateView: "+maxVolume+ "::::"+currentVolume)
        binding.editSeekbarGoalAmount.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Snap progress to 0 or 1
                    val toggleState = if (progress >= 0.5) 1 else 0
                    seekBar?.progress = toggleState

                    if (toggleState == 1) {
                        // Enable voice-over
                        val focusGranted = voiceOverManager.requestAudioFocus()
                        if (focusGranted) {
                            getApiCall("true", "AudioOver") // Notify the server or update settings
                        } else {
                            Log.d("SeekBar", "Failed to gain audio focus.")
                        }
                    } else {
                        // Disable voice-over
                        voiceOverManager.abandonAudioFocus()
                        getApiCall("false", "AudioOver") // Notify the server or update settings
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optional: Handle touch start if needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optional: Handle touch stop if needed
            }
        })







        binding.seekbarAudioEffect.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Set the volume when user changes SeekBar position
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC, progress, 0
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                getApiCall(seekBar?.progress.toString(), "AudioEffects")

            }
        })

    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer {
            when (it?.id) {
                R.id.tvMale -> {
                    gender = 1
                    binding.tvMale.setBackgroundResource(R.drawable.corner_radius_setting)
                    binding.tvFemale.setBackgroundResource(0)
                    binding.tvMale.setTextColor(
                        ContextCompat.getColorStateList(
                            requireContext(), R.color.black
                        )
                    )
                    binding.tvFemale.setTextColor(
                        ContextCompat.getColorStateList(
                            requireContext(), R.color.white
                        )
                    )
                    getApiCall(gender.toString(), "Gender")
                }

                R.id.tvFemale -> {
                    gender = 2
                    binding.tvFemale.setBackgroundResource(R.drawable.corner_radius_setting)
                    binding.tvMale.setBackgroundResource(0)
                    binding.tvFemale.setTextColor(
                        ContextCompat.getColorStateList(
                            requireContext(), R.color.black
                        )
                    )
                    binding.tvMale.setTextColor(
                        ContextCompat.getColorStateList(
                            requireContext(), R.color.white
                        )
                    )

                    getApiCall(gender.toString(), "Gender")
                }

                R.id.viewPolicy -> {
                    val intent = Intent(requireContext(), CommonPageActivity::class.java)
                    intent.putExtra("From", "Setting")
                    startActivity(intent)

                }

                R.id.ivUserImage -> {
                    val intent = Intent(requireContext(), CommonPageActivity::class.java)
                    intent.putExtra("From", "Profile")
                    startActivity(intent)

                }

                R.id.tvMiles -> {
                    measure = 2
                    binding.tvMiles.setBackgroundResource(R.drawable.corner_radius_setting)
                    binding.tvKm.setBackgroundResource(0)
                    binding.tvMiles.setTextColor(
                        ContextCompat.getColorStateList(
                            requireContext(), R.color.black
                        )
                    )
                    binding.tvKm.setTextColor(
                        ContextCompat.getColorStateList(
                            requireContext(), R.color.white
                        )
                    )

                    getApiCall(measure.toString(), "Measure")
                }

                R.id.tvKm -> {
                    measure = 1
                    binding.tvKm.setBackgroundResource(R.drawable.corner_radius_setting)
                    binding.tvMiles.setBackgroundResource(0)
                    binding.tvKm.setTextColor(
                        ContextCompat.getColorStateList(
                            requireContext(), R.color.black
                        )
                    )
                    binding.tvMiles.setTextColor(
                        ContextCompat.getColorStateList(
                            requireContext(), R.color.white
                        )
                    )

                    getApiCall(measure.toString(), "Measure")
                }
            }


        })

    }


    fun getApiCall(string: String, form: String) {
        val requestBody = HashMap<String, RequestBody>()
        when (form) {
            "Measure" -> {
                requestBody["unitOfMeasure"] = string.toRequestBody()
                viewModel.updateUserData(requestBody, Constants.UPDATE_USER_DATA, null)
            }

            "Gender" -> {
                requestBody["gender"] = string.toRequestBody()
                viewModel.updateUserData(requestBody, Constants.UPDATE_USER_DATA, null)
            }

            "AudioOver" -> {
                requestBody["voiceOver"] = string.toRequestBody()
                viewModel.updateUserData(requestBody, Constants.UPDATE_USER_DATA, null)
            }

            "AudioEffects" -> {
                requestBody["audioEffects"] = string.toRequestBody()
                viewModel.updateUserData(requestBody, Constants.UPDATE_USER_DATA, null)
            }
        }

    }


    private fun initView() {
        viewModel.getUserData(Constants.GET_USER_DATA)
        initObserver()
    }

    private fun setMaleAndMeasure(gender: Int, measure: Int) {
        if (gender == 1) {
            binding.tvMale.setBackgroundResource(R.drawable.corner_radius_setting)
            binding.tvFemale.setBackgroundResource(0)
            binding.tvMale.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(), R.color.black
                )
            )
            binding.tvFemale.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(), R.color.white
                )
            )
        } else {
            binding.tvFemale.setBackgroundResource(R.drawable.corner_radius_setting)
            binding.tvMale.setBackgroundResource(0)
            binding.tvFemale.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(), R.color.black
                )
            )
            binding.tvMale.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(), R.color.white
                )
            )

        }


        if (measure == 1) {
            binding.tvKm.setBackgroundResource(R.drawable.corner_radius_setting)
            binding.tvMiles.setBackgroundResource(0)
            binding.tvKm.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(), R.color.black
                )
            )
            binding.tvMiles.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(), R.color.white
                )
            )

        } else {
            binding.tvMiles.setBackgroundResource(R.drawable.corner_radius_setting)
            binding.tvKm.setBackgroundResource(0)
            binding.tvMiles.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(), R.color.black
                )
            )
            binding.tvKm.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(), R.color.white
                )
            )
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
                        "USER_DATA" -> {
                            try {
                                //showToast(it.message)
                                val myDataModel: LoginResponseAPI? =
                                    ImageUtils.parseJson(it.data.toString())
                                if (myDataModel != null) {
                                    setMaleAndMeasure(
                                        myDataModel.user?.gender ?: 1,
                                        myDataModel.user?.unitOfMeasure ?: 1
                                    )
                                    binding.editSeekbarGoalAmount.progress =
                                        if (myDataModel.user?.voiceOver == true) 1 else 0
                                    if (myDataModel.user?.voiceOver == true) {
                                        binding.editSeekbarGoalAmount.progress = 1

                                    } else {
                                        binding.editSeekbarGoalAmount.progress = 0
                                    }
                                    binding.seekbarAudioEffect.progress =
                                        myDataModel.user?.audioEffects ?: 0

                                    sharedPrefManager.saveUserIMage(myDataModel.user?.profileImage)
                                    binding.bean = sharedPrefManager.getUserImage()

                                    //  Log.i("fewf", "initObserver: "+myDataModel.user?.name)

                                    Log.d("sharedUserData", "initObserver: ${Gson().toJson(sharedPrefManager.getCurrentUser())}")
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        "UPDATE" -> {

                            try {
                                showToast(it.message)
                                val myDataModel: UserUpdateResponse? =
                                    ImageUtils.parseJson(it.data.toString())
                                if (myDataModel != null) {
                                    sharedPrefManager.saveUserIMage(myDataModel.user?.profileImage)
                                    sharedPrefManager.saveUserNewData(myDataModel.user)
                                    binding.bean = sharedPrefManager.getUserImage()

                                    val userData  = sharedPrefManager.getCurrentUser()

                                    userData.let {
                                        it?.gender = myDataModel.user?.gender
                                    }

                                    userData.let {
                                        it?.unitOfMeasure = myDataModel.user?.unitOfMeasure
                                    }

                                    sharedPrefManager.saveUser(userData)

                                    Log.d("sharedUserData", "initObserver: ${Gson().toJson(sharedPrefManager.getCurrentUser())}")
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