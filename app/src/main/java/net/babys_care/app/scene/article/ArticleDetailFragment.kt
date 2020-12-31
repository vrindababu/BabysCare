package net.babys_care.app.scene.article

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import jp.winas.android.foundation.extension.onChanged
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_article_detail.*
import net.babys_care.app.AppManager
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.api.responses.Article
import net.babys_care.app.api.responses.Author
import net.babys_care.app.api.responses.Tag
import net.babys_care.app.extensions.toDotDateFormat
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.scene.favourite.FavouriteFragment
import net.babys_care.app.scene.history.BrowsingHistoryActivity
import net.babys_care.app.scene.trouble.*
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.adapters.TagAdapter
import net.babys_care.app.utils.debugLogInfo

class ArticleDetailFragment : BaseFragment(), ViewModelable<ArticleDetailViewModel> {

    override val layout = R.layout.fragment_article_detail
    override val viewModelClass = ArticleDetailViewModel::class

    private var loadingDialog: AlertDialog? = null
    private val script: String = """
        document.getElementsByClassName('l-header')[0].remove();
        document.getElementsByClassName('breadcrumb-area')[0].remove();
        document.getElementsByClassName('c-btn-more c-btn-archive')[0].remove();
        document.getElementsByClassName('l-footer')[0].remove();
        document.getElementsByClassName('c-block-01')[0].remove();
        document.getElementsByClassName('c-block-01')[0].remove();
        document.getElementsByClassName('c-block-01')[0].remove();
        document.getElementsByClassName('c-head c-archive-head')[0].remove();
        document.getElementsByClassName('l-pageBody')[0].style["paddingTop"] = "0px";
        document.getElementsByClassName('c-archive')[0].style["paddingBottom"] = "30px";
    """.trimIndent()
    var article: Article? = null
    private var isShowNative: Boolean = true

    override fun onStart() {
        (activity as? MainActivity)?.addBackButtonAndActionToMain(true)
        updateTabLayoutVisibility(true)
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        article?.let {

            setupObserver()
            setWebView()

            loadWebView(it.link)
            getTags()
            getFavouriteArticleIds()
            getAuthorData()

            (activity as? MainActivity)?.updateToolbarTitle(it.title)

            fab_favourite.setOnClickListener {
                toggleFavourite()
            }
            createBrowsingHistory()
        } ?: kotlin.run {
            activity?.onBackPressed()
        }
    }

    private fun setupObserver() {
        viewModel?.authorLiveData?.onChanged(viewLifecycleOwner) {list ->
            if (!list.isNullOrEmpty()) {
                setAuthorData(list)
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

            override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
                if (BuildConfig.FLAVOR == "dev" || BuildConfig.FLAVOR == "demo") {
                    handler?.proceed("winas-akachan", "n5Wf4CK0LK")
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.toString()?.let { urlString ->
                    if (article?.link == urlString) {
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
                    if (article?.link == urlString) {
                        view?.loadUrl(urlString)
                    } else {
                        view?.context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)))
                    }

                    return true
                }

                return super.shouldOverrideUrlLoading(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                debugLogInfo("url: $url")
                view?.evaluateJavascript("""javascript:window.Android.processHtml(document.getElementsByClassName('404')[0].textContent);""", null)
                view?.evaluateJavascript(script) {
                    showLoadingDialog(false)
                    showArticleNativeInfo()
                }
            }
        }
        web_view.addJavascriptInterface(JavaScriptInterface(), "Android")
    }

    inner class JavaScriptInterface {
        //This method is being called from webView
        @JavascriptInterface
        fun processHtml(html: String) {
            if (html == "404") {
                isShowNative = false
            }
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
            loadingDialog = LoadingHelper().getLoadingDialog(requireContext())
            loadingDialog?.show()
        }
    }

    private fun showFavouriteButton(isFavourite: Boolean) {
        if (isFavourite) {
            fab_favourite.supportImageTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
            fab_favourite.setImageResource(R.drawable.ic_favorite_filled_24)
        } else {
            fab_favourite.supportImageTintList = ContextCompat.getColorStateList(requireContext(), R.color.pinkishGrey)
            fab_favourite.setImageResource(R.drawable.ic_favorite_24)
        }
        if (fab_favourite.visibility != View.VISIBLE) {
            fab_favourite.visibility = View.VISIBLE
        }
    }

    private fun toggleFavourite() {
        val article = article ?: return
        if (article.isFavourite) {
            viewModel?.deleteFavourite(AppManager.apiToken, article.articleId){response, error ->
                if (response == null) {
                    article.isFavourite = !article.isFavourite
                    debugLogInfo("Error: $error")
                }
            }
        } else {
            viewModel?.createFavourite(AppManager.apiToken, article.articleId){response, error ->
                if (response == null) {
                    article.isFavourite = !article.isFavourite
                    debugLogInfo("Error: $error")
                }
            }
        }
        article.isFavourite = !article.isFavourite
        showFavouriteButton(article.isFavourite)
    }

    private fun getTags() {
        viewModel?.fetchTags(AppManager.apiToken)
    }

    private fun getFavouriteArticleIds() {
        viewModel?.fetchFavouriteArticleIds(AppManager.apiToken)
    }

    private fun getAuthorData() {
        viewModel?.fetchAuthor(AppManager.apiToken)
    }

    private fun createBrowsingHistory() {
        val articleId = article?.articleId ?: return
        viewModel?.createBrowsingHistory(AppManager.apiToken, articleId)
    }

    private fun updateTabLayoutVisibility(isHidden: Boolean) {
        when(val fragment = parentFragment){
            is ExerciseFragment -> fragment.hideTabLayout(isHidden)
            is MealFragment -> fragment.hideTabLayout(isHidden)
            is SymptomSearchFragment -> fragment.hideTabLayout(isHidden)
            is ExcretionFragment -> fragment.hideTabLayout(isHidden)
            is SleepFragment -> fragment.hideTabLayout(isHidden)
        }
    }

    private fun setAuthorData(authorList: List<Author>) {
        for (author in authorList) {
            if (author.authorId == article?.author) {
                writer_name.text = author.name
                Glide.with(requireContext()).load(author.avatarUrl).into(writer_image)
                break
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showArticleNativeInfo() {
        if (!isShowNative) return
        article_info_container.visibility = View.VISIBLE
        viewModel?.tagList?.let { list ->
            val filteredTags = mutableListOf<Tag>()
            val tagList = article?.tagIds ?: listOf()
            for (tagId in tagList) {
                for (tag in list) {
                    if (tag.tagId == tagId) {
                        filteredTags.add(tag)
                        break
                    }
                }
            }
            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.FLEX_START
            tag_recycler_view.layoutManager = layoutManager
            tag_recycler_view.adapter = TagAdapter(filteredTags)
        }
        article_view_count.text = "${article?.view ?: 0}view"
        article_date.text = article?.date?.toDotDateFormat()
        showFavouriteButton(article?.isFavourite == true)
    }

    override fun onDestroyView() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onDestroyView()
    }

    override fun onStop() {
        updateTabLayoutVisibility(false)
        (activity as? MainActivity)?.addBackButtonAndActionToMain(false)
        (activity as? BrowsingHistoryActivity)?.showDetailView(false)
        (parentFragment as? FavouriteFragment)?.showDetailView(false)
        super.onStop()
    }
}