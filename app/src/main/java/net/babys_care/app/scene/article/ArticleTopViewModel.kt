package net.babys_care.app.scene.article

import androidx.lifecycle.viewModelScope
import jp.winas.android.foundation.scene.BaseViewModel
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.ApiClientManager
import net.babys_care.app.api.requests.*
import net.babys_care.app.api.responses.*
import net.babys_care.app.utils.debugLogInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArticleTopViewModel : BaseViewModel() {

    val articleList: MutableList<Article> = mutableListOf()
    val tagList: MutableList<Tag> = mutableListOf()
    val searchHistories: MutableList<SearchHistory> = mutableListOf()
    val favouriteArticleIds: MutableList<FavouriteArticle> = mutableListOf()
    var isLastPage: Boolean = false

    fun fetchSearchHistory(apiToken: String, onResponse: ((SearchHistoryResponse?, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.getSearchHistories(SearchHistoryRequest(apiToken))
            .enqueue(object : Callback<SearchHistoryResponse> {
                override fun onResponse(
                    call: Call<SearchHistoryResponse>,
                    response: Response<SearchHistoryResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.data?.search_histories?.let {
                            searchHistories.addAll(it)
                        }
                        onResponse?.invoke(response.body(), null)
                    } else {
                        onResponse?.invoke(null, ErrorResponse(getErrorMessage(response.code())))
                    }
                }

                override fun onFailure(call: Call<SearchHistoryResponse>, t: Throwable) {
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                }
            })
    }

    fun fetchArticleList(apiToken: String, page: Int, searchWord: String? = null, onResponse: ((ArticleResponse?, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.getArticleList(ArticleRequest(apiToken, searchWord, null, page))
            .enqueue(object : Callback<ArticleResponse> {
                override fun onResponse(
                    call: Call<ArticleResponse>,
                    response: Response<ArticleResponse>
                ) {
                    if (response.isSuccessful) {
                        onResponse?.invoke(response.body(), null)
                    } else {
                        val error = getErrorMessage(response.code())
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
                        onResponse?.invoke(null, ErrorResponse(getErrorMessage(response.code())))
                    }
                }

                override fun onFailure(call: Call<FavouriteArticleResponse>, t: Throwable) {
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                }
            })
    }

    fun fetchTags(apiToken: String) {
        viewModelScope.launch {
            try {
                val tagResponse = ApiClientManager.babyCareApi.getTags(TagRequest(apiToken))
                tagList.addAll(tagResponse.data.tags)
            }catch (ex: java.lang.Exception) {
                debugLogInfo("Exception: $ex")
            }
        }
    }

    fun createSearchHistory(apiToken: String, word: String, onResponse: ((CreateSearchHistoryResponse?, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.createSearchHistory(CreateSearchHistoryRequest(apiToken, word))
            .enqueue(object : Callback<CreateSearchHistoryResponse> {
                override fun onResponse(
                    call: Call<CreateSearchHistoryResponse>,
                    response: Response<CreateSearchHistoryResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.search_histories?.let {
                            searchHistories.clear()
                            searchHistories.addAll(it)
                        }
                        onResponse?.invoke(response.body(), null)
                    } else {
                        onResponse?.invoke(null, ErrorResponse(getErrorMessage(response.code())))
                    }
                }

                override fun onFailure(call: Call<CreateSearchHistoryResponse>, t: Throwable) {
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                }
            })
    }

    fun deleteSearchHistory(apiToken: String, historyId: Int, deleteAll: Int = 0, onResponse: ((DeleteSearchHistoryResponse?, ErrorResponse?) -> Unit)? = null) {
        ApiClientManager.babyCareApi.deleteSearchHistory(DeleteSearchHistoryRequest(apiToken, historyId, deleteAll))
            .enqueue(object : Callback<DeleteSearchHistoryResponse> {
                override fun onResponse(
                    call: Call<DeleteSearchHistoryResponse>,
                    response: Response<DeleteSearchHistoryResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.data?.search_histories?.let {
                            searchHistories.clear()
                            searchHistories.addAll(it)
                        }
                        onResponse?.invoke(response.body(), null)
                    } else {
                        onResponse?.invoke(null, ErrorResponse(getErrorMessage(response.code())))
                    }
                }

                override fun onFailure(call: Call<DeleteSearchHistoryResponse>, t: Throwable) {
                    onResponse?.invoke(null, ErrorResponse((t.message ?: "エラー")))
                }
            })
    }

    private fun getErrorMessage(code: Int): String {
        return when(code){
            400 -> AppManager.context.getString(R.string.input_error_ea001)
            401 -> AppManager.context.getString(R.string.authentication_error_ea002)
            403 -> AppManager.context.getString(R.string.account_has_been_deleted_ea003)
            503 -> AppManager.context.getString(R.string.currently_under_maintenance_ea004)
            422 -> AppManager.context.getString(R.string.input_content_error_ea005)
            else -> AppManager.context.getString(R.string.api_data_error_ea006)
        }
    }
}