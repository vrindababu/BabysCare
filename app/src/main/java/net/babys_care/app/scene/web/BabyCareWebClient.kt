package net.babys_care.app.scene.web

import android.content.Intent
import android.net.Uri
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import net.babys_care.app.AppManager
import net.babys_care.app.BuildConfig
import net.babys_care.app.utils.NavigationController
import net.babys_care.app.utils.debugLogInfo

class BabyCareWebClient(private val navigationController: NavigationController, private val script: String? = null, private val onJSEvaluationFinished: (() -> Unit)? = null): WebViewClient() {

    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
        if (BuildConfig.FLAVOR == "dev" || BuildConfig.FLAVOR == "demo") {
            handler?.proceed("winas-akachan", "n5Wf4CK0LK")
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.url?.toString()?.let { urlString ->
            if (findNativePageOf(urlString)) {
                return true
            }

            if (overrideUrlLoading(urlString) || urlString.startsWith(BuildConfig.URL_CLINIC_TOP))  {
                view?.loadUrl(urlString)
            } else {
                view?.context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)))
            }
            return true
        }

        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        url?.let { urlString ->
            if (findNativePageOf(urlString)) {
                return true
            }

            if (overrideUrlLoading(urlString) || urlString.startsWith(BuildConfig.URL_CLINIC_TOP))  {
                view?.loadUrl(urlString)
            } else {
                view?.context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)))
            }
            return true
        }

        return super.shouldOverrideUrlLoading(view, url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        debugLogInfo("url = $url")
        script?.let {
            view?.evaluateJavascript(script) {
                onJSEvaluationFinished?.invoke()
            }
        }
    }

    private fun overrideUrlLoading(url: String?): Boolean {
        debugLogInfo("url = $url")
        return url?.startsWith(BuildConfig.BASE_URL) ?: false
    }

    private fun findNativePageOf(webPageUrl: String): Boolean {
        val str = webPageUrl.trim('/')
        debugLogInfo("url = $str")

        when (str) {
            BuildConfig.URL_LOGIN -> {
                AppManager.isLoggedIn = false
                navigationController.navigateToLogin()
                return true
            }
        }
        return false
    }
}