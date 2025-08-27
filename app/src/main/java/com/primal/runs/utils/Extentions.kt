package com.primal.runs.utils
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.primal.runs.R
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.edit


/** Network Extensions */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager =
        this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}

fun SharedPreferences.saveValue(key: String, value: Any?) {
    when (value) {
        is String? -> editNdCommit { it.putString(key, value) }
        is Int -> editNdCommit { it.putInt(key, value) }
        is Boolean -> editNdCommit { it.putBoolean(key, value) }
        is Float -> editNdCommit { it.putFloat(key, value) }
        is Long -> editNdCommit { it.putLong(key, value) }
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}

fun <T> SharedPreferences.getValue(key: String, defaultValue: Any? = null): T? {
    return when (defaultValue) {
        is String? -> {
            getString(key, defaultValue as? String) as? T
        }
        is Int -> {
            getInt(key, defaultValue as? Int ?: -1) as? T
        }
        is Boolean -> getBoolean(key, defaultValue as? Boolean ?: false) as? T
        is Float -> getFloat(key, defaultValue as? Float ?: -1f) as? T
        is Long -> getLong(key, defaultValue as? Long ?: -1) as? T
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}

inline fun SharedPreferences.editNdCommit(operation: (SharedPreferences.Editor) -> Unit) {
    this.edit {
        operation(this)
    }
}

fun Activity.hideKeyboard() {
    val manager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    manager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
}



fun Fragment.showSuccessToast(message: String) {
    MyToast.success(this.requireContext(), message, Toast.LENGTH_SHORT, true).show()
}

fun Activity.showSuccessToast(message: String) {
    MyToast.success(this, message, Toast.LENGTH_SHORT, true).show()
}

fun Fragment.showToast(message: String) {
    Toast.makeText(this.context, message, Toast.LENGTH_LONG).show()
}

fun Fragment.showInfoToast(message: String) {
    MyToast.info(this.requireContext(), message, Toast.LENGTH_SHORT, true).show()
}
fun Fragment.showErrorToast(message: String) {
    MyToast.error(this.requireContext(), message, Toast.LENGTH_SHORT, true).show()
}
fun Activity.showErrorToast(message: String) {
    MyToast.error(this, message, Toast.LENGTH_SHORT, true).show()
}

fun Activity.showInfoToast(message: String) {
    MyToast.info(this, message, Toast.LENGTH_SHORT, true).show()
}

fun Fragment.successToast(message: String) {
    if (message.isNotEmpty())
        MyToast.success(this.requireContext(), message, Toast.LENGTH_SHORT, true).show()
}

fun Activity.successToast(message: String) {
    if (message.isNotEmpty())
        MyToast.success(this, message, Toast.LENGTH_SHORT, true).show()
}

fun View.showSnackBar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).also {
        it.view.setBackgroundColor(ContextCompat.getColor(this.context, R.color.colorAccent))
        it.show()
    }
}


