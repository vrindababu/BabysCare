package net.babys_care.app.scene.settings

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.NotificationEditRequest
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.NotificationEditResponse
import net.babys_care.app.utils.debugLogInfo

class NotificationSettingsViewModel : BaseViewModel() {

    fun updateNotificationSettings(apiToken: String, notifiableLocal: Int, notifiableRemote: Int, onResponse: ((NotificationEditResponse?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.updateNotificationSettings(
                    NotificationEditRequest(apiToken, notifiableLocal, notifiableRemote)
                )
                if (response.isSuccessful) {
                    onResponse?.invoke(response.body(), null)
                } else {
                    val error = response.errorBody()
                    val errorMessage = try {
                        Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                    } catch (ex: Exception) {
                        Gson().fromJson(error.toString(), ErrorResponse::class.java)
                    }
                    debugLogInfo("Error: $errorMessage")
                    onResponse?.invoke(null, errorMessage)
                }
            } catch (e: Exception) {
                debugLogInfo("Exception: $e")
                onResponse?.invoke(null, ErrorResponse(e.message ?: "エラー"))
            }
        }
    }
}