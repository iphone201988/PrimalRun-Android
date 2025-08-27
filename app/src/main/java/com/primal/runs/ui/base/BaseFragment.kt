package com.primal.runs.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.primal.runs.BR
import com.primal.runs.data.local.SharedPrefManager
import com.primal.runs.utils.hideKeyboard
import javax.inject.Inject


abstract class BaseFragment<Binding : ViewDataBinding> : Fragment() {
    lateinit var binding: Binding
    lateinit var progressDialogAvl: ProgressDialogAvl
    val parentActivity: BaseActivity<*>?
        get() = activity as? BaseActivity<*>
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCreateView(view)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val layout: Int = getLayoutResource()
        binding = DataBindingUtil.inflate(layoutInflater, layout, container, false)
        binding.setVariable(BR.vm, getViewModel())
        progressDialogAvl = ProgressDialogAvl(parentActivity)
        return binding.root
    }

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    protected abstract fun getLayoutResource(): Int
    protected abstract fun getViewModel(): BaseViewModel
    protected abstract fun onCreateView(view: View)
    override fun onPause() {
        super.onPause()
        activity?.hideKeyboard()
    }

/*    fun showLoading(s: String?) {
        parentActivity?.showLoading(s)
    }

    fun showLoading(s: Int) {
        parentActivity?.showLoading(getString(s))
    }*/


    fun showLoading() {
        progressDialogAvl.isLoading(true)
    }

    fun hideLoading() {
        progressDialogAvl.isLoading(false)
    }

    open fun onBackPressed() {
        findNavController().popBackStack()
    }

}