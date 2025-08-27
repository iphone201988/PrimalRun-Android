package com.primal.runs.ui.dashboard.free_run

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.primal.runs.data.api.ApiHelper
import com.primal.runs.data.api.Constants
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.Resource
import com.primal.runs.utils.event.SingleRequestEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FreeRunVm @Inject constructor(private val apiHelper: ApiHelper) : BaseViewModel() {
    val observeCommon = SingleRequestEvent<JsonObject>()

    fun getAllBadges(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            observeCommon.postValue(Resource.loading(null))
            try {
                apiHelper.apiGetOnlyAuthToken(url).let {
                    if (it.isSuccessful) {
                        Log.d("GET_ALL_BADGES", "getAllBadges: ${it.body()}")
                        observeCommon.postValue(Resource.success("GET_ALL_BADGES", it.body()))
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