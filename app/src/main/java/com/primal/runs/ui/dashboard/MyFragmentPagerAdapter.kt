package com.primal.runs.ui.dashboard
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/*class MyFragmentPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4 // Update this based on your fragments

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LoginFragment()
            1 -> PolicyFragment()
            2 -> SelectGenderFragment()
            3 -> GpsEnableFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}*/

class MyFragmentPagerAdapter(
    activity: FragmentActivity,
    private val fragments: List<Fragment>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
