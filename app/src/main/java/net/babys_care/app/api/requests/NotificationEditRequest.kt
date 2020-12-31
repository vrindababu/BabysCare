package net.babys_care.app.api.requests

data class NotificationEditRequest(
    val api_token: String,
    val is_notifiable_local: Int,
    val is_notifiable_remote: Int
)