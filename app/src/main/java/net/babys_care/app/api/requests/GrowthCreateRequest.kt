package net.babys_care.app.api.requests

data class GrowthCreateRequest(
    val api_token: String,
    val child_id: Int,
    val measured_at: String,
    val height: Double,
    val weight: Double
)