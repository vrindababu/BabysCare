package net.babys_care.app.api.responses

data class DeleteSearchHistoryResponse(
    val `data`: SearchHistoryData,
    val result: Boolean
)