package net.babys_care.app.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.babys_care.app.AppManager
import net.babys_care.app.BuildConfig
import okhttp3.Credentials
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit

open class ApiClientManager {
    companion object {
        private val cookieJar = JavaNetCookieJar(CookieManager())

        val builder: Gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").serializeNulls().create()

        val babyCareApi: BabyCareApi
            get() = Retrofit.Builder()
                .client(getClient())
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(builder))
                .build()
                .create(BabyCareApi::class.java)

        private fun getClient(): OkHttpClient {

            return OkHttpClient
                .Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .addInterceptor {
                    val response = it.proceed(
                        it.request()
                            .newBuilder().let { builder ->
                                if (it.request().url.encodedPath.contains("/image")) {
                                    builder.addHeader("Accept", "application/json")
                                    builder.addHeader("Authorization", "Bearer ${AppManager.apiToken}")
                                    builder.addHeader("Cache-Control", "no-cache")
                                } else {
                                    if (BuildConfig.FLAVOR == "dev" || BuildConfig.FLAVOR == "demo" || BuildConfig.FLAVOR == "stg") {
                                        builder.addHeader("Authorization", Credentials.basic("winas-akachan", "n5Wf4CK0LK"))
                                    }
                                    builder.addHeader("Content-Type", "application/json")
                                    builder.addHeader("Accept", "application/json")
                                    builder.addHeader("X-APP-OS", "Android")
                                    builder.addHeader("X-app-version-android", getVersionName())
                                    builder.addHeader("Cache-Control", "no-cache")
                                }

                            }
                            .build()
                    )
                    if (response.code == 401) {
                        AppManager.isLoggedIn = false
                    }
                    response
                }
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .cookieJar(cookieJar)
                .build()
        }

        fun getVersionName(): String {
            return AppManager.context.packageManager.getPackageInfo(AppManager.context.packageName, 0)?.versionName ?: ""
        }
    }
}