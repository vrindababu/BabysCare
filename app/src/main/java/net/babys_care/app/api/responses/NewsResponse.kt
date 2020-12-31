package net.babys_care.app.api.responses

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    val result: Boolean,
    val data: NewsData?
)

data class NewsData(
    val news: List<News>
)

data class News(
    @SerializedName("news_id")
    val newsId: Int,
    val title: String,
    @SerializedName("is_release")
    val isRelease: Int,
    @SerializedName("list_image")
    val listImage: String?,
    @SerializedName("release_end_at")
    val releaseEndAt: String?,
    @SerializedName("release_start_at")
    val releaseStartAt: String
)