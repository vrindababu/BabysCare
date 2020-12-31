package net.babys_care.app.scene.registration

import androidx.lifecycle.MutableLiveData
import jp.winas.android.foundation.scene.BaseViewModel
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.UserCreateRequest
import net.babys_care.app.api.responses.UserCreateResponse
import net.babys_care.app.models.Children
import net.babys_care.app.models.Parent
import net.babys_care.app.models.User
import net.babys_care.app.utils.debugLogInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BabyRegistrationViewModel: BaseViewModel() {

    val userLiveData = MutableLiveData<UserCreateResponse?>()

    fun createUser(parent: Parent, children: Children?, user: User, onError: ((String) -> Unit)? = null) {
        ApiClientManager.babyCareApi.createUser(UserCreateRequest(parent, children ?: Object(), user))
            .enqueue(object : Callback<UserCreateResponse> {
                override fun onResponse(
                    call: Call<UserCreateResponse>,
                    response: Response<UserCreateResponse>
                ) {
                    if (response.isSuccessful) {
                        userLiveData.value = response.body()
                        debugLogInfo("Response: ${userLiveData.value}")
                    } else {
                        val error = when(response.code()){
                            400 -> AppManager.context.getString(R.string.input_error_ea001)
                            401 -> AppManager.context.getString(R.string.authentication_error_ea002)
                            403 -> AppManager.context.getString(R.string.account_has_been_deleted_ea003)
                            503 -> AppManager.context.getString(R.string.currently_under_maintenance_ea004)
                            422 -> AppManager.context.getString(R.string.input_content_error_ea005)
                            else -> AppManager.context.getString(R.string.api_data_error_ea006)
                        }
                        onError?.invoke(error)
                    }
                }

                override fun onFailure(call: Call<UserCreateResponse>, t: Throwable) {
                    debugLogInfo("Failed: ${t.message}")
                    userLiveData.value = null
                    onError?.invoke(t.message ?: AppManager.context.getString(R.string.api_data_error_ea006))
                }
            })
    }
}