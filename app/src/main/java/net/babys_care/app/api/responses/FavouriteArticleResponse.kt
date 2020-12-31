package net.babys_care.app.api.responses

import java.util.*

data class FavouriteArticleResponse(
    val `data`: FavouriteData,
    val result: Boolean
)

data class FavouriteData(
    val favorites: List<FavouriteArticle>,
    val message: String?
)

data class FavouriteArticle(
    val article_id: Int,
    val created_at: Date
)