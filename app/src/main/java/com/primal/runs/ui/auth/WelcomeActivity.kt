package com.primal.runs.ui.auth

import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.primal.runs.R
import com.primal.runs.databinding.ActivityWelcomeBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.ImageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {
    private val viewModel: WelcomeActivityVM by viewModels()
    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.mainNavigationHost) as NavHostFragment).navController
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_welcome
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView() {
        ImageUtils.statusBarStyleWhite(this)
        ImageUtils.styleSystemBars(this, getColor(R.color.black))
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                navController.graph =
                    navController.navInflater.inflate(R.navigation.auth_navigation).apply {
                        setStartDestination(R.id.fragmentLogin)
                        //setStartDestination(R.id.fragmentGender)
                    }
            }
        }
    }
}