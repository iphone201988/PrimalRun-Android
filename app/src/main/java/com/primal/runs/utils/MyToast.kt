package com.primal.runs.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.primal.runs.R

object MyToast {
    @ColorInt
    private val DEFAULT_TEXT_COLOR = Color.parseColor("#FFFFFF")

    @ColorInt
    private val ERROR_COLOR = Color.parseColor("#D50000")

    @ColorInt
    private val INFO_COLOR = Color.parseColor("#08aad2")

    @ColorInt
    private val SUCCESS_COLOR = Color.parseColor("#388E3C")

    @ColorInt
    private val WARNING_COLOR = Color.parseColor("#FFA900")

    @ColorInt
    private val NORMAL_COLOR = Color.parseColor("#353A3E")
    private const val tintIcon = true

    @CheckResult
    fun normal(context: Context, message: CharSequence): Toast {
        return normal(context, message, Toast.LENGTH_SHORT, null, false)
    }

    @CheckResult
    fun normal(
        context: Context,
        message: CharSequence,
        icon: Drawable?
    ): Toast {
        return normal(context, message, Toast.LENGTH_SHORT, icon, true)
    }

    @CheckResult
    fun normal(
        context: Context,
        message: CharSequence,
        duration: Int
    ): Toast {
        return normal(context, message, duration, null, false)
    }

    @CheckResult
    fun normal(
        context: Context, message: CharSequence, duration: Int,
        icon: Drawable?
    ): Toast {
        return normal(context, message, duration, icon, true)
    }

    @CheckResult
    fun normal(
        context: Context, message: CharSequence, duration: Int,
        icon: Drawable?, withIcon: Boolean
    ): Toast {
        return custom(
            context,
            message,
            icon,
            NORMAL_COLOR,
            duration,
            withIcon,
            true
        )
    }

    @CheckResult
    fun warning(context: Context, message: CharSequence): Toast {
        return warning(context, message, Toast.LENGTH_SHORT, true)
    }

    @CheckResult
    fun warning(
        context: Context,
        message: CharSequence,
        duration: Int
    ): Toast {
        return warning(context, message, duration, true)
    }

    @CheckResult
    fun warning(
        context: Context,
        message: CharSequence,
        duration: Int,
        withIcon: Boolean
    ): Toast {
        return custom(
            context, message, getDrawable(context, R.drawable.ic_danger),
            WARNING_COLOR, duration, withIcon, true
        )
    }

    @CheckResult
    fun info(context: Context, message: CharSequence): Toast {
        return info(context, message.trim(), Toast.LENGTH_SHORT, true)
    }

    @CheckResult
    fun info(context: Context, message: CharSequence, duration: Int): Toast {
        return info(context, message.trim(), duration, true)
    }

    @CheckResult
    fun info(
        context: Context,
        message: CharSequence,
        duration: Int,
        withIcon: Boolean
    ): Toast {
        return custom(
            context, message, getDrawable(context, R.drawable.ic_info),
            INFO_COLOR, duration, withIcon, true
        )
    }

    @CheckResult
    fun success(context: Context, message: CharSequence): Toast {
        return success(context, message, Toast.LENGTH_SHORT, true)
    }

    @CheckResult
    fun success(
        context: Context,
        message: CharSequence,
        duration: Int
    ): Toast {
        return success(context, message, duration, true)
    }

    @CheckResult
    fun success(
        context: Context,
        message: CharSequence,
        duration: Int,
        withIcon: Boolean
    ): Toast {
        return custom(
            context, message.trim(), getDrawable(context, R.drawable.ic_tick),
            SUCCESS_COLOR, duration, withIcon, true
        )
    }

    @CheckResult
    fun error(context: Context, message: CharSequence): Toast {
        return error(context, message, Toast.LENGTH_SHORT, true)
    }

    @CheckResult
    fun error(
        context: Context,
        message: CharSequence,
        duration: Int
    ): Toast {
        return error(context, message, duration, true)
    }

    @CheckResult
    fun error(
        context: Context,
        message: CharSequence,
        duration: Int,
        withIcon: Boolean
    ): Toast {
        return custom(
            context, message, getDrawable(context, R.drawable.ic_close),
            ERROR_COLOR, duration, withIcon, true
        )
    }

    @CheckResult
    fun custom(
        context: Context, message: CharSequence, icon: Drawable?,
        duration: Int, withIcon: Boolean
    ): Toast {
        return custom(context, message, icon, -1, duration, withIcon, false)
    }

    @CheckResult
    fun custom(
        context: Context, message: CharSequence, @DrawableRes iconRes: Int,
        @ColorInt tintColor: Int, duration: Int,
        withIcon: Boolean, shouldTint: Boolean
    ): Toast {
        return custom(
            context, message, getDrawable(context, iconRes),
            tintColor, duration, withIcon, shouldTint
        )
    }

    @SuppressLint("ShowToast")
    @CheckResult
    fun custom(
        context: Context, message: CharSequence, icon: Drawable?,
        @ColorInt tintColor: Int, duration: Int,
        withIcon: Boolean, shouldTint: Boolean
    ): Toast {
        var icon = icon
        val currentToast = Toast.makeText(context, "", duration)
        val toastLayout =
            (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.toast_layout, null)
        val toastIcon =
            toastLayout.findViewById<ImageView>(R.id.toast_icon)
        val toastTextView = toastLayout.findViewById<TextView>(R.id.toast_text)
        val drawableFrame: Drawable?
        drawableFrame =
            if (shouldTint) tint9PatchDrawableFrame(context, tintColor) else getDrawable(
                context,
                R.drawable.toast_frame
            )
        setBackground(toastLayout, drawableFrame)
        if (withIcon) {
            requireNotNull(icon) { "Avoid passing 'icon' as null if 'withIcon' is set to true" }
            if (tintIcon) icon = tintIcon(icon, DEFAULT_TEXT_COLOR)
            setBackground(toastIcon, icon)
        } else {
            toastIcon.visibility = View.GONE
        }
        toastTextView.text = message
        toastTextView.setTextColor(DEFAULT_TEXT_COLOR)
        currentToast.view = toastLayout
        return currentToast
    }

    private fun setBackground(view: View, drawable: Drawable?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) view.background = drawable
        /* else
            view.setBackgroundDrawable(drawable);*/
    }

    private fun tintIcon(drawable: Drawable, @ColorInt tintColor: Int): Drawable {
        drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        return drawable
    }

    private fun tint9PatchDrawableFrame(
        context: Context,
        @ColorInt tintColor: Int
    ): Drawable {
        val toastDrawable =
            getDrawable(context, R.drawable.toast_frame) as NinePatchDrawable?
        return tintIcon(toastDrawable!!, tintColor)
    }

    private fun getDrawable(context: Context, @DrawableRes id: Int): Drawable? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) context.getDrawable(id) else context.resources
            .getDrawable(id)
    }
}