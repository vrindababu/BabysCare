package net.babys_care.app.api.base

import jp.winas.android.foundation.network.HttpMethod
import jp.winas.android.foundation.network.HttpRequest
import net.babys_care.app.BuildConfig
import org.json.JSONObject

internal open class BabyCareHttpRequest<R>(
    httpMethod: HttpMethod = HttpMethod.GET,
    query: Map<String, Any>? = null,
    parameter: JSONObject? = null,
    path: String,
    header: Map<String, String> = mapOf()
) : HttpRequest<R>(
    httpMethod = httpMethod,
    baseUrl = BuildConfig.BASE_URL,
    path = path,
    header = header,
    query = query,
    parameter = parameter
)