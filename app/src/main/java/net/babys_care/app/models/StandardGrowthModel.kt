package net.babys_care.app.models

/**
 * Created by Vrinda R Babu on 12 November, 2020.
 * Package net.babys_care.app.models
 * Project BabysCare
 */
data class StandardGrowthModel(
    val general_growth_value: List<GeneralGrowthValue>
)

data class GeneralGrowthValue(
    val id: Int,
    val standard_growth_value: List<StandardGrowthValue>,
    val gender: String
)

data class StandardGrowthValue(
    val age_group: String,
    val height: Height,
    val weight: Weight
)

data class Height(
    val avg: Double,
    val max: Double,
    val min: Double
)

data class Weight(
    val avg: Double,
    val max: Double,
    val min: Double
)