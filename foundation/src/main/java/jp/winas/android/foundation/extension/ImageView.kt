package jp.winas.android.foundation.extension

import android.graphics.Bitmap
import android.widget.ImageView
import jp.winas.android.foundation.network.*
import kotlinx.coroutines.CoroutineScope
import java.lang.Exception

fun ImageView.setImageUrl(url: String?, defImageResId: Int? = null, completionHandler: ((HttpResponse<Bitmap>?, HttpError?) -> Unit)? = null) {
    // context should be an instance of BaseActivity or BaseFragment
//        Log.i("foundation", "setImageUrl : url = $url")

    val coroutineScope: CoroutineScope = try { context as CoroutineScope
    } catch (e: Exception) { return }
    if (url == null) return

    val request = HttpRequest<Bitmap>(
        baseUrl = url,
        httpMethod = HttpMethod.GET,
        path = "",
        header = mapOf()
    )

    HttpClient.send(request, coroutineScope)
        .onSuccess { response ->
            //                Log.i("foundation", "setImageUrl onSuccess : statusCode = ${response.statusCode}")
            setImageBitmap(response.body)
            completionHandler?.invoke(response, null)
        }
        .onError { error ->
            //                Log.i("foundation", "setImageUrl onError : statusCode = ${error.statusCode}, error = ${error.message}")
            defImageResId?.let { setImageResource(it) }
            completionHandler?.invoke(null, error)
        }
}