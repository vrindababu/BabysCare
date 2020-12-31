package net.babys_care.app.api.responses

import net.babys_care.app.api.requests.PasswordRemindEmailData

data class PasswordRecoveryEmailResponse (
    val result: String?,
    val data: PasswordRemindEmailData?
)