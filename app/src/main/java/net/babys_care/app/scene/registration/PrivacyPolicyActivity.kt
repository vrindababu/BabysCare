package net.babys_care.app.scene.registration

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.webkit.WebView
import jp.winas.android.foundation.scene.BaseActivity
import kotlinx.android.synthetic.main.activity_privacy_policy.*
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.constants.AppConstants
import net.babys_care.app.scene.web.BabyCareWebClient
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.NavigationController

class PrivacyPolicyActivity : BaseActivity() {

    override val layout = R.layout.activity_privacy_policy

    private var loadingDialog: AlertDialog? = null
    private val headerFooterHideScript = """
        document.getElementsByTagName('header')[0].style.display = 'none';
        document.getElementsByClassName('breadcrumb-area')[0].style.display = 'none';
        document.getElementsByClassName('l-pageBody')[0].style.paddingTop = '5px';
        document.getElementsByTagName('footer')[0].style.display = 'none';
    """

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar_back_button.setOnClickListener {
            onBackPressed()
        }
        setWebView()
        toolbar_title_text.text = intent.getStringExtra("title") ?: getString(R.string.app_name)
        intent.getStringExtra(AppConstants.URL)?.let { url ->
            loadWebView(url)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        web_view.settings.javaScriptEnabled = true
        web_view.settings.setSupportMultipleWindows(false)
        web_view.settings.javaScriptCanOpenWindowsAutomatically = false
        web_view.webViewClient = BabyCareWebClient(NavigationController(this), headerFooterHideScript) {
            showLoadingDialog(false)
        }
    }

    private fun loadWebView(url: String) {
        showLoadingDialog(true)
        web_view.loadUrl(url)
    }

    private fun showLoadingDialog(isShow: Boolean) {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }

        if (isShow) {
            loadingDialog = LoadingHelper().getLoadingDialog(this)
            loadingDialog?.show()
        }
    }

    override fun onBackPressed() {
        if (web_view.canGoBack()) {
            web_view.goBack()
        } else {
            super.onBackPressed()
        }
    }
}