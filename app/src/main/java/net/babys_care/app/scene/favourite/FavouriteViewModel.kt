package net.babys_care.app.scene.favourite

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.ArticleRequest
import net.babys_care.app.api.requests.FavouriteArticleRequest
import net.babys_care.app.api.requests.TagRequest
import net.babys_care.app.api.responses.*
import net.babys_care.app.utils.debugLogInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavouriteViewModel : BaseViewModel() {

    val articleList: MutableList<Article> = mutableListOf()
    val tagList: MutableList<Tag> = mutableListOf()
    val favouriteArticleIds: MutableList<FavouriteArticle> = mutableListOf()

    fun fetchArticleList(apiToken: String, onResponse: ((ArticleResponse?, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.getArticleList(ArticleRequest(apiToken, null))
            .enqueue(object : Callback<ArticleResponse> {
                override fun onResponse(
                    call: Call<ArticleResponse>,
                    response: Response<ArticleResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.data?.articles?.let { list ->
                            articleList.clear()
                            articleList.addAll(list)
                        }
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
                }

                override fun onFailure(call: Call<ArticleResponse>, t: Throwable) {
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                    debugLogInfo("Response: ${t.message}")
                }
            })
    }

    fun fetchFavouriteArticleIds(apiToken: String, onResponse: ((FavouriteArticleResponse?, ErrorResponse?) -> Unit)? = null) {
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

    fun fetchTags(apiToken: String, onResponse: ((Boolean, ErrorResponse?) -> Unit)?) {
        viewModelScope.launch {
            try {
                val tagResponse = ApiClientManager.babyCareApi.getTags(TagRequest(apiToken))
                tagList.addAll(tagResponse.data.tags)
                onResponse?.invoke(true, null)
            } catch (ex: java.lang.Exception) {
                debugLogInfo("Exception: $ex")
                onResponse?.invoke(false, ErrorResponse(ex.message ?: "エラー"))
            }
        }
    }
}