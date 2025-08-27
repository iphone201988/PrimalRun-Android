package com.primal.runs.ui.base


import android.view.View
import androidx.lifecycle.ViewModel
import com.primal.runs.utils.event.SingleActionEvent
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject

open class BaseViewModel : ViewModel() {

    val onClick: SingleActionEvent<View?> = SingleActionEvent()

    override fun onCleared() {
        super.onCleared()
    }

    open fun onClick(view: View?) {
        onClick.value = view
    }



    fun handleErrorResponse(errorBody: ResponseBody?): String {
        if (errorBody != null) {
            try {
                val errorString = errorBody.string()
                val jsonObject = JSONObject(errorString)
                if (jsonObject.has("message")) {
                    val errMsg = jsonObject.getString("message")
                    return errMsg
                }
            } catch (e: JSONException) {
                return "Something went wrong1"
                // Handle the JSON parsing exception
                e.printStackTrace()
            }
        }
        return "Something went wrong"
    }
}