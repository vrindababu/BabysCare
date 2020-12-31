package net.babys_care.app.api.responses

data class UserInfoResponse (
    val `data`: UserData,
    val result: Boolean
)

data class UserData(
    val user_id: Int,
    val birth_day: String,
    val building: String,
    val children: List<ChildData>,
    val city: String,
    val email: String,
    val first_name: String,
    val first_name_kana: String,
    val gender: String,
    val image: String?,
    val is_notifiable_local: Int,
    val is_notifiable_remote: Int,
    val is_premama: Int,
    val last_name: String,
    val last_name_kana: String,
    val user_type: String,
    val parent_id: Int,
    val postal_code: String,
    val prefecture: Int,
    val status: String,
    val message: String?
)