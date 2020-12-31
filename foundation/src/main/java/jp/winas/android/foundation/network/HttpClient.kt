package jp.winas.android.foundation.network

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope

object HttpClient {

    inline fun <reified ResponseBody : Any> send(request: HttpRequest<ResponseBody>, coroutineScope: CoroutineScope): HttpTask<ResponseBody> =
        createTask(request, coroutineScope).execute(object : TypeToken<ResponseBody>() {})

    inline fun <reified ResponseBody : Any> createTask(request: HttpRequest<ResponseBody>, coroutineScope: CoroutineScope) = HttpTask(coroutineScope, request)

}

enum class HttpMethod {
    GET, POST
}