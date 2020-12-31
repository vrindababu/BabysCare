package net.babys_care.app.models

data class TodayWord(
    val month: List<Month>
)

data class Month(
    val info: List<Info>,
    val name: String
)

data class Info(
    val date: Int,
    val text: String
)