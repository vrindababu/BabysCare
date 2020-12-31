package net.babys_care.app.models

import net.babys_care.app.models.realmmodels.GrowthHistories

data class ChildGrowth(
    var childId: Int = 0,
    var lastName: String = "",
    var firstName: String = "",
    var lastNameKana: String = "",
    var firstNameKana: String = "",
    var image: String? = null,
    var gender: String = "",
    var birthDay: String = "",
    var birthOrder: Int = 0,
    var siblingOrder: Int = 0,
    val  growthHistories: MutableList<GrowthHistories> = mutableListOf()
)