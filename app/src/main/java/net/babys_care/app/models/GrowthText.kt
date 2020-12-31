package net.babys_care.app.models

data class GrowthText(
    val age: List<Age>
)

data class Age(
    val info: List<Info>,
    val month: Int
)