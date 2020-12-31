package net.babys_care.app.scene.news

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.NewsRequest
import net.babys_care.app.api.requests.ReadNewsCreateRequest
import net.babys_care.app.api.requests.ReadNewsRequest
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.NewsResponse
import net.babys_care.app.api.responses.ReadNewsResponse
import net.babys_care.app.utils.debugLogInfo

class NewsListViewModel : BaseViewModel() {

    val newsData = MutableLiveData<NewsResponse?>()
    val readNewsLiveData = MutableLiveData<ReadNewsResponse?>()

    fun getNewsList(apiToken: String, onResponse: ((Boolean, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.getNewsList(NewsRequest(apiToken))
                if (response.isSuccessful) {
                    newsData.value = response.body()
                    onResponse?.invoke(true, null)
                } else {
                    val error = response.errorBody()
                    val errorMessage = try {
                        Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                    } catch (ex: Exception) {
                        Gson().fromJson(error.toString(), ErrorResponse::class.java)
                    }
                    debugLogInfo("Error: ${errorMessage.message}")
                    newsData.value = null
                    onResponse?.invoke(false, errorMessage)
                }
            } catch (ex: Exception) {
                val error = ErrorResponse(ex.message ?: "エラー")
                debugLogInfo("Failed: ${error.message}")
                newsData.value = null
                onResponse?.invoke(false, error)
            }
        }
    }

    fun getReadNewsList(apiToken: String) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.getReadNewsList(ReadNewsRequest(apiToken))
                if (response.isSuccessful) {
                    readNewsLiveData.value = response.body()
                } else {
                    val error = response.errorBody()
                    val errorMessage = try {
                        Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                    } catch (ex: Exception) {
                        Gson().fromJson(error.toString(), ErrorResponse::class.java)
                    }
                    debugLogInfo("Error: ${errorMessage.message}")
                    readNewsLiveData.value = null
                }
            } catch (ex: Exception) {
                val error = ErrorResponse(ex.message ?: "エラー")
                debugLogInfo("Failed: ${error.message}")
                readNewsLiveData.value = null
            }
        }
    }

    fun createReadNews(newsId: Int, apiToken: String, onResponse: ((Boolean, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.createReadNews(ReadNewsCreateRequest(apiToken, newsId))
                if (response.isSuccessful) {
                    onResponse?.invoke(true, null)
                } else {
                    val error = response.errorBody()
                    val errorMessage = try {
                        Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                    } catch (ex: Exception) {
                        Gson().fromJson(error.toString(), ErrorResponse::class.java)
                    }
                    debugLogInfo("Error: ${errorMessage.message}")
                    onResponse?.invoke(false, errorMessage)
                }
            } catch (ex: Exception) {
                val error = ErrorResponse(ex.message ?: "エラー")
                debugLogInfo("Failed: ${error.message}")
                onResponse?.invoke(false, error)
            }
        }
    }
}