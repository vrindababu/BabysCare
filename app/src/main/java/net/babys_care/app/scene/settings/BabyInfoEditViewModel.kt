package net.babys_care.app.scene.settings

import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.BabyInfoUpdateRequest
import net.babys_care.app.api.responses.ChildCreateResponse
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.UserImageUploadResponse
import net.babys_care.app.api.responses.UserInfoUpdateResponse
import net.babys_care.app.models.Children
import net.babys_care.app.utils.debugLogInfo
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BabyInfoEditViewModel: BaseViewModel() {

    var child: Children = Children()

    fun createBabyInfo(
        request: BabyInfoUpdateRequest,
        onResponse: ((ChildCreateResponse?, ErrorResponse?) -> Unit)? = null
    ) {
        ApiClientManager.babyCareApi.createBabyInfo(request)
            .enqueue(object : Callback<ChildCreateResponse> {
                override fun onResponse(
                    call: Call<ChildCreateResponse>,
                    response: Response<ChildCreateResponse>
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

                override fun onFailure(call: Call<ChildCreateResponse>, t: Throwable) {
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                }
            })
    }

    fun updateBabyInfo(
        childId: Int,
        request: BabyInfoUpdateRequest,
        onResponse: ((UserInfoUpdateResponse?, ErrorResponse?) -> Unit)? = null
    ) {
        ApiClientManager.babyCareApi.updateBabyInfo(childId, request)
            .enqueue(object : Callback<UserInfoUpdateResponse> {
                override fun onResponse(
                    call: Call<UserInfoUpdateResponse>,
                    response: Response<UserInfoUpdateResponse>
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

                override fun onFailure(call: Call<UserInfoUpdateResponse>, t: Throwable) {
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                }
            })
    }

    suspend fun uploadBabyImage(
        image: MultipartBody.Part,
        childId: Int,
        onResponse: ((UserImageUploadResponse?, ErrorResponse?) -> Unit)? = null
    ) {
        val response = ApiClientManager.babyCareApi.updateBabyImage(childId, image)
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
}