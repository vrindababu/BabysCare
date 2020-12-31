package net.babys_care.app.api.responses

import net.babys_care.app.models.realmmodels.NewsModel

class NewsListResponse (
    val `data`: NewsList?,
    val result: Boolean
)

data class NewsList (
    val news: List<NewsModel>?
)