package net.babys_care.app.api.responses

data class TokenResponse(
    val result: Boolean?,
    val data: TokenData?,
    val message: String?
)

data class TokenData(
    val message: String?,
    val api_token: String?
)