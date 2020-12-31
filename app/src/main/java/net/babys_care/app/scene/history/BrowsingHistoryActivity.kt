package net.babys_care.app.scene.history

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseActivity
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.activity_browsing_history.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.responses.Article
import net.babys_care.app.api.responses.FavouriteArticle
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.scene.news.NewsActivity
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.Toaster
import net.babys_care.app.utils.adapters.ArticleAdapter
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

class BrowsingHistoryActivity : BaseActivity(), ViewModelable<BrowsingHistoryViewModel> {

    override val layout = R.layout.activity_browsing_history
    override val viewModelClass = BrowsingHistoryViewModel::class

    private var loadingDialog: AlertDialog? = null
    private var notificationBadge: TextView? = null
    val adapter: ArticleAdapter by lazy {
        ArticleAdapter(this).apply {
            onItemClickListener = {article ->
                transitToArticleDetail(article)
                showDetailView(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setRecyclerView()

        getBrowsingHistory()
        getTags()
        getFavouriteArticles()
        getArticleList()

        toolbar_back_button.setOnClickListener {
            onBackPressed()
        }

        swipe_refresh.setOnRefreshListener {
            getArticleList(true)
        }
        setSupportActionBar(tool_bar)
    }

    override fun onResume() {
        if (notificationBadge != null) {
            getUnReadNoticeCount()
        }
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        val notificationItem = menu?.findItem(R.id.notification) ?: return true
        val actionView = notificationItem.actionView
        notificationBadge = actionView.findViewById(R.id.notification_count) as TextView

        actionView.setOnClickListener {
            onOptionsItemSelected(notificationItem)
        }

        getUnReadNoticeCount()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.notification) {
            navigate(Intent(this, NewsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setRecyclerView() {
        browsing_history_recycler.layoutManager = LinearLayoutManager(this)
        browsing_history_recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        browsing_history_recycler.adapter = adapter
        browsing_history_recycler.setHasFixedSize(true)
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

    private fun getArticleList(isFromSwipe: Boolean = false) {
        if (!isFromSwipe) {
            showLoadingDialog(true)
        }
        viewModel?.fetchArticleList(AppManager.apiToken) { response, error ->
            if (isFromSwipe) {
                swipe_refresh.isRefreshing = false
            } else {
                showLoadingDialog(false)
            }
            if (response != null) {
                filterArticle()
            } else {
                showNoData(false)
                error?.message?.let {
                    Toaster.showToast(it)
                }
            }
        }
    }

    private fun filterArticle() {
        val browsingHistories = viewModel?.browsingHistories ?: mutableListOf()
        val articles = viewModel?.articleList ?: mutableListOf()
        val filteredArticleList = mutableListOf<Article>()
        browsingHistories.sortByDescending { it.created_at }
        for (history in browsingHistories) {
            for (article in articles) {
                if (article.articleId == history.article_id) {
                    filteredArticleList.add(article)
                    break
                }
            }
        }

        viewModel?.articleList?.clear()
        viewModel?.articleList?.addAll(filteredArticleList)

        val favouriteArticleIds = viewModel?.favouriteArticleIds ?: mutableListOf()
        if (favouriteArticleIds.size > 0) {
            setFavouriteArticle(favouriteArticleIds)
        } else {
            if (filteredArticleList.size > 0) {
                viewModel?.tagList?.let {
                    adapter.setTagData(it)
                }
                adapter.setArticleData(filteredArticleList)
                showNoData(true)
            } else {
                showNoData(false)
            }
        }

    }

    private fun getTags() {
        viewModel?.fetchTags(AppManager.apiToken)
    }

    private fun getFavouriteArticles() {
        viewModel?.fetchFavouriteArticleIds(AppManager.apiToken) { response, error ->
            response?.data?.favorites?.let { list ->
                if (viewModel?.articleList?.size ?: 0 > 0) {
                    setFavouriteArticle(list)
                }
            } ?: kotlin.run {
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun getBrowsingHistory() {
        viewModel?.fetchBrowsingHistory(AppManager.apiToken)
    }

    private fun setFavouriteArticle(list: List<FavouriteArticle>) {
        val articleList = viewModel?.articleList ?: return
        for (favourite in list) {
            for (article in articleList) {
                if (article.articleId == favourite.article_id) {
                    article.isFavourite = true
                    break
                }
            }
        }

        viewModel?.tagList?.let {
            adapter.setTagData(it)
        }

        adapter.setArticleData(articleList)
        if (articleList.isEmpty()) {
            showNoData(false)
        } else {
            showNoData(true)
        }
    }

    private fun showNoData(hasData: Boolean) {
        if (hasData) {
            no_data.visibility = View.GONE
            swipe_refresh.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        } else {
            no_data.visibility = View.VISIBLE
            swipe_refresh.visibility = View.GONE
        }
    }

    fun showDetailView(isDetails: Boolean) {
        if (isDetails) {
            swipe_refresh.visibility = View.GONE
            fragment_container.visibility = View.VISIBLE
        } else {
            fragment_container.visibility = View.GONE
            swipe_refresh.visibility = View.VISIBLE
            fragment_container.removeAllViews()
            supportFragmentManager.popBackStack()
        }
    }

    private fun transitToArticleDetail(article: Article) {
        val detailFragment = ArticleDetailFragment()
        detailFragment.article = article
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, detailFragment, ArticleDetailFragment::class.java.simpleName)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack(ArticleDetailFragment::class.java.simpleName)
            .commit()
    }

    private fun getUnReadNoticeCount() {
        launch {
            val dateFormatter1 = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            val dateFormatter2 = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
            val dateFormatter3 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.JAPAN)
            val dateFormatter4 = SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss", Locale.JAPAN)
            val currentDate = Date()
            var count = 0
            val realm = Realm.getDefaultInstance()
            val newsData = realm.where<NewsModel>().findAll()
            for (news in newsData) {
                if (news.isRelease == 0 || news.isRead == 1) {
                    continue
                }
                val formatter = when {
                    news.releaseStartAt.contains("/") && news.releaseStartAt.contains("T") -> {
                        dateFormatter4
                    }
                    news.releaseStartAt.contains("-") && news.releaseStartAt.contains("T") -> {
                        dateFormatter3
                    }
                    news.releaseStartAt.contains("/") -> {
                        dateFormatter1
                    }
                    else -> {
                        dateFormatter2
                    }
                }
                val newsStartDate = formatter.parse(news.releaseStartAt) ?: Date()
                if (currentDate.before(newsStartDate)) {
                    continue
                }
                val releaseEnd = news.releaseEndAt
                if (!releaseEnd.isNullOrEmpty()) {
                    val newsEndDate = formatter.parse(releaseEnd) ?: Date()
                    if (newsEndDate.before(currentDate)) {
                        continue
                    }
                }
                count++
            }

            realm.close()

            launch(Dispatchers.Main) {
                updateBadge(count)
            }
        }
    }

    private fun updateBadge(count: Int) {
        if (count > 0) {
            notificationBadge?.text = if (count > 99) "99+" else "$count"
            if (notificationBadge?.visibility == View.GONE) {
                notificationBadge?.visibility = View.VISIBLE
            }
        } else {
            notificationBadge?.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onDestroy()
    }
}