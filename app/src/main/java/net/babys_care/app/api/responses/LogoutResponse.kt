package net.babys_care.app.api.responses

data class LogoutResponse(
    val `data`: LogoutData,
    val result: Boolean
)

data class LogoutData(
    val message: String?
)