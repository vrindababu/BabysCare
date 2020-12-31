package net.babys_care.app.api.requests

data class CreateSearchHistoryRequest(
    val api_token: String,
    val word: String
)