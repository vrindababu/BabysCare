package net.babys_care.app.scene.news

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import jp.winas.android.foundation.extension.onChanged
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_read_news.*
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.responses.News
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.utils.DialogHelper
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

class ReadNewsFragment : BaseFragment(), ViewModelable<ReadNewsViewModel> {

    override val layout = R.layout.fragment_read_news
    override val viewModelClass = ReadNewsViewModel::class

    private lateinit var realm: Realm
    private val newsData: MutableList<NewsModel> = mutableListOf()
    private val adapter by lazy {
        NewsAdapter().apply {
            onItemClick = { news, _ ->
                onDetailShow?.invoke(news)
            }
        }
    }
    var onDetailShow: ((NewsModel) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        realm = Realm.getDefaultInstance()
        setRecyclerView()
        getReadNewsList()

        viewModel?.newsData?.onChanged(viewLifecycleOwner) {response ->
            response?.data?.news?.let { list ->
                createNewsList(list)
            } ?: kotlin.run {
                if (!AppManager.isLoggedIn) {
                    showDialogAndTransitToLogin(getString(R.string.authentication_error_ea002))
                }
            }
        }
        viewModel?.readNewsLiveData?.onChanged(viewLifecycleOwner) {response ->
            if (response?.data?.read_news?.isNotEmpty() == true) {
                if (viewModel?.newsData?.value?.data?.news?.isNotEmpty() == true) {
                    setReadStatus()
                }
            } else if (!AppManager.isLoggedIn) {
                showDialogAndTransitToLogin(getString(R.string.authentication_error_ea002))
            }
        }

        getNews()
        swipe_refresh.setOnRefreshListener {
            getNews(true)
        }
    }

    private fun setRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        read_news_list.layoutManager = layoutManager
        read_news_list.adapter = adapter
        read_news_list.setHasFixedSize(true)
        val separateLine = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        read_news_list.addItemDecoration(separateLine)
    }

    private fun getNews(isFromSwipe: Boolean = false) {
        viewModel?.getNewsList(AppManager.apiToken){isSuccess, error ->
            if (isFromSwipe) {
                swipe_refresh.isRefreshing = false
            }
            if (!isSuccess) {
                if (!AppManager.isLoggedIn) {
                    showDialogAndTransitToLogin(getString(R.string.authentication_error_ea002))
                }
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun getReadNewsList() {
        viewModel?.getReadNewsList(AppManager.apiToken)
    }

    private fun setReadStatus() {
        val readNewsIdList = viewModel?.readNewsLiveData?.value?.data?.read_news ?: listOf()

        if (newsData.isEmpty()) {
            return
        }

        for (newsId in readNewsIdList) {
            for (news in newsData) {
                if (news.newsId == newsId) {
                    news.isRead = 1
                    break
                }
            }
        }
        filterData()
    }

    private fun createNewsList(list: List<News>) {
        newsData.clear()
        for (item  in list) {
            val newsModel = NewsModel(item.newsId, item.title, item.releaseStartAt, item.releaseEndAt, item.isRelease, item.listImage, 0)
            newsData.add(newsModel)
        }

        if (viewModel?.readNewsLiveData?.value?.data?.read_news?.isNotEmpty() == true) {
            setReadStatus()
        } else {
            filterData()
        }
    }

    private fun filterData() {
        val currentDate = Date()
        val readNews = mutableListOf<NewsModel>()
        for (news in newsData) {
            if (news.isRelease == 0 || news.isRead == 0) {
                continue
            }
            val formatter = getDateFormatter(news)
            val newsStartDate = formatter.parse(news.releaseStartAt) ?: Date()
            if (currentDate.before(newsStartDate)) {
                continue
            }
            if (news.releaseEndAt != null) {
                val newsEndDate = formatter.parse(news.releaseEndAt!!) ?: Date()
                if (newsEndDate.before(currentDate)) {
                    continue
                }
            }
            readNews.add(news)
        }
        readNews.sortByDescending {
            val formatter = getDateFormatter(it)
            formatter.parse(it.releaseStartAt)
        }
        adapter.setData(readNews)
        if (readNews.isEmpty()) {
            showNoData(true)
        } else {
            showNoData(false)
            adapter.notifyDataSetChanged()
            saveDataToLocalDB(newsData)
        }
    }

    private fun getDateFormatter(news: NewsModel): SimpleDateFormat {
        return when {
            news.releaseStartAt.contains("/") && news.releaseStartAt.contains("T") -> {
                SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss", Locale.JAPAN)
            }
            news.releaseStartAt.contains("-") && news.releaseStartAt.contains("T") -> {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.JAPAN)
            }
            news.releaseStartAt.contains("/") -> {
                SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            }
            else -> {
                SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
            }
        }
    }

    private fun saveDataToLocalDB(newsList: MutableList<NewsModel>) {
        realm.executeTransactionAsync { realmBg ->
            for (news in newsList) {
                try {
                    realmBg.insertOrUpdate(news)
                } catch (ex: Exception) {}
            }
        }
    }

    private fun showNoData(isShow: Boolean) {
        if (isShow) {
            swipe_refresh.visibility = View.GONE
            no_notice.text =  String.format(getString(R.string.no_notice), "既読")
            no_notice.visibility = View.VISIBLE
        } else {
            no_notice.visibility = View.GONE
            swipe_refresh.visibility = View.VISIBLE
        }
    }

    private fun showDialogAndTransitToLogin(message: String) {
        val activity = activity ?: return
        DialogHelper().showLoginTransitionDialog(activity, message, true)
    }

    override fun onDestroyView() {
        realm.close()
        super.onDestroyView()
    }
}