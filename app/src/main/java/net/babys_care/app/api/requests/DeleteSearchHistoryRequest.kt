package net.babys_care.app.api.requests

data class DeleteSearchHistoryRequest(
    val api_token: String,
    val search_history_id: Int,
    val delete_all: Int = 0// If 1 is set, then all history will be deleted
)