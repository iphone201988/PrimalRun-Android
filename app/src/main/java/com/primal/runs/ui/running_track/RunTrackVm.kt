package com.primal.runs.ui.running_track

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.primal.runs.data.api.ApiHelper
import com.primal.runs.data.api.Constants.SAVE_RESULT_VIDEO
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.Resource
import com.primal.runs.utils.event.SingleRequestEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class RunTrackVm @Inject constructor(val apiHelper: ApiHelper): BaseViewModel() {
    val commonObserver = SingleRequestEvent<JsonObject>()

    fun sendPreviewApi(
        url: String, request: HashMap<String, RequestBody>?, part: MultipartBody.Part
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            commonObserver.postValue(Resource.loading(null))
            try {
                val response =   apiHelper.apiForMultipartPut(url, request, part)
                Log.d("response", "sendPreviewApi: ${Gson().toJson(response)}")
                if (response.isSuccessful) {
                    commonObserver.postValue(Resource.success(SAVE_RESULT_VIDEO, response.body()))
                } else commonObserver.postValue(
                    Resource.error(
                        handleErrorResponse(response.errorBody()), null
                    )
                )

            } catch (e: Exception) {
                commonObserver.postValue(
                    Resource.error(
                        e.message, null
                    )
                )
            }

        }
    }
}