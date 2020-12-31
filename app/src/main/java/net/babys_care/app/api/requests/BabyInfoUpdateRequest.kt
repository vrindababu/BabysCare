package net.babys_care.app.api.requests

import net.babys_care.app.models.Children

data class BabyInfoUpdateRequest(
    val api_token: String,
    val children: Children
)