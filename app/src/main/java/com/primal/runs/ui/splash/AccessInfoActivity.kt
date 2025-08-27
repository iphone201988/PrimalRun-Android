package com.primal.runs.ui.splash

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.primal.runs.R
import com.primal.runs.databinding.ActivityAccessInfoBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.ImageUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccessInfoActivity : BaseActivity<ActivityAccessInfoBinding>() {
    private val viewmodel : AccessInfoVm by viewModels()

    override fun getLayoutResource(): Int {
        return R.layout.activity_access_info
    }

    override fun getViewModel(): BaseViewModel {
        return viewmodel
    }

    override fun onCreateView() {
        ImageUtils.statusBarStyleWhite(this)
        ImageUtils.styleSystemBars(this, getColor(R.color.black))
        viewmodel.onClick.observe(this){
            when(it?.id){
                R.id.tvSureThing ->{
                    //sharedPrefManager.saveIsFirst(true)
                    setResult(RESULT_OK)
                    finish()
                }

                R.id.ivCancel ->{
                    finishAffinity()
                }
            }
        }
    }
}