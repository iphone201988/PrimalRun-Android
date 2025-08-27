package com.primal.runs.ui.dashboard.free_run.model


import com.google.gson.annotations.SerializedName

data class GetAllBadgesModel(
    @SerializedName("data")
    var `data`: ArrayList<BadgesData?>?,
    @SerializedName("success")
    var success: Boolean?,
    @SerializedName("videos")
    var videos: ArrayList<StagesData?>?
)


data class BadgesData(
    @SerializedName("backgroundSound")
    var backgroundSound: String?,
    @SerializedName("badgeImage")
    var badgeImage: String?,
    @SerializedName("badgeType")
    var badgeType: Int?,
    @SerializedName("femaleAttackSound")
    var femaleAttackSound: String?,
    @SerializedName("_id")
    var id: String?,
    @SerializedName("maleAttackSound")
    var maleAttackSound: String?,
    @SerializedName("startSound")
    var startSound: String?,
    @SerializedName("videoUrl")
    var videoUrl: String?,
    var isSelected: Boolean = false
)

data class StagesData(
    @SerializedName("createdAt")
    var createdAt: String?,
    @SerializedName("_id")
    var id: String?,
    @SerializedName("thumbnail")
    var thumbnail: String?,
    @SerializedName("updatedAt")
    var updatedAt: String?,
    @SerializedName("__v")
    var v: Int?,
    @SerializedName("videoLink")
    var videoLink: String?,
    var isSelected: Boolean = false
)