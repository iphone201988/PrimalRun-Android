package com.primal.runs.ui.dashboard.start_run

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.primal.runs.data.api.ApiHelper
import com.primal.runs.data.api.Constants.SAVE_RESULTS
import com.primal.runs.data.api.Constants.SAVE_RESULT_VIDEO
import com.primal.runs.data.api.Constants.SAVE_STAGE_HISTORY
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
class StartRunVM @Inject constructor(val apiHelper: ApiHelper): BaseViewModel() {
    val homePlanDetailObserver = SingleRequestEvent<JsonObject>()

    fun getPlansById(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            homePlanDetailObserver.postValue(Resource.loading(null))
            try {
                apiHelper.apiGetOnlyAuthToken(url).let {
                    if (it.isSuccessful) {
                        homePlanDetailObserver.postValue(Resource.success("STAGE_BY_ID", it.body()))
                    } else
                        if (it.code() == 401)
                            homePlanDetailObserver.postValue(Resource.error("Unauthorized", null))
                        else
                            homePlanDetailObserver.postValue(Resource.error(handleErrorResponse(it.errorBody()), null))
                }
            } catch (e: Exception) {
                homePlanDetailObserver.postValue(
                    Resource.error(
                        e.message, null
                    )
                )
            }

        }
    }

    fun saveStageHistory(url: String, map: HashMap<String, Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            homePlanDetailObserver.postValue(Resource.loading(null))
            try {
                apiHelper.apiPutForRawBody(url, map).let {
                    if (it.isSuccessful) {
                        homePlanDetailObserver.postValue(Resource.success(SAVE_STAGE_HISTORY, it.body()))
                    } else
                        if (it.code() == 401)
                            homePlanDetailObserver.postValue(Resource.error("Unauthorized", null))
                        else
                            homePlanDetailObserver.postValue(Resource.error(handleErrorResponse(it.errorBody()), null))
                }
            } catch (e: Exception) {
                homePlanDetailObserver.postValue(
                    Resource.error(
                        e.message, null
                    )
                )
            }

        }
    }

    fun saveResultApi(url: String, request: HashMap<String, Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            homePlanDetailObserver.postValue(Resource.loading(null))
            try {
                apiHelper.apiPostForRawBody(url,request).let {
                    if (it.isSuccessful) {
                        homePlanDetailObserver.postValue(Resource.success(SAVE_RESULTS, it.body()))
                    } else
                        if (it.code() == 401)
                            homePlanDetailObserver.postValue(Resource.error("Unauthorized", null))
                        else
                            homePlanDetailObserver.postValue(Resource.error(handleErrorResponse(it.errorBody()), null))

                }
            } catch (e: Exception) {
                homePlanDetailObserver.postValue(
                    Resource.error(
                        e.message, null
                    )
                )
            }

        }
    }

    fun sendPreviewApi(
         url: String,request: HashMap<String, RequestBody>?, part: MultipartBody.Part
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            homePlanDetailObserver.postValue(Resource.loading(null))
            try {
                val response =   apiHelper.apiForMultipartPut(url, request, part)
                Log.d("response", "sendPreviewApi: ${Gson().toJson(response)}")
                    if (response.isSuccessful) {
                        homePlanDetailObserver.postValue(Resource.success(SAVE_RESULT_VIDEO, response.body()))
                    } else homePlanDetailObserver.postValue(
                        Resource.error(
                            handleErrorResponse(response.errorBody()), null
                        )
                    )

            } catch (e: Exception) {
                homePlanDetailObserver.postValue(
                    Resource.error(
                        e.message, null
                    )
                )
            }

        }
    }
}