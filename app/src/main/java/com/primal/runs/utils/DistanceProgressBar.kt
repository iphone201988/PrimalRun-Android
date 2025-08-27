package com.primal.runs.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.primal.runs.R
import java.math.BigDecimal
import java.math.RoundingMode

class DistanceProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val arcPaint = Paint()
    private val arcBounds = RectF()
    private var progress = 0f  // From 0f to 100f
    private val maxProgress = 21f  // Max progress value
    private var startAngle = 270f  // Start angle at the top
    private var sweepAngle = 360f  // Full-circle arc
    private var thumbAngle = startAngle  // Initial thumb angle

    private var distance: Float? = 1.0f

    var thumbDrawable: Drawable = ContextCompat.getDrawable(context, R.drawable.thumb)!!

    private var progressListener: ProgressListener? = null

    init {
        arcPaint.apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 20f  // Arc thickness
            strokeCap = Paint.Cap.ROUND  // Rounded edges on the arc
        }
    }

    // Interface to communicate the progress back to the Fragment
    interface ProgressListener {
        fun onProgressChanged(progress: Float)
    }

    // Method to set the listener in the fragment
    fun setProgressListener(listener: ProgressListener) {
        this.progressListener = listener
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        val minDimen = Math.min(width, height)
        val padding = 58f
        val reducedSize = minDimen - 2 * padding
        arcBounds.set(
            (width - reducedSize) / 2f,
            (height - reducedSize) / 2f,
            (width + reducedSize) / 2f,
            (height + reducedSize) / 2f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background arc
        arcPaint.color = Color.parseColor("#19191B")
        arcPaint.shader = null
        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, arcPaint)

        // Draw the active progress arc
        arcPaint.color = Color.parseColor("#D2FF22")
        arcPaint.shader = null
        val sweepProgress = (sweepAngle * progress) / maxProgress
        canvas.drawArc(arcBounds, startAngle, sweepProgress, false, arcPaint)

        // Calculate thumb position
        val thumbX = (arcBounds.centerX() + Math.cos(Math.toRadians(thumbAngle.toDouble())) * (arcBounds.width() / 2f)).toFloat()
        val thumbY = (arcBounds.centerY() + Math.sin(Math.toRadians(thumbAngle.toDouble())) * (arcBounds.width() / 2f)).toFloat()

        // Draw the thumb
        val thumbWidth = thumbDrawable.intrinsicWidth.toFloat()
        val thumbHeight = thumbDrawable.intrinsicHeight.toFloat()
        thumbDrawable.setBounds(
            (thumbX - thumbWidth / 2).toInt(),
            (thumbY - thumbHeight / 2).toInt(),
            (thumbX + thumbWidth / 2).toInt(),
            (thumbY + thumbHeight / 2).toInt()
        )
        thumbDrawable.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Calculate touch angle relative to the center
                val x = event.x - arcBounds.centerX()
                val y = event.y - arcBounds.centerY()
                val touchAngle = Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble())).toFloat()

                // Convert touch angle to 0-360 range
                var adjustedAngle = touchAngle + 360f
                if (adjustedAngle > 360f) adjustedAngle -= 360f

                // Normalize the touch angle to align with the progress arc's start and sweep angles
                val relativeAngle = (adjustedAngle - startAngle + 360f) % 360f
                if (relativeAngle <= sweepAngle) { // Only update progress if within the active arc
                    val mappedProgress = (relativeAngle / sweepAngle) * maxProgress
                    setProgress(mappedProgress)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setProgress(progress: Float) {

        this.progress = progress.coerceIn(0f, maxProgress)
        thumbAngle = startAngle + (sweepAngle * this.progress / maxProgress)
        invalidate()
        this.distance = BigDecimal(progress.toDouble()).setScale(1, RoundingMode.HALF_UP).toFloat()
        // Notify the listener about the progress change
        progressListener?.onProgressChanged(BigDecimal(progress.toDouble()).setScale(1, RoundingMode.HALF_UP).toFloat())

        /*val adjustedProgress = progress.coerceIn(0f, maxProgress)
        val displayedProgress = maxOf(1f, adjustedProgress) // force minimum of 1f for visual/progress reporting

        this.progress = adjustedProgress // keep internal value unchanged if needed
        thumbAngle = startAngle + (sweepAngle * adjustedProgress / maxProgress)
        invalidate()
        this.distance = BigDecimal(displayedProgress.toDouble()).setScale(1, RoundingMode.HALF_UP).toFloat()
        progressListener?.onProgressChanged(
            BigDecimal(displayedProgress.toDouble()).setScale(1, RoundingMode.HALF_UP).toFloat()
        )*/

        Log.d("RunProgressBar", "progress ${this.progress} ${ BigDecimal(progress.toDouble()).setScale(1, RoundingMode.HALF_UP).toFloat()} ")  // Debug log
    }
    fun getProgress(): Float {
        Log.d("getProgress", "progress ${this.progress}")
        return progress
    }
    fun getDistance(): Float? {
        return distance
    }
}
