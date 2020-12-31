package net.babys_care.app.api.responses

import com.google.gson.annotations.SerializedName

data class GrowthCreateResponse(
    val result: Boolean,
    val data: GrowthCreateData?
)

data class GrowthCreateData(
    val growth_histories: List<GrowthHistory>?,
    val message: String?
)

data class GrowthHistory(
    @SerializedName("child_id")
    var childId: Int,
    @SerializedName("measured_at")
    var measuredAt: String,
    var height: Float,
    var weight: Float
)