package com.primal.runs.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.primal.runs.databinding.FragmentOnBoardFirstBinding

// OnBoardFragment.kt
class OnBoardFragment(private val imageResId: Int) : Fragment() {
    private lateinit var imageView: ImageView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentOnBoardFirstBinding.inflate(inflater, container, false)
        imageView = binding.ivMain


        // Set image and text dynamically
        imageView.setImageResource(imageResId)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        imageView.setImageResource(imageResId)
    }
}
