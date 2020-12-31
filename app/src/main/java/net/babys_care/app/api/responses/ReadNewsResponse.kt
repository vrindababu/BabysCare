package net.babys_care.app.api.responses

data class ReadNewsResponse(
    val result: Boolean,
    val data: ReadNewsData?,
    val message: String?
)

data class ReadNewsData(
    val read_news: List<Int>
)