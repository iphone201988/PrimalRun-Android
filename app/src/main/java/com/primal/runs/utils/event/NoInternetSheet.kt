package com.primal.runs.utils.event

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.primal.runs.R
import com.primal.runs.databinding.SheetNoInternetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class NoInternetSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: SheetNoInternetBinding =
            DataBindingUtil.inflate(inflater, R.layout.sheet_no_internet, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            setupFullHeight(bottomSheetDialog)
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
//        val bottomSheet =
//            bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?
//        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from<FrameLayout?>(bottomSheet!!)
//        val layoutParams = bottomSheet.layoutParams
//        val windowHeight: Int = getWindowHeight()
//        if (layoutParams != null) {
//            layoutParams.height = windowHeight
//        }
//        bottomSheet.layoutParams = layoutParams
//        behavior.state = BottomSheetBehavior.STATE_EXPANDED
//        behavior.isDraggable = false // disable dragging
    }

    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        (requireActivity())!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

}