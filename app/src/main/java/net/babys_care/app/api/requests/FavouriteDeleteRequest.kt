package net.babys_care.app.api.requests

data class FavouriteDeleteRequest(
    val api_token: String,
    val article_id: Int
)