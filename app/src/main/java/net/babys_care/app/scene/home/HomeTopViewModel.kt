package net.babys_care.app.scene.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.ArticleRequest
import net.babys_care.app.api.requests.AuthorRequest
import net.babys_care.app.api.requests.FavouriteArticleRequest
import net.babys_care.app.api.requests.TagRequest
import net.babys_care.app.api.responses.*
import net.babys_care.app.models.ChildGrowth
import net.babys_care.app.utils.debugLogInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeTopViewModel : BaseViewModel() {

    val articleList: MutableList<Article> =  mutableListOf()
    val favouriteArticleIds: MutableList<FavouriteArticle> = mutableListOf()
    val authorList: MutableList<Author> = mutableListOf()
    val tagList: MutableList<Tag> = mutableListOf()

    private val _smallestChildBirthDate = MutableLiveData<String>()
    val smallestChildBirthDate: LiveData<String> = _smallestChildBirthDate

    private val _selectedChildId = MutableLiveData<Int>()
    val selectedChildId: LiveData<Int> = _selectedChildId


    fun fetchArticleList(apiToken: String, pageNo: Int, onResponse: ((ArticleResponse?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            ApiClientManager.babyCareApi.getArticleList(ArticleRequest(apiToken, null, page = pageNo))
                .enqueue(object : Callback<ArticleResponse> {
                    override fun onResponse(
                        call: Call<ArticleResponse>,
                        response: Response<ArticleResponse>
                    ) {
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
                            debugLogInfo("Response: $error")
                            onResponse?.invoke(null, ErrorResponse(error))
                        }
                    }

                    override fun onFailure(call: Call<ArticleResponse>, t: Throwable) {
                        onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                        debugLogInfo("Response: ${t.message}")
                    }
                })
        }
    }

    fun fetchFavouriteArticleIds(apiToken: String, onResponse: ((FavouriteArticleResponse?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            ApiClientManager.babyCareApi.getFavouriteArticleIds(FavouriteArticleRequest(apiToken))
                .enqueue(object : Callback<FavouriteArticleResponse> {
                    override fun onResponse(
                        call: Call<FavouriteArticleResponse>,
                        response: Response<FavouriteArticleResponse>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.data?.favorites?.let { list ->
                                favouriteArticleIds.clear()
                                favouriteArticleIds.addAll(list)
                            }
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

                    override fun onFailure(call: Call<FavouriteArticleResponse>, t: Throwable) {
                        onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                    }
                })
        }
    }

    fun fetchTags(apiToken: String, onResponse: ((List<Tag>, ErrorResponse?)-> Unit)? = null) {
        viewModelScope.launch {
            try {
                val tagResponse = ApiClientManager.babyCareApi.getTags(TagRequest(apiToken))
                tagList.addAll(tagResponse.data.tags)
                onResponse?.invoke(tagResponse.data.tags, null)
            }catch (ex: java.lang.Exception) {
                debugLogInfo("Exception: $ex")
                onResponse?.invoke(listOf(), ErrorResponse(ex.message ?: "エラー"))
            }
        }
    }

    fun fetchAuthor(apiToken: String, onResponse: ((AuthorResponse?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val authorResponse = ApiClientManager.babyCareApi.getAuthor(AuthorRequest(apiToken))
                if (authorResponse.isSuccessful) {
                    authorList.clear()
                    authorResponse.body()?.data?.authors?.let {
                        authorList.addAll(it)
                    }
                    onResponse?.invoke(authorResponse.body(), null)
                } else {
                    val error = ErrorResponse(authorResponse.errorBody().toString())
                    debugLogInfo("Error: $error")
                    onResponse?.invoke(null, error)
                }
            }catch (ex: Exception) {
                val error = ErrorResponse(ex.message ?: "エラー")
                onResponse?.invoke(null, error)
                debugLogInfo("Error: $ex")
            }
        }
    }

    fun setBirthday(childGrowth: ChildGrowth) {
        _smallestChildBirthDate.value=childGrowth.birthDay
    }

    fun getBirthDate(): String? {
        return smallestChildBirthDate.value
    }

    fun setSelectedChildId(childGrowth: ChildGrowth) {
        _selectedChildId.value=childGrowth.childId
    }

    fun getSelectedChildId(): Int? {
        return selectedChildId.value
    }
}