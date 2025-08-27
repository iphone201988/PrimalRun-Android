package com.primal.runs.ui.dashboard

import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.viewModels
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.primal.runs.R
import com.primal.runs.databinding.ActivityDashBoardBinding
import com.primal.runs.ui.base.BaseActivity
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.dashboard.activities.ActivitiesFragment
import com.primal.runs.ui.dashboard.free_run.FreeRunFragment
import com.primal.runs.ui.dashboard.home.HomePlansFragment
import com.primal.runs.ui.dashboard.settings.SettingsFragment
import com.primal.runs.utils.AudioPlayerHelper
import com.primal.runs.utils.ImageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashBoardActivity : BaseActivity<ActivityDashBoardBinding>() {

    private val viewModel: DashBoardActivityVM by viewModels()
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun getLayoutResource(): Int {
        return R.layout.activity_dash_board
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView() {

        ImageUtils.statusBarStyleWhite(this)

        window.navigationBarColor = getColor(R.color.black)
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@DashBoardActivity) {

            }
        }
        viewPager = binding.viewPager
        bottomNavigationView = binding.bottomNavigation
        viewPager.isUserInputEnabled = false

       /* val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)*/




        initView()
        initOnClick()
        /*val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)*/
    }

    private fun initOnClick() {

    }

    private fun initView() {
        val fragments = listOf(
            HomePlansFragment(), FreeRunFragment(), ActivitiesFragment(), SettingsFragment()
        )
        val adapter = MyFragmentPagerAdapter(this, fragments)
        viewPager.adapter = adapter
        //bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> viewPager.currentItem = 0
                R.id.run -> viewPager.currentItem = 1
                R.id.activities -> viewPager.currentItem = 2
                R.id.setting -> viewPager.currentItem = 3
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigationView.menu[position].isChecked = true
            }
        })
    }
}