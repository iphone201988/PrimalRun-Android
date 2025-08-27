package com.primal.runs.ui.common

import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.primal.runs.R
import com.primal.runs.databinding.ActivityCommonPageBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.ImageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommonPageActivity : BaseActivity<ActivityCommonPageBinding>() {
    private val viewModel: CommonActivityVM by viewModels()
    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.mainNavigationHost) as NavHostFragment).navController
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_common_page
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
                    navController.navInflater.inflate(R.navigation.common_navigation).apply {
                        val message = intent.getStringExtra("From")
                        if (message != null) {
                            when (message) {
                                "ActivitiesFragment" -> {
                                    setStartDestination(R.id.fragmentDetails)
                                }

                                "Setting" -> {
                                    setStartDestination(R.id.fragmentPolicy)
                                }

                                "Profile" -> {
                                    setStartDestination(R.id.fragmentProfile)
                                }
                                "GoPro" -> {
                                    setStartDestination(R.id.fragmentPro)
                                }
                                "Plans" -> {
                                    setStartDestination(R.id.fragmentPlanDetails)
                                }
                            }
                        } else {
                            //   setStartDestination(R.id.fragmentProfile)
                        }

                    }
            }
        }
    }

}