package net.babys_care.app.api.requests

import net.babys_care.app.models.Parent
import net.babys_care.app.models.User

data class UserCreateRequest (
    val parent: Parent,
    val children: Any,
    val user: User
)