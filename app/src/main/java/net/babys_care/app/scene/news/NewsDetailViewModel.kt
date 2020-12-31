package net.babys_care.app.scene.news

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.NewsDetailRequest
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.NewsDetailResponse
import net.babys_care.app.utils.debugLogInfo

class NewsDetailViewModel : BaseViewModel() {

    fun getNoticeDetails(newsId: Int, apiToken: String, onResponse: ((NewsDetailResponse?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.getNewsDetails(newsId, NewsDetailRequest(apiToken))
                if (response.isSuccessful) {
                    onResponse?.invoke(response.body(), null)
                } else {
                    val error = response.errorBody()
                    val errorMessage = try {
                        Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                    } catch (ex: Exception) {
                        Gson().fromJson(error.toString(), ErrorResponse::class.java)
                    }
                    debugLogInfo("Error: ${errorMessage.message}")
                    onResponse?.invoke(null, errorMessage)
                }
            } catch (ex: Exception) {
                debugLogInfo("Exception: $ex")
                onResponse?.invoke(null, ErrorResponse(ex.message ?: "エラー"))
            }
        }
    }
}