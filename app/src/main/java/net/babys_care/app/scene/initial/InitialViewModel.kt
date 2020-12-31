package net.babys_care.app.scene.initial

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.*
import net.babys_care.app.api.responses.*
import net.babys_care.app.utils.debugLogInfo
import net.babys_care.app.utils.version.Version
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InitialViewModel: BaseViewModel() {

    val userInfoLiveData = MutableLiveData<UserInfoResponse?>()
    val growthHistoryLiveData = MutableLiveData<GrowthHistoryResponse?>()
    val errorResponse = MutableLiveData<ErrorResponse?>()
    val newsData = MutableLiveData<NewsResponse?>()
    val readNewsLiveData = MutableLiveData<ReadNewsResponse?>()

    fun fetchApiToken(email: String, password: String, fcmToken: String, onResponse: ((TokenResponse?, String?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.loginUser(LoginRequest(email, password, fcmToken))
            .enqueue(object : Callback<TokenResponse> {
                override fun onResponse(
                    call: Call<TokenResponse>,
                    response: Response<TokenResponse>
                ) {
                    val appVersion = response.headers()["X-app-version-android"]
                    if (!appVersion.isNullOrEmpty()) {
                        val version = Version(appVersion)
                        if (version.isHigherThan(ApiClientManager.getVersionName())) {
                            val error = ErrorResponse("最新のアプリに\n更新してください。", 100)
                            errorResponse.value = error
                            return
                        }
                    }
                    if (response.isSuccessful) {
                        onResponse?.invoke(response.body(), null)
                    } else {
                        val error = when(response.code()){
                            400 -> AppManager.context.getString(R.string.input_error_ea001)
                            401 -> AppManager.context.getString(R.string.authentication_error_ea002)
                            403 -> AppManager.context.getString(R.string.account_has_been_deleted_ea003)
                            503 -> AppManager.context.getString(R.string.currently_under_maintenance_ea004)
                            422 -> AppManager.context.getString(R.string.input_content_error_ea005)
                            else -> AppManager.context.getString(R.string.api_data_error_ea006)
                        }
                        onResponse?.invoke(null, error)
                    }
                }

                override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                    debugLogInfo("onFailure: ${t.message}")
                    onResponse?.invoke(null, t.message)
                }
            })
    }

    fun fetchUserInfo(apiToken: String) {
        ApiClientManager.babyCareApi.getUserInfo(UserInfoRequest(apiToken))
            .enqueue(object : Callback<UserInfoResponse> {
                override fun onResponse(
                    call: Call<UserInfoResponse>,
                    response: Response<UserInfoResponse>
                ) {
                    if (response.isSuccessful) {
                        userInfoLiveData.value = response.body()
                    } else {
                        userInfoLiveData.value = null
                        val error = response.errorBody()
                        errorResponse.value = try {
                            Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                        } catch (ex: Exception) {
                            Gson().fromJson(error.toString(), ErrorResponse::class.java)
                        }
                    }
                }

                override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                    debugLogInfo("onFailure: ${t.message}")
                    userInfoLiveData.value = null
                    errorResponse.value = Gson().fromJson(t.message ?: "", ErrorResponse::class.java)
                }
            })
    }

    fun fetchGrowthHistory(apiToken: String) {
        ApiClientManager.babyCareApi.getGrowthHistories(GrowthHistoryRequest((apiToken)))
            .enqueue(object : Callback<GrowthHistoryResponse> {
                override fun onResponse(
                    call: Call<GrowthHistoryResponse>,
                    response: Response<GrowthHistoryResponse>
                ) {
                    if (response.isSuccessful) {
                        growthHistoryLiveData.value = response.body()
                    } else {
                        growthHistoryLiveData.value = null
                        val error = response.errorBody()
                        errorResponse.value = try {
                            Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                        } catch (ex: Exception) {
                            Gson().fromJson(error.toString(), ErrorResponse::class.java)
                        }
                    }
                }

                override fun onFailure(call: Call<GrowthHistoryResponse>, t: Throwable) {
                    debugLogInfo("onFailed: ${t.message}")
                    growthHistoryLiveData.value = null
                    errorResponse.value = Gson().fromJson(t.message, ErrorResponse::class.java)
                }
            })
    }

    fun getNewsList(apiToken: String) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.getNewsList(NewsRequest(apiToken))
                if (response.isSuccessful) {
                    newsData.value = response.body()
                } else {
                    newsData.value = null
                }
            } catch (ex: Exception) {
                newsData.value = null
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
                    readNewsLiveData.value = null
                }
            } catch (ex: Exception) {
                readNewsLiveData.value = null
            }
        }
    }
}