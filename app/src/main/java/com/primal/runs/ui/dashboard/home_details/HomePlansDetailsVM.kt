package com.primal.runs.ui.dashboard.home_details

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.primal.runs.data.api.ApiHelper
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.utils.Resource
import com.primal.runs.utils.event.SingleRequestEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomePlansDetailsVM @Inject constructor(val apiHelper: ApiHelper): BaseViewModel() {
    val homePlanDetailObserver = SingleRequestEvent<JsonObject>()
    fun getPlansById(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            homePlanDetailObserver.postValue(Resource.loading(null))
            try {
                apiHelper.apiGetOnlyAuthToken(url).let {
                    if (it.isSuccessful) {

                        homePlanDetailObserver.postValue(Resource.success("PLAN_BY_ID", it.body()))
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
}