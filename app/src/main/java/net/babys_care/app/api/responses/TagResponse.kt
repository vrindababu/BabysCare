package net.babys_care.app.api.responses

import com.google.gson.annotations.SerializedName

data class TagResponse(
    val `data`: TagData,
    val result: Boolean
)

data class TagData(
    val tags: List<Tag>
)

data class Tag(
    val description: String,
    val name: String,
    @SerializedName("tag_id")
    val tagId: Int
)