package com.primal.runs.ui.auth.login

import android.app.Activity
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.primal.runs.R
import com.primal.runs.data.api.Constants
import com.primal.runs.data.model.LoginResponseAPI
import com.primal.runs.databinding.FragmentLoginBinding
import com.primal.runs.ui.auth.WelcomeActivity
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.dashboard.DashBoardActivity
import com.primal.runs.utils.ImageUtils
import com.primal.runs.utils.Status
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    private val viewModel: LoginVM by viewModels()
    private var mCurrentLocation: Location? = null
    private var profileUrl: String? = null

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun getLayoutResource(): Int {
        return R.layout.fragment_login
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initView()
        initOnClick()
        initObserver()
    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner) {
            when (it?.id) {
                R.id.btnLogin -> {
                    //  findNavController().navigate(R.id.navigateToGenderFragment)
                    signIn()
                }

                R.id.tvTerms -> {
                    findNavController().navigate(R.id.navigateToPolicyFragment)
                }
            }
        }
    }

    private fun initView() {
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun signIn() {
        mGoogleSignInClient.signOut()
        val signInIntent = mGoogleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            }
            Log.d("resultGoogle", ": $result")
        }


    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            updateUI(account)
        } catch (e: ApiException) {
            showToast(e.message.toString())
            //updateUI(null)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        account?.let {
            profileUrl = account.photoUrl.toString()

            Log.d("profile_url", "updateUI: ${profileUrl}")

            Constants.userName = it.displayName.toString()
            Constants.email = it.email.toString()
            Constants.socialId = it.id.toString()
            val data = HashMap<String, Any>()
            data["socialId"] = Constants.socialId
            data["email"] = Constants.email
            data["name"] = Constants.userName
            // data["gender"] = Constants.zender.toInt() // MALE: 1, FEMALE: 2, OTHER: 3
            data["socialType"] = 1
            data["lat"] = Constants.latitude.toDouble()
            data["lng"] = Constants.longitude.toDouble()
            data["deviceToken"] = "efsdfsfefefef"
            data["deviceType"] = 1
            data["profileImage"] = account.photoUrl.toString()

            viewModel.socialLogin(data, Constants.SOCIAL_LOGIN)


        }
    }


    private fun     initObserver() {
        viewModel.observeCommon.observe(viewLifecycleOwner) {
            when (it!!.status) {
                Status.LOADING -> {
                    showLoading()
                }

                Status.SUCCESS -> {
                    hideLoading()
                    when (it.message) {
                        "SOCIAL" -> {
                            try {
                                val myDataModel: LoginResponseAPI? =
                                    ImageUtils.parseJson(it.data.toString())
                                if (myDataModel != null) {
                                    sharedPrefManager.saveUser(myDataModel.user)
                                    if (myDataModel.user?.isUserExists == true) {
                                        val intent =
                                            Intent(requireContext(), DashBoardActivity::class.java)
                                        startActivity(intent)
                                        requireActivity().finish()
                                    } else {
                                        findNavController().navigate(R.id.navigateToGenderFragment)
                                    }
                                }

                            } catch (_: Exception) {

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