package net.babys_care.app.api.requests

data class FavouriteCreateRequest(
    val api_token: String,
    val article_id: Int
)