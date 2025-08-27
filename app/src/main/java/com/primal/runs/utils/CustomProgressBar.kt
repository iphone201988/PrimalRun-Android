package com.primal.runs.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.primal.runs.R

class CustomProgressBar  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.app_gray) // Background color
        style = Paint.Style.FILL
    }

    private val progressPaint1 = Paint().apply {
        color = ContextCompat.getColor(context, R.color.bg_panel_color) // First Progress Color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var progressPaint2 = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorAccent) // Second Progress Color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var progress1 = 0f // First progress value (e.g., completed progress)
    private var progress2 = 0f // Second progress value (e.g., total progress)
    private val cornerRadius = 40f // Adjust this for rounded progress

    fun setProgress(progress1: Float?, progress2: Float) {
        //this.progress1 = progress1.coerceIn(0f, 100f)
        if(progress1 != null){
            this.progress1  = progress1.coerceIn(0f, 100f)
        }
        /*if(lessDistance){
            this.progressPaint2 = Paint().apply {
                color = ContextCompat.getColor(context, R.color.red) // Second Progress Color
                style = Paint.Style.FILL
                isAntiAlias = true
            }
        }*/
        this.progress2 = progress2.coerceIn(0f, 100f)
        invalidate() // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw background without rounded edges
       // canvas.drawRect(0f, 0f, width, height, backgroundPaint)
        canvas.drawRoundRect(0f, 0f, width, height, cornerRadius, cornerRadius, backgroundPaint)

        // Draw second progress (Total progress, Green) with rounded corners
        val progress2Width = (progress2 / 100) * width
        canvas.drawRoundRect(0f, 0f, progress2Width, height, cornerRadius, cornerRadius, progressPaint2)

        // Draw first progress (Completed progress, Blue) with rounded corners
        val progress1Width = (progress1 / 100) * width
        canvas.drawRoundRect(0f, 0f, progress1Width, height, cornerRadius, cornerRadius, progressPaint1)
    }

    fun getProgressXPositions(): Pair<Float, Float> {
        val screenPosition = IntArray(2)
        getLocationOnScreen(screenPosition) // Get absolute position of the view
        val screenX = screenPosition[0].toFloat()


        val progress1X =  ((progress1 / 100) * width) - 10
        val progress2X =   ((progress2 / 100) * width) - 10


        return Pair(progress1X, progress2X)

    }

    private var isBlinking = false
    private var blinkAnimator: ValueAnimator? = null
    private var originalColor = progressPaint2.color
    private var blinkColor = ContextCompat.getColor(context, R.color.red) // red color for blinking

    fun setBlinkingProgress2(isBlinking: Boolean) {
        if (this.isBlinking == isBlinking) return
        this.isBlinking = isBlinking

        blinkAnimator?.cancel()

        if (isBlinking) {
            blinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    val fraction = it.animatedFraction
                    progressPaint2.color = if (fraction < 0.5f) blinkColor else originalColor
                    invalidate()
                }
                start()
            }
        } else {
            progressPaint2.color = originalColor
            invalidate()
        }
    }
}