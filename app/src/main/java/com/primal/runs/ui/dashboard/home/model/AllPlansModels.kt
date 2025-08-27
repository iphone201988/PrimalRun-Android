package com.primal.runs.ui.dashboard.home.model

import com.google.gson.annotations.SerializedName


data class GetAllPlans(
    val data: GetAllPlansData?,
    val success: Boolean?
)

data class GetAllPlansData(
    val groupedData: List<GroupedData>?
)

data class GroupedData(
    val categoryName: String?,
    val data: List<GroupedPlanData>?,
    val maxDistance: Double?,
    val minDistance: Double?,
    var measureUnits: String? = "km",
)

data class GroupedPlanData(
    val _id: String?,
    val category: Category?,
    val description: String?,
    val distancePlan: Double?,
    val image: String?,
    val isPremium: Boolean?,
    val progress: Float?,
    val title: String?,
    @SerializedName("unlockedByDefault")
    var stageLock : Boolean?,
    var measureUnits: String? = "km",
)

data class Category(
    val from: Double?,
    val name: String?,
    val to: Double?,
    val type: Int?
)