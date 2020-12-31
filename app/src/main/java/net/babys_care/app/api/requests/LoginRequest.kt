package net.babys_care.app.api.requests

data class LoginRequest (
    val email:String,
    val password: String,
    val fcm_token: String,
    val device_os: String = "Android"
)