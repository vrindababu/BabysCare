package net.babys_care.app.api.responses

class NewsDetailResponse (
    val `data`: NewsDetail?,
    val result: Boolean
)

data class NewsDetail (
    val news_id: Int,
    val title: String,
    val content: String,
    val release_start_at: String,
    val release_end_at: String?,
    val is_release: Int,
    val detail_image: String?,
    val related_article_id: Int?
)