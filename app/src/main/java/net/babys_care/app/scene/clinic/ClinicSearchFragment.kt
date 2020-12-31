package net.babys_care.app.scene.clinic

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_clinic_search.*
import net.babys_care.app.AppManager
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.NavigationController
import net.babys_care.app.utils.debugLogInfo

class ClinicSearchFragment : BaseFragment() {

    override val layout = R.layout.fragment_clinic_search

    private var loadingDialog: AlertDialog? = null
    private val headerFooterHideScript = """
                $('header.l-header').remove();
                $('footer.l-footer').remove();
                $('div.l-pageBody').css('padding-top', '0px');
                document.getElementsByClassName('breadcrumb-area')[0].style.display = 'none';
                $('div.c-block-01').css('padding-bottom','55px');
            """
    private lateinit var navigationController: NavigationController

    override fun onResume() {
        (activity as? MainActivity)?.updateToolbarTitle(getString(R.string.clinic_search_top_title))
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setWebView()
        loadWebView()
        setupBackPressHandler()
        navigationController = NavigationController(requireActivity())
        back_button.setOnClickListener {
            if (web_view.canGoBack()) {
                web_view.goBack()
            } else {
                updateBackButtonVisibility(false)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        web_view.settings.javaScriptEnabled = true
        web_view.settings.setSupportMultipleWindows(false)
        web_view.settings.javaScriptCanOpenWindowsAutomatically = false
        web_view.webViewClient = object : WebViewClient() {

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

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                updateBackButtonVisibility(false)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(headerFooterHideScript) {
                    showLoadingDialog(false)
                }
                updateBackButtonVisibility(web_view.canGoBack())
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

    private fun updateBackButtonVisibility(isVisible: Boolean) {
        if (isVisible) {
            back_button.visibility =View.VISIBLE
            if (web_view.canGoBackOrForward(2)) {
                back_button.text = getString(R.string.return_to_previous_screen)
            } else {
                back_button.text = getString(R.string.prefecture_select)
            }
        } else {
            back_button.visibility = View.GONE
        }
    }

    private fun loadWebView() {
        showLoadingDialog(true)
        web_view.loadUrl(BuildConfig.URL_CLINIC_TOP)
    }

    private fun showLoadingDialog(isShow: Boolean) {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }

        if (isShow) {
            loadingDialog = LoadingHelper().getLoadingDialog(requireContext())
            loadingDialog?.show()
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) {
                if (web_view.canGoBack()) {
                    web_view.goBack()
                } else {
                    if (isEnabled) {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
    }
}