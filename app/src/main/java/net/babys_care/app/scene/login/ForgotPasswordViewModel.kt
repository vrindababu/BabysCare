package net.babys_care.app.scene.login

import jp.winas.android.foundation.scene.BaseViewModel
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.PasswordRecoveryEmailRequest
import net.babys_care.app.api.requests.PasswordResetEmailResponse
import net.babys_care.app.utils.debugLogInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordViewModel : BaseViewModel() {

    var emailAddress: String = ""

    fun requestPasswordReset(
        email: String,
        onResponse: ((PasswordResetEmailResponse?, String?) -> Unit)? = null
    ) {
        ApiClientManager.babyCareApi.getPasswordRecoveryEmail(PasswordRecoveryEmailRequest(email))
            .enqueue(object : Callback<PasswordResetEmailResponse> {
                override fun onResponse(
                    call: Call<PasswordResetEmailResponse>,
                    response: Response<PasswordResetEmailResponse>
                ) {
                    onResponse?.invoke(response.body(), null)
                }

                override fun onFailure(call: Call<PasswordResetEmailResponse>, t: Throwable) {
                    debugLogInfo("Failed-> ${t.message}")
                    onResponse?.invoke(null, t.message)
                }
            })
    }
}