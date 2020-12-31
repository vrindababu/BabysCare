package net.babys_care.app.models

import com.google.gson.annotations.SerializedName
import java.util.*

data class News(
    @SerializedName("news_id")
    var news_id: Int = 0,
    var title: String? = null,
    var release_start_at: Date = Date(),
    var release_end_at: Date? = null,
    var is_release: Int = 0,
    var list_image: String? = null,
    var is_read: Int = 0
)