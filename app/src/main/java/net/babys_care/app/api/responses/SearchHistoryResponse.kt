package net.babys_care.app.api.responses

data class SearchHistoryResponse(
    val `data`: SearchHistoryData,
    val result: Boolean
)

data class SearchHistoryData(
    val search_histories: List<SearchHistory>
)

data class SearchHistory(
    val search_history_id: Int,
    val word: String
)