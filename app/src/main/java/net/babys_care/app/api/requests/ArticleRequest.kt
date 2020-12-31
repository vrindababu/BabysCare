package net.babys_care.app.api.requests

class ArticleRequest(
    val api_token: String,
    val search_text: String? = null,
    val parent: Int? = null,
    val page: Int = 1
)
