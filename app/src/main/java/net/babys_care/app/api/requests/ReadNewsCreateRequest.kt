package net.babys_care.app.api.requests

data class ReadNewsCreateRequest(
    val api_token: String,
    val news_id: Int
)