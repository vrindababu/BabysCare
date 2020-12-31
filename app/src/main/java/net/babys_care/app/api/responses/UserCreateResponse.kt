package net.babys_care.app.api.responses

import com.google.gson.annotations.SerializedName

data class UserCreateResponse(
    val result: Boolean?,
    val data: CreateUserResponseData?,
    val message: String?,
    val errors: ResponseError?
)

data class CreateUserResponseData (
    @SerializedName("api_token")
    val apiToken: String,
    val message: String,
    @SerializedName("user_id")
    val userId: Int,
    val email: String,
    @SerializedName("user_type")
    val userType: String,
    val status: String,
    @SerializedName("parent_id")
    val parentId: Int,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name_kana")
    val lastNameKana: String,
    @SerializedName("first_name_kana")
    val firstNameKana: String,
    val image: String?,
    var gender: String,
    @SerializedName("birth_day")
    var birthday: String,
    @SerializedName("postal_code")
    var postalCode: String,
    var prefecture: Int,
    var city: String,
    var building: String? = null,
    @SerializedName("is_notifiable_local")
    val isNotifiableLocal: Int,
    @SerializedName("is_notifiable_remote")
    val isNotifiableRemote: Int,
    @SerializedName("is_premama")
    val isPremama: Int,
    val children: List<ChildData>
)

data class ChildData(
    @SerializedName("child_id")
    var childId: Int,
    var last_name: String,
    var first_name: String,
    var last_name_kana: String,
    var first_name_kana: String,
    var image: String?,
    var gender: String,
    var birth_day: String,
    var birth_order: Int,
    var sibling_order: Int
)

data class ResponseError(
    val child: ChildError?,
    val parent: ParentError?,
    val user: UserError?
)

data class ChildError(
    val last_name: String?,
    val first_name: String?,
    val last_name_kana: String?,
    val first_name_kana: String?,
    val image: String?,
    val gender: String?,
    val birth_day: String?,
    val birth_order: Int?,
    val sibling_order: Int?
)

data class ParentError(
    val first_name: String?,
    val last_name: String?,
    val first_name_kana: String?,
    val last_name_kana: String?,
    val gender: String?,
    val birth_day: String?,
    val postal_code: String?,
    val prefecture: Int?,
    val city: String?,
    val building: String?,
    val is_premama: Int?
)

data class UserError(
    val email: String?,
    val password: String?
)