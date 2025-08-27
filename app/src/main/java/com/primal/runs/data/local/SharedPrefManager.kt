package com.primal.runs.data.local

import android.content.SharedPreferences
import com.google.gson.Gson
import com.primal.runs.data.model.LoginUser
import com.primal.runs.data.model.UserUpdate
import com.primal.runs.ui.dashboard.start_run.model.DataStartModule
import com.primal.runs.utils.getValue
import com.primal.runs.utils.saveValue
import javax.inject.Inject

class SharedPrefManager @Inject constructor(private val sharedPreferences: SharedPreferences) {

    object KEY {
        const val USER = "user"
        const val USER_UPDATED = "user_updated"
        const val USER_ID = "user_id"
        const val BEARER_TOKEN = "bearer_token"
        const val PROFILE_COMPLETED = "profile_completed"
        const val APPEARANCE_KEY = "appearance_key"
        const val LOCALE = "locale_key"
        const val TODAY_RECORD = "today_record"
        const val TODAY = "today"
        const val ANS = "ans"
        const val IS_FIRST = "is_first"
        const val IS_FIRST_HOME = "is_first_home"
        const val IS_FIRST_ESTIMATE = "is_first_estimate"
    }

    fun saveIsFirst(isFirst: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY.IS_FIRST, isFirst)
        editor.apply()
    }

    fun getIsFirst(): Boolean? {
        return sharedPreferences.getValue(KEY.IS_FIRST, false)
    }

    fun saveRunData(bean: DataStartModule?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY.IS_FIRST_HOME, Gson().toJson(bean))
        editor.apply()
    }

    fun getRunData(): DataStartModule? {
        val s: String? = sharedPreferences.getString(KEY.IS_FIRST_HOME, null)
        return Gson().fromJson(s, DataStartModule::class.java)
    }


    fun saveUser(bean: LoginUser?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY.USER, Gson().toJson(bean))
        editor.apply()
    }

    fun getCurrentUser(): LoginUser? {
        val s: String? = sharedPreferences.getString(KEY.USER, null)
        return Gson().fromJson(s, LoginUser::class.java)
    }

    fun saveUserIMage(userId: String?) {
        sharedPreferences.saveValue(KEY.USER_ID, userId)
    }

    fun getUserImage(): String? {
        return sharedPreferences.getValue(KEY.USER_ID, null)
    }


    fun saveUserNewData(bean: UserUpdate?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY.USER_UPDATED, Gson().toJson(bean))
        editor.apply()
    }

    fun getCurrentUserNewData(): UserUpdate? {
        val s: String? = sharedPreferences.getString(KEY.USER_UPDATED, null)
        return Gson().fromJson(s, UserUpdate::class.java)
    }


    fun profileCompleted(isProfile: Boolean) {
        sharedPreferences.saveValue(KEY.PROFILE_COMPLETED, isProfile)
    }

    fun isProfileCompleted(): Boolean? {
        return sharedPreferences.getValue(KEY.PROFILE_COMPLETED, false)
    }

    fun setAppearance(type: Int) {
        sharedPreferences.saveValue(KEY.APPEARANCE_KEY, type)
    }

    fun getAppearance(): Int {
        return sharedPreferences.getInt(KEY.APPEARANCE_KEY, 0)
    }

    fun setLocaleType(type: String?) {
        sharedPreferences.saveValue(KEY.LOCALE, type)
    }

    fun getLocaleType(): String? {
        return sharedPreferences.getString(KEY.LOCALE, "en")
    }


    fun getToday(): Int {
        return sharedPreferences.getInt(KEY.TODAY, 0)
    }

    fun setToday(type: Int?) {
        sharedPreferences.saveValue(KEY.TODAY, type)
    }

    fun ansToday(): Int {
        return sharedPreferences.getInt(KEY.ANS, 0)
    }

    fun setAnsToday(type: Int?) {
        sharedPreferences.saveValue(KEY.ANS, type)
    }

    /* fun getToken(): String {
         return getCurrentUser()?.token?.let { token ->
             "Bearer $token"
         }.toString()
     }*/

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}