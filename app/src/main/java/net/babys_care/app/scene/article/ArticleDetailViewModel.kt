package net.babys_care.app.scene.article

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.*
import net.babys_care.app.api.responses.*
import net.babys_care.app.utils.debugLogInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArticleDetailViewModel: BaseViewModel() {

    val favouriteArticleIds: MutableList<FavouriteArticle> = mutableListOf()
    val authorLiveData = MutableLiveData<List<Author>>()
    val tagList: MutableList<Tag> = mutableListOf()

    fun fetchFavouriteArticleIds(apiToken: String, onResponse: ((FavouriteArticleResponse?, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.getFavouriteArticleIds(FavouriteArticleRequest(apiToken))
            .enqueue(object : Callback<FavouriteArticleResponse> {
                override fun onResponse(
                    call: Call<FavouriteArticleResponse>,
                    response: Response<FavouriteArticleResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.data?.favorites?.let { list ->
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
                        favouriteArticleIds.clear()
                        onResponse?.invoke(null, errorMessage)
                    }
                }

                override fun onFailure(call: Call<FavouriteArticleResponse>, t: Throwable) {
                    favouriteArticleIds.clear()
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                }
            })
    }

    fun fetchAuthor(apiToken: String) {
        viewModelScope.launch {
            try {
                val authorResponse = ApiClientManager.babyCareApi.getAuthor(AuthorRequest(apiToken))
                if (authorResponse.isSuccessful) {
                    authorResponse.body()?.data?.authors?.let {
                        authorLiveData.value = it
                    }
                } else {
                    debugLogInfo("Error: ${authorResponse.errorBody().toString()}")
                }
            }catch (ex: Exception) {
                authorLiveData.value = listOf()
                debugLogInfo("Error: $ex")
            }
        }
    }

    fun createFavourite(apiToken: String, articleId: Int, onResponse: ((FavouriteCreateResponse?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.createFavourite(FavouriteCreateRequest(apiToken, articleId))
                onResponse?.invoke(response, null)
            } catch (ex: Exception) {
                val error = ErrorResponse(ex.message ?: "エラー")
                onResponse?.invoke(null, error)
            }
        }
    }

    fun deleteFavourite(apiToken: String, articleId: Int, onResponse: ((FavouriteDeleteResponse?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.deleteFavourite(FavouriteDeleteRequest(apiToken, articleId))
                favouriteArticleIds.clear()
                favouriteArticleIds.addAll(response.data.favorites)
                onResponse?.invoke(response, null)
            } catch (ex: Exception) {
                val error = ErrorResponse(ex.message ?: "エラー")
                onResponse?.invoke(null, error)
            }
        }
    }

    fun fetchTags(apiToken: String) {
        viewModelScope.launch {
            try {
                val tagResponse = ApiClientManager.babyCareApi.getTags(TagRequest(apiToken))
                tagList.addAll(tagResponse.data.tags)
            } catch (ex: java.lang.Exception) {
                debugLogInfo("Exception: $ex")
            }
        }
    }

    fun createBrowsingHistory(apiToken: String, articleId: Int) {
        viewModelScope.launch {
            try {
                val response = ApiClientManager.babyCareApi.createBrowsingHistory(CreateBrowseHistoryRequest(apiToken, articleId))
                if (response.isSuccessful) {
                    debugLogInfo("Success: ${response.body()}")
                } else {
                    debugLogInfo("Failed")
                }
            } catch (ex: java.lang.Exception) {
                debugLogInfo("Exception: $ex")
            }
        }
    }
}