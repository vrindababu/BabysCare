package net.babys_care.app.scene

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.LogoutRequest
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.LogoutResponse
import net.babys_care.app.utils.debugLogInfo

class MainViewModel : BaseViewModel() {

    fun logoutUser(apiToken: String, onResponse: ((LogoutResponse?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.logoutUser(LogoutRequest(apiToken))
                if (response.isSuccessful) {
                    onResponse?.invoke(response.body(), null)
                } else {
                    val error = response.errorBody()
                    val errorMessage = try {
                        Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                    } catch (ex: Exception) {
                        Gson().fromJson(error.toString(), ErrorResponse::class.java)
                    }
                    debugLogInfo("Response: ${errorMessage.message}")
                    onResponse?.invoke(null, errorMessage)
                }
            } catch (e: Exception) {
                debugLogInfo("Error: $e")
                onResponse?.invoke(null, ErrorResponse(e.message ?: "エラー"))
            }
        }
    }
}