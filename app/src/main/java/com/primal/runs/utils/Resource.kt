package com.primal.runs.utils

import com.google.gson.Gson

data class Resource<out T>(val status: Status, val data: T?, val message: String?) {

    companion object {

        fun <T> success(msg: String?,data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data, msg)
        }

        fun <T> error(msg: String?, data: T?): Resource<T> {
            return Resource(Status.ERROR, data, msg)
        }
        inline fun <reified T> errorBody(msg: okhttp3.ResponseBody?, data: T?): Resource<T> {
            val gson = Gson()
            val rawJson = msg?.string()
            if (isHtmlContent(rawJson)) {
                return Resource(Status.ERROR, null, "HTML content received")
            }
            val parsedData = gson.fromJson(rawJson, T::class.java)
            return Resource(Status.ERROR, parsedData, rawJson)
        }

        fun isHtmlContent(data: String?): Boolean {
            return data?.contains("<html>") == true || data?.contains("<!DOCTYPE html>") == true
        }

        fun <T> loading(data: T?): Resource<T> {
            return Resource(Status.LOADING, data, null)
        }

    }

}