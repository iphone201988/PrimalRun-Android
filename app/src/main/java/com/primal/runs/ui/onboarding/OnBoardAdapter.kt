package com.primal.runs.ui.onboarding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.primal.runs.data.model.OnBoardPage

// OnBoardAdapter.kt
class OnBoardAdapter(
    private val pages: List<OnBoardPage>,
    fragment: FragmentActivity
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        val page = pages[position]
        // Pass the callback to each fragment
        return OnBoardFragment(page.imageResId)
    }
}



