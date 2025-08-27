package com.primal.runs.ui.auth.gps

import com.google.gson.JsonObject
import com.primal.runs.data.api.ApiHelper
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
class GpsEnableVM @Inject constructor(private val apiHelper: ApiHelper) : BaseViewModel() {

    val observeCommon = SingleRequestEvent<JsonObject>()

    fun updateUserData(request: HashMap<String, RequestBody>, url: String, part: MultipartBody.Part?) {
        CoroutineScope(Dispatchers.IO).launch {
            observeCommon.postValue(Resource.loading(null))
            try {
                apiHelper.apiForMultipartPut(url,request,part).let {
                    if (it.isSuccessful) {
                        observeCommon.postValue(Resource.success("UPDATE", it.body()))
                    } else
                        if (it.code() == 401)
                            observeCommon.postValue(Resource.error("Unauthorized", null))
                        else
                            observeCommon.postValue(Resource.error(handleErrorResponse(it.errorBody()), null))
                }
            } catch (e: Exception) {
                observeCommon.postValue(
                    Resource.error(
                        e.message, null
                    )
                )
            }

        }
    }
}