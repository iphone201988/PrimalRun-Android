package com.primal.runs.data.model


data class LoginRequest(
    val email: String, val password: String, val device_type: String, val device_token: String
)

data class Contract(
    val created_at: String?,
    val id: Int?,
    val pdf: String?,
    val signature: String?,
    val updated_at: String?,
    val user_id: Int?
)

data class LoginResponse(
    val answered_count: Int?,
    val certifications: String?,
    val city: String?,
    val contact_number: String?,
    val contract: String?,
    val contracts: Contract?,
    val country: String?,
    val cover_image: String?,
    val created_at: String?,
    val created_by: String?,
    val descriptions: String?,
    val device_token: String?,
    val device_type: String?,
    val dob: String?,
    val email: String?,
    val email_verified_at: String?,
    val gender: String?,
    val headlines: String?,
    val hiring_type_id: Int?,
    val id: Int?,
    val industory_type_id: Int?,
    val is_online: Int?,
    val islogin: Int?,
    val lats: String?,
    val longs: String?,
    val name: String,
    val pincode: String?,
    val profile: String?,
    val resume: String?,
    val role_id: Int?,
    val social_id: String?,
    val state: String?,
    val status: Int?,
    val token: String?,
    val updated_at: String?,
    val website: String?
)


data class CardData(
    val name: String, var isSelected: Boolean = false
)
