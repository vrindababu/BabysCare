package net.babys_care.app.api.requests

import net.babys_care.app.models.Parent
import net.babys_care.app.models.User

data class UserInfoUpdateRequest(
    val api_token: String,
    val parent: Parent,
    val user: User
)