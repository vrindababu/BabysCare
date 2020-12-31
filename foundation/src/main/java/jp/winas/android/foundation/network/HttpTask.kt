package jp.winas.android.foundation.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import jp.winas.android.foundation.BuildConfig
import jp.winas.android.foundation.extension.withMainContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class HttpTask<ResponseBody : Any>(private val coroutineScope: CoroutineScope, val request: HttpRequest<ResponseBody>) {

    private var _onSuccess: ((HttpResponse<ResponseBody>) -> Unit)? = null
    private var _onError: ((HttpError) -> Unit)? = null

    fun execute(typeToken: TypeToken<ResponseBody>): HttpTask<ResponseBody> {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val query = request.query?.entries?.joinToString("&") { "${it.key}=${it.value}" }
                    val url = if (query != null) URL(request.endPoint + "?" + query) else URL(request.endPoint)
                    url.openConnection().let { it as? HttpURLConnection }?.apply {
                        readTimeout = READ_TIMEOUT
                        connectTimeout = CONNECT_TIMEOUT
                        requestMethod = request.httpMethod.name

                        request.header.onEach { setRequestProperty(it.key, it.value) }
                        request.parameter?.let { body ->
                            doOutput = true
                            BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).let {
                                it.write(body.toString())
                                it.flush()
                                it.close()
                            }
                        }
                        when (val statusCode = responseCode) {
                            // Success
                            in HttpURLConnection.HTTP_OK..299 -> {
                                val body: ResponseBody = when (typeToken.rawType) {
                                    Bitmap::class.java -> BitmapFactory.decodeStream(inputStream) as ResponseBody
                                    else -> {
                                        val builder = StringBuilder()
                                        BufferedReader(InputStreamReader(inputStream)).let { reader ->
                                            while (reader.readLine()?.also { builder.append(it) } != null) Unit
                                        }
                                        if (BuildConfig.DEBUG) {
                                            Log.d("API Response", "URL-> $url")
                                            Log.d("API Response", builder.toString())
                                        }
                                        jsonToModel(builder.toString(), typeToken)
                                    }
                                }
                                withMainContext {
                                    _onSuccess?.invoke(
                                        HttpResponse(
                                            statusCode = statusCode,
                                            headers = headerFields,
                                            body = body
                                        )
                                    )
                                }
                            }
                            // Client or Server Error etc.
                            else -> {
                                val builder = StringBuilder()
                                BufferedReader(InputStreamReader(errorStream)).let { reader ->
                                    while (reader.readLine()?.also { builder.append(it) } != null) Unit
                                }
                                if (BuildConfig.DEBUG) {
                                    Log.d("API Response", "URL-> $url")
                                    Log.d("API Response Error", builder.toString())
                                }
                                withMainContext {
                                    _onError?.invoke(HttpError(statusCode, builder.toString()))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("API Error", "$e")
                    withMainContext {
                        _onError?.invoke(HttpError(999, e.message ?: "Exception at HttpTask"))
                    }
                }
            }
        }

        return this
    }

    fun onSuccess(handler: (HttpResponse<ResponseBody>) -> Unit): HttpTask<ResponseBody> {
        _onSuccess = handler
        return this
    }

    fun onError(handler: (HttpError) -> Unit): HttpTask<ResponseBody> {
        _onError = handler
        return this
    }

//    fun <T : Any> jsonToModel(json: String, kClass: KClass<T>): T = Gson().fromJson(json, kClass.java)

    private fun <T : Any> jsonToModel(json: String, typeToken: TypeToken<T>): T = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create().fromJson(json, typeToken.type)

    companion object {
        const val READ_TIMEOUT = 30_000
        const val CONNECT_TIMEOUT = 60_000
    }
}