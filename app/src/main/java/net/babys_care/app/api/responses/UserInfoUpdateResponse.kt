package net.babys_care.app.api.responses

data class UserInfoUpdateResponse(
    val `data`: UserData,
    val result: Boolean,
    val errors: ResponseError?
)