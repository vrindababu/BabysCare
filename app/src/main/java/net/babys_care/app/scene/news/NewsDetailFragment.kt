package net.babys_care.app.scene.news

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_news_detail.*
import net.babys_care.app.AppManager
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.api.responses.NewsDetail
import net.babys_care.app.extensions.toDotDateFormat
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.debugLogInfo

class NewsDetailFragment : BaseFragment(), ViewModelable<NewsDetailViewModel> {

    override val layout = R.layout.fragment_news_detail
    override val viewModelClass = NewsDetailViewModel::class
    private var loadingDialog: AlertDialog? = null
    var news: NewsModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        news?.let {
            getNewsDetails(it.newsId)
        } ?: kotlin.run {
            activity?.onBackPressed()
        }
    }

    private fun getNewsDetails(newsId: Int) {
        showLoadingDialog(true)
        viewModel?.getNoticeDetails(newsId, AppManager.apiToken) {response, error ->
            showLoadingDialog(false)
            if (response != null) {
                response.data?.let { newsDetail ->
                    showNoticeDetails(newsDetail)
                } ?: kotlin.run {
                    activity?.onBackPressed()
                }
            } else {
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun showNoticeDetails(newsDetail: NewsDetail) {
        val context = context ?: return
        title.text = newsDetail.title
        datetime.text = newsDetail.release_start_at.toDotDateFormat()
        content.text = newsDetail.content
        newsDetail.detail_image?.let { imageUrl ->
            Glide.with(context).load("${BuildConfig.URL_IMAGE_DIRECTORY}$imageUrl").listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    notice_image.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    try {
                        notice_image.visibility = View.VISIBLE
                    } catch (ex: Exception) {}
                    return false
                }
            }).into(notice_image)
        } ?: kotlin.run {
            notice_image.visibility = View.GONE
        }
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

    override fun onStop() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onStop()
    }
}