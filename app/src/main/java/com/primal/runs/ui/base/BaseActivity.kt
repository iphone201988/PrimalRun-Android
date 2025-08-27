package com.primal.runs.ui.base

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.primal.runs.App
import com.primal.runs.BR
import com.primal.runs.R
import com.primal.runs.data.local.SharedPrefManager
import com.primal.runs.data.network.ErrorCodes
import com.primal.runs.data.network.NetworkError
import com.primal.runs.databinding.ViewProgressSheetBinding
import com.primal.runs.ui.base.connectivity.ConnectivityProvider
import com.primal.runs.utils.AlertManager
import com.primal.runs.utils.event.NoInternetSheet
import com.primal.runs.utils.hideKeyboard
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject

abstract class BaseActivity<Binding : ViewDataBinding> : AppCompatActivity(),
    ConnectivityProvider.ConnectivityStateListener {
    //var db: AppDb? = null
    lateinit var progressDialogAvl: ProgressDialogAvl
    private var progressSheet: ProgressSheet? = null
    open val onRetry: (() -> Unit)? = null


    var onStartCount = 0
    lateinit var binding: Binding
    val app: App
        get() = application as App

    private lateinit var connectivityProvider: ConnectivityProvider
    private var noInternetSheet: NoInternetSheet? = null

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //   db=  Room.databaseBuilder(this, AppDb::class.java, applicationContext.packageName).build()
        val layout: Int = getLayoutResource()
        binding = DataBindingUtil.setContentView(this, layout)
        binding.setVariable(BR.vm, getViewModel())
        connectivityProvider = ConnectivityProvider.createProvider(this)
        connectivityProvider.addListener(this)

        onCreateView()
    }

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    protected abstract fun getLayoutResource(): Int
    protected abstract fun getViewModel(): BaseViewModel
    protected abstract fun onCreateView()

    fun showToast(msg: String? = "Something went wrong !!") {
        Toast.makeText(this, msg ?: "Showed null value !!", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        hideKeyboard()
    }

    fun showLoading(s: String?) {
        progressSheet?.dismissAllowingStateLoss()
        progressSheet = ProgressSheet(object : ProgressSheet.BaseCallback {
            override fun onClick(view: View?) {}
            override fun onBind(bind: ViewProgressSheetBinding) {
                progressSheet?.showMessage(s);
            }
        })
        progressSheet?.isCancelable = false
        progressSheet?.show(supportFragmentManager, progressSheet?.tag)

    }

    fun hideLoading() {
        progressSheet?.dismissAllowingStateLoss()
    }

    fun onError(error: Throwable, showErrorView: Boolean) {
        if (error is NetworkError) {

            when (error.errorCode) {
                ErrorCodes.SESSION_EXPIRED -> {
                    showToast(getString(R.string.session_expired))
                    app.onLogout()
                }

                else -> AlertManager.showNegativeAlert(
                    this,
                    error.message,
                    getString(R.string.alert)
                )
            }
        } else {
            AlertManager.showNegativeAlert(
                this,
                getString(R.string.please_try_again),
                getString(R.string.alert)
            )
        }
    }

    override fun onDestroy() {

        connectivityProvider.removeListener(this)
        super.onDestroy()
    }


    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (supportFragmentManager.isStateSaved) {
            return
        }
        if (noInternetSheet == null) {
            noInternetSheet = NoInternetSheet()
            noInternetSheet?.isCancelable = false
        }
        if (state.hasInternet()) {
            if (noInternetSheet?.isVisible == true)
                noInternetSheet?.dismiss()
        } else {
            if (noInternetSheet?.isVisible == false)
                noInternetSheet?.show(supportFragmentManager, noInternetSheet?.tag)

        }
    }

    private fun ConnectivityProvider.NetworkState.hasInternet(): Boolean {
        return (this as? ConnectivityProvider.NetworkState.ConnectedState)?.hasInternet == true
    }


    fun textToRequestBody(text: String?): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), text!!)
    }

    fun multipartImageBody(image: File): MultipartBody.Part {
        return MultipartBody.Part.createFormData(
            "profile",
//            "profileImage",
            image.name,
            RequestBody.create("image/png".toMediaTypeOrNull(), image)
        )
    }

    fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }


}