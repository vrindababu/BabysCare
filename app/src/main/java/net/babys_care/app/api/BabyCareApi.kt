package net.babys_care.app.api

import androidx.annotation.CheckResult
import net.babys_care.app.api.requests.*
import net.babys_care.app.api.responses.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface BabyCareApi {

    @POST("/api/password/email")
    @CheckResult
    fun getPasswordRecoveryEmail(
        @Body passwordRecoveryEmailRequest: PasswordRecoveryEmailRequest
    ): Call<PasswordResetEmailResponse>

    @PUT("/api/user/create")
    @CheckResult
    fun createUser(
        @Body userCreateRequest: UserCreateRequest
    ): Call<UserCreateResponse>

    @POST("/api/child/body")
    @CheckResult
    fun getGrowthHistories(
        @Body growthHistoryRequest: GrowthHistoryRequest
    ): Call<GrowthHistoryResponse>

    @PUT("/api/child/body/create/{id}")
    @CheckResult
    suspend fun createGrowthHistory(
        @Path("id") id: Int,
        @Body growthCreateRequest: GrowthCreateRequest
    ): Response<GrowthCreateResponse>

    @POST("/api/login")
    @CheckResult
    fun loginUser(
        @Body loginRequest: LoginRequest
    ): Call<TokenResponse>

    @POST("/api/logout")
    @CheckResult
    suspend fun logoutUser(
        @Body logoutRequest: LogoutRequest
    ): Response<LogoutResponse>

    @POST("/api/user")
    @CheckResult
    fun getUserInfo(
        @Body userInfoRequest: UserInfoRequest
    ): Call<UserInfoResponse>

    @DELETE("/api/user/unsubscribe")
    @CheckResult
    fun unsubscribeUser(
        @Body userUnsubscribeRequest: UserUnsubscribeRequest
    ): Call<UserUnsubscribeResponse>

    @PUT("/api/user/edit/parent")
    @CheckResult
    fun updateUserInfo(
        @Body userInfoUpdateRequest: UserInfoUpdateRequest
    ): Call<UserInfoUpdateResponse>

    @Multipart
    @POST("/api/user/edit/parent/{id}/image")
    suspend fun updateUserImage(
        @Path("id") id: Int,
        @Part image: MultipartBody.Part
    ): Response<UserImageUploadResponse>

    @PUT("/api/user/create/child")
    @CheckResult
    fun createBabyInfo(
        @Body babyInfoUpdateRequest: BabyInfoUpdateRequest
    ): Call<ChildCreateResponse>

    @PUT("/api/user/edit/child/{id}")
    @CheckResult
    fun updateBabyInfo(
        @Path("id") id: Int,
        @Body babyInfoUpdateRequest: BabyInfoUpdateRequest
    ): Call<UserInfoUpdateResponse>

    @Multipart
    @POST("/api/user/edit/child/{id}/image")
    suspend fun updateBabyImage(
        @Path("id") id: Int,
        @Part image: MultipartBody.Part
    ): Response<UserImageUploadResponse>

    @POST("/api/search_history")
    @CheckResult
    fun getSearchHistories(
        @Body searchHistoryRequest: SearchHistoryRequest
    ): Call<SearchHistoryResponse>

    @HTTP( method = "DELETE", path = "/api/search_history/delete", hasBody = true)
    @CheckResult
    fun deleteSearchHistory(
        @Body deleteSearchHistoryRequest: DeleteSearchHistoryRequest
    ): Call<DeleteSearchHistoryResponse>

    @PUT("/api/search_history/create")
    @CheckResult
    fun createSearchHistory(
        @Body createSearchHistoryRequest: CreateSearchHistoryRequest
    ): Call<CreateSearchHistoryResponse>

    @POST("/api/article")
    @CheckResult
    fun getArticleList(
        @Body articleRequest: ArticleRequest
    ): Call<ArticleResponse>

    @POST("/api/favorite/article")
    @CheckResult
    fun getFavouriteArticleIds(
        @Body favouriteArticleRequest: FavouriteArticleRequest
    ): Call<FavouriteArticleResponse>

    @PUT("/api/favorite/create")
    @CheckResult
    suspend fun createFavourite(
        @Body favouriteCreateRequest: FavouriteCreateRequest
    ): FavouriteCreateResponse

    @HTTP( method = "DELETE", path = "/api/favorite/delete", hasBody = true)
    @CheckResult
    suspend fun deleteFavourite(
        @Body favouriteDeleteRequest: FavouriteDeleteRequest
    ): FavouriteDeleteResponse

    @POST("/api/tag")
    @CheckResult
    suspend fun getTags(
        @Body tagRequest: TagRequest
    ): TagResponse

    @POST("/api/author")
    @CheckResult
    suspend fun getAuthor(
        @Body authorRequest: AuthorRequest
    ): Response<AuthorResponse>

    @POST("/api/browse_history")
    @CheckResult
    suspend fun getBrowsingHistory(
        @Body browseHistoryRequest: BrowseHistoryRequest
    ): Response<BrowsingHistoryResponse>

    @PUT("/api/browse_history/create")
    @CheckResult
    suspend fun createBrowsingHistory(
        @Body createBrowseHistoryRequest: CreateBrowseHistoryRequest
    ): Response<BrowsingHistoryResponse>

    @PUT("/api/notification/edit")
    @CheckResult
    suspend fun updateNotificationSettings(
        @Body notificationEditRequest: NotificationEditRequest
    ): Response<NotificationEditResponse>

    @POST("/api/news")
    @CheckResult
    suspend fun getNewsList(
        @Body newsRequest: NewsRequest
    ): Response<NewsResponse>

    @POST("/api/read_news")
    @CheckResult
    suspend fun getReadNewsList(
        @Body readNewsRequest: ReadNewsRequest
    ): Response<ReadNewsResponse>

    @POST("/api/news/{id}")
    @CheckResult
    suspend fun getNewsDetails(
        @Path("id") id: Int,
        @Body newsDetailRequest: NewsDetailRequest
    ): Response<NewsDetailResponse>

    @PUT("/api/read_news/create")
    @CheckResult
    suspend fun createReadNews(
        @Body readNewsCreateRequest: ReadNewsCreateRequest
    ): Response<ReadNewsCreateResponse>
}