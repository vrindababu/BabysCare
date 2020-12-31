package net.babys_care.app.api.requests

data class CreateBrowseHistoryRequest(
    val api_token: String,
    val article_id: Int
)