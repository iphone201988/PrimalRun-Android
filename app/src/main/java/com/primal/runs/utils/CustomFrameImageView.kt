package com.primal.runs.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.primal.runs.R

class CustomFrameImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val frameDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_frame) // Frame image

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the frame over the image
        frameDrawable?.setBounds(0, 0, width, height)
        frameDrawable?.draw(canvas)
    }
}