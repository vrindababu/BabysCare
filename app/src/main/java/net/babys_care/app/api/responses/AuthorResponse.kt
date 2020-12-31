package net.babys_care.app.api.responses

import com.google.gson.annotations.SerializedName

data class AuthorResponse(
    val `data`: AuthorData,
    val result: Boolean
)

data class AuthorData(
    val authors: List<Author>
)

data class Author(
    @SerializedName("author_id")
    val authorId: Int,
    @SerializedName("author_wp_id")
    val authorWpId: Int,
    val name: String,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    val description: String?,
    val link: String?
)