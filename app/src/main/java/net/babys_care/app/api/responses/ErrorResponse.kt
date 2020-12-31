package net.babys_care.app.api.responses

data class ErrorResponse (
    val message: String,
    val code: Int = 500
)