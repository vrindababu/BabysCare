package net.babys_care.app.scene.trouble

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

class ArticleListViewModel : BaseViewModel() {

    val articleList: MutableList<Article> = mutableListOf()
    val tagList: MutableList<Tag> = mutableListOf()

    fun fetchArticleList(apiToken: String, parentId: Int? = null, page: Int = 1, onResponse: ((Boolean, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.getArticleList(ArticleRequest(apiToken, null, parentId, page))
            .enqueue(object : Callback<ArticleResponse> {
                override fun onResponse(
                    call: Call<ArticleResponse>,
                    response: Response<ArticleResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.data?.articles?.let { list ->
                            if (page == 1) {
                                articleList.clear()
                            }
                            articleList.addAll(list)
                        }
                        onResponse?.invoke(true, null)
                    } else {
                        val error = response.errorBody()
                        val errorMessage = try {
                            Gson().fromJson(error?.charStream(), ErrorResponse::class.java)
                        } catch (ex: Exception) {
                            Gson().fromJson(error.toString(), ErrorResponse::class.java)
                        }
                        debugLogInfo("Response: ${errorMessage.message}")
                        onResponse?.invoke(false, errorMessage)
                    }
                }

                override fun onFailure(call: Call<ArticleResponse>, t: Throwable) {
                    onResponse?.invoke(false, ErrorResponse((t.message ?: "エラー")))
                    debugLogInfo("Response: ${t.message}")
                }
            })
    }

    private fun fetchFavouriteArticleIds(apiToken: String, onResponse: ((FavouriteArticleResponse?, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.getFavouriteArticleIds(FavouriteArticleRequest(apiToken))
            .enqueue(object : Callback<FavouriteArticleResponse> {
                override fun onResponse(
                    call: Call<FavouriteArticleResponse>,
                    response: Response<FavouriteArticleResponse>
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

                override fun onFailure(call: Call<FavouriteArticleResponse>, t: Throwable) {
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                }
            })
    }

    fun getFavouriteArticles(apiToken: String, onResponse: ((FavouriteArticleResponse?, ErrorResponse?) -> Unit)? = null) {
        fetchFavouriteArticleIds(apiToken) {response, error ->
            if (response != null) {
                for (favourite in response.data.favorites) {
                    for (article in articleList) {
                        if (article.articleId == favourite.article_id) {
                            article.isFavourite = true
                            break
                        }
                    }
                }

                onResponse?.invoke(response, null)
            } else {
                onResponse?.invoke(null, error)
            }
        }
    }

    fun fetchTags(apiToken: String, onResponse: ((List<Tag>?, ErrorResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val tagResponse = ApiClientManager.babyCareApi.getTags(TagRequest(apiToken))
                tagList.clear()
                tagList.addAll(tagResponse.data.tags)
                onResponse?.invoke(tagResponse.data.tags, null)
            }catch (ex: java.lang.Exception) {
                debugLogInfo("Exception: $ex")
                onResponse?.invoke(null, ErrorResponse(ex.message ?: "エラー"))
            }
        }
    }
}