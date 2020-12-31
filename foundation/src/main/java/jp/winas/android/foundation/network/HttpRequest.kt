package jp.winas.android.foundation.network

import org.json.JSONObject

open class HttpRequest<ResponseBody>(
    open val httpMethod: HttpMethod = HttpMethod.GET,
    open val baseUrl: String,
    open val path: String,
    open val header: Map<String, String>,
    open val query: Map<String, Any>? = null,
    open val parameter: JSONObject? = null
) {
    val endPoint: String get() = baseUrl + path
}