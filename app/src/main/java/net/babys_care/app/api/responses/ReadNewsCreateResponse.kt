package net.babys_care.app.api.responses

import com.google.gson.annotations.SerializedName

data class ReadNewsCreateResponse(
    val result: Boolean,
    val data : ReadNewsCreateData?
)

data class ReadNewsCreateData(
    val message: String?,
    @SerializedName("read_news_list")
    val readNewsList: List<Int>?
)