package net.babys_care.app.scene.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.GrowthCreateRequest
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.GrowthCreateResponse

class GrowthInputViewModel : BaseViewModel() {

    val growthResponseLiveData = MutableLiveData<GrowthCreateResponse?>()
    val errorResponse = MutableLiveData<ErrorResponse?>()

    fun createGrowthData(
        apiToken: String,
        childId: Int,
        measuredAt: String,
        height: Double,
        weight: Double
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.createGrowthHistory(
                    childId,
                    GrowthCreateRequest(apiToken, childId, measuredAt, height, weight)
                )
                if (response.isSuccessful) {
                    growthResponseLiveData.value = response.body()
                } else {
                    val error = response.errorBody()
                    val errorMessage = try {
                        Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                    } catch (ex: Exception) {
                        try {
                            Gson().fromJson(error.toString(), ErrorResponse::class.java)
                        } catch (e: Exception) {
                            ErrorResponse(e.message ?: AppManager.context.getString(R.string.api_data_error_ea006))
                        }
                    }
                    errorResponse.value = errorMessage
                }
            } catch (ex: Exception) {
                val error = ErrorResponse(ex.message?: AppManager.context.getString(R.string.api_data_error_ea006))
                errorResponse.value = error
            }
        }
    }
}