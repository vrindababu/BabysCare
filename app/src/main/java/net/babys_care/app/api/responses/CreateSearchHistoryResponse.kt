package net.babys_care.app.api.responses

data class CreateSearchHistoryResponse(
    val message: String?,
    val search_histories: List<SearchHistory>
)