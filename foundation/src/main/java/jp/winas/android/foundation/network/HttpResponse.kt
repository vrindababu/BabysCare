package jp.winas.android.foundation.network

import javax.net.ssl.HttpsURLConnection

class HttpResponse<ResponseBody>(
    var statusCode: Int = HttpsURLConnection.HTTP_BAD_REQUEST,
    var headers: Map<String, List<String>>,
    var body: ResponseBody
)