package net.babys_care.app.api.responses

data class GrowthHistoryResponse(
    val `data`: Data?,
    val result: Boolean
)

data class Data(
    val growth_histories: List<GrowthHistory>?
)