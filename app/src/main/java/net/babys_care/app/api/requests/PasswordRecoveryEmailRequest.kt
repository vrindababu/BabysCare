package net.babys_care.app.api.requests

data class PasswordRecoveryEmailRequest (
    val email: String
)

data class PasswordResetEmailResponse(
    val result: Boolean?,
    val data: PasswordRemindEmailData?
)

data class PasswordRemindEmailData(
    val message: String
)