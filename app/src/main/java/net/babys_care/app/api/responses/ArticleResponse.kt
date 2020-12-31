package net.babys_care.app.api.responses

import com.google.gson.annotations.SerializedName

data class ArticleResponse(
    val `data`: ArticleData
)

data class ArticleData(
    val articles: List<Article>
)

data class Article(
    @SerializedName("article_id")
    val articleId: Int,
    @SerializedName("article_wp_id")
    val articleWpId: Int,
    val author: Int,
    val content: String,
    val view: Int,
    val date: String,
    @SerializedName("date_gmt")
    val dateGmt: String,
    val link: String,
    @SerializedName("source_url")
    val sourceUrl: String,
    val status: String,
    @SerializedName("tag_id")
    val tagIds: List<Int>,
    val title: String,
    val type: String,
    var isFavourite: Boolean = false //Local variable to track favourite flag
)