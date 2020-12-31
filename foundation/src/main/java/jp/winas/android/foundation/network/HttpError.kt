package jp.winas.android.foundation.network

class HttpError(val statusCode: Int, message: String) : Error(message)