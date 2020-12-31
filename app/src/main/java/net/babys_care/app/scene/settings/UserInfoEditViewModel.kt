package net.babys_care.app.scene.settings

import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.UserInfoUpdateRequest
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.UserImageUploadResponse
import net.babys_care.app.api.responses.UserInfoUpdateResponse
import net.babys_care.app.models.Parent
import net.babys_care.app.utils.debugLogInfo
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserInfoEditViewModel : BaseViewModel() {
    val parentUser = Parent(
        "", "", "", "", "",
        "", "", 0, "", ""
    )

    fun updateUserInfo(
        request: UserInfoUpdateRequest,
        onResponse: ((UserInfoUpdateResponse?, ErrorResponse?) -> Unit)? = null
    ) {
        ApiClientManager.babyCareApi.updateUserInfo(request)
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

    suspend fun uploadUserImage(
        parentId: Int,
        image: MultipartBody.Part,
        onResponse: ((UserImageUploadResponse?, ErrorResponse?) -> Unit)? = null
    ) {
        try {
            val response = ApiClientManager.babyCareApi.updateUserImage(parentId, image)
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
        } catch (ex: Exception) {
            debugLogInfo("Exception:$ex")
            onResponse?.invoke(null, ErrorResponse((ex.message ?: "エラー")))
        }
    }
}