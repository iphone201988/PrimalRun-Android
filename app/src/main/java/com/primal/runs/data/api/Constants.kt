package com.primal.runs.data.api

import com.primal.runs.data.model.PreviousResult

object Constants {
    const val BASE_URL = "http://98.83.65.142:8000"
    const val BASE_URL_IMAGE = "http://69.49.232.235/job_app/api/"
    const val GOOGLE_API_KEY = "AIzaSyD5Jt2e9ocVmXovnsOsdmtdhPRkP8m9IhQ"


    /**************** API LIST *****************/

    const val HEADER_API = "X-API-Key:lkcMuYllSgc3jsFi1gg896mtbPxIBzYkEL"
    const val SOCIAL_LOGIN = "/api/user/socialLogin"
    const val LOGOUT = "/api/user/logoutUser"
    const val ACTIVITIES = "/api/user/getMyActivities"
    const val UPDATE_USER_DATA = "/api/user/updateUserData"
    const val GET_USER_DATA = "/api/user/getUserProfile"
    const val GET_ALL_BADGES = "/api/badge/getAllBadges"

    const val GET_ALL_PLANS = "/api/plan/getAllPlans"
    const val GET_PLAN_BY_ID = "/api/plan/getPlanById/"
    const val STAGE_BY_ID = "/api/stage/getStageById/"
    const val GET_ACHIEVEMENTS = "/api/achievement/getMyAchievements"
    const val SAVE_STAGE_HISTORY = "/api/stage/saveStageHistory"
    const val SAVE_RESULTS = "/api/result/saveResults"
    const val SAVE_RESULT_VIDEO = "/api/result/saveResultVideo"

    var activities: PreviousResult? = null

    //for login process require key
    var latitude = "0.0"
    var longitude = "0.0"
    var userName = ""
    var email = ""
    var socialId = ""
    var zender = ""


}