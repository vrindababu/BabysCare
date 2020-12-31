package net.babys_care.app.api.responses

data class ChildCreateResponse(
    val result: Boolean?,
    val data: CreateChildResponseData?,
    val message: String?,
    val errors: ResponseError?
)

data class CreateChildResponseData (
    val userId: Int,
    val email: String,
    val userType: String,
    val status: String,
    val parentId: Int,
    val lastName: String,
    val firstName: String,
    val lastNameKana: String,
    val firstNameKana: String,
    val image: String?,
    var gender: String,
    var birthday: String,
    var postalCode: String,
    var prefecture: Int,
    var city: String,
    var building: String? = null,
    val isNotifiableLocal: Int,
    val isNotifiableRemote: Int,
    val isPremama: Int,
    val children: List<ChildData>
)