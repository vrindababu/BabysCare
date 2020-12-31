package net.babys_care.app.scene.settings

import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.UserUnsubscribeRequest
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.UserUnsubscribeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccountSettingsTopViewModel: BaseViewModel() {

    fun unsubscribeUser(apiToken: String, onResponse: ((UserUnsubscribeResponse?, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.unsubscribeUser(UserUnsubscribeRequest(apiToken))
            .enqueue(object : Callback<UserUnsubscribeResponse> {
                override fun onResponse(
                    call: Call<UserUnsubscribeResponse>,
                    response: Response<UserUnsubscribeResponse>
                ) {
                    if (response.isSuccessful) {
                        onResponse?.invoke(response.body(), null)
                    } else {
                        val error = response.errorBody()
                        val errorMessage = try {
                            Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                        } catch (ex: Exception) {
                            Gson().fromJson(error.toString(), ErrorResponse::class.java)
                        }
                        onResponse?.invoke(null, errorMessage)
                    }
                }

                override fun onFailure(call: Call<UserUnsubscribeResponse>, t: Throwable) {
                    val error = ErrorResponse(t.message ?: "エラー")
                    onResponse?.invoke(null, error)
                }
            })
    }
}