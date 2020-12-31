package net.babys_care.app.api.responses

import java.util.*

data class BrowsingHistoryResponse(
    val `data`: BrowsingHistory,
    val result: Boolean
)

data class BrowsingHistory(
    val browse_histories: List<BrowseHistory>
)

data class BrowseHistory(
    val article_id: Int,
    val created_at: Date
)