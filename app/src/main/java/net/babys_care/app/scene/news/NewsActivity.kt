package net.babys_care.app.scene.news

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseActivity
import kotlinx.android.synthetic.main.activity_about.toolbar_back_button
import kotlinx.android.synthetic.main.activity_news.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.R
import net.babys_care.app.models.realmmodels.NewsModel
import java.text.SimpleDateFormat
import java.util.*

class NewsActivity : BaseActivity() {

    override val layout: Int = R.layout.activity_news

    private var notificationBadge: TextView? = null
    private var currentSelectedTabPosition: Int = 0
    private var isInitial: Boolean = true
    private val onNewsDetailsShow = { news: NewsModel ->
        showNewsDetails(news)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(tool_bar)
        showUnreadNewsFragment()
        isInitial = false

        toolbar_back_button.setOnClickListener {
            onBackPressed()
        }
        unread_news_tab.setOnClickListener {
            showUnreadNewsFragment()
        }
        read_news_tab.setOnClickListener {
            showReadNewsFragment()
        }
    }

    private fun showReadNewsFragment() {
        if (currentSelectedTabPosition == 1) {
            return
        }
        val manager = supportFragmentManager
        while (manager.backStackEntryCount > 0) {
            manager.popBackStackImmediate()
        }
        val fragment = ReadNewsFragment().apply {
            onDetailShow = onNewsDetailsShow
        }
        navigate(fragment, R.id.news_body, false)
        unread_news_tab.setTextColor(ContextCompat.getColor(this, R.color.pinkishGrey))
        read_news_tab.setTextColor(ContextCompat.getColor(this, R.color.brownishGrey))
        currentSelectedTabPosition = 1
    }

    private fun showUnreadNewsFragment() {
        if (!isInitial) {
            if (currentSelectedTabPosition == 0) {
                return
            }
            val manager = supportFragmentManager
            while (manager.backStackEntryCount > 0) {
                manager.popBackStackImmediate()
            }
        }
        val fragment = UnreadNewsFragment().apply {
            onDetailShow = onNewsDetailsShow
        }
        navigate(fragment, R.id.news_body, false)
        read_news_tab.setTextColor(ContextCompat.getColor(this, R.color.pinkishGrey))
        unread_news_tab.setTextColor(ContextCompat.getColor(this, R.color.brownishGrey))
        currentSelectedTabPosition = 0
    }

    private fun showNewsDetails(news: NewsModel) {
        val detail = NewsDetailFragment()
        detail.news = news
        navigate(detail, R.id.news_body)
        unread_news_tab.visibility = View.GONE
        read_news_tab.visibility = View.GONE
        updateToolbarTitle(getString(R.string.notice_content))
    }

    private fun hideNewsDetails() {
        unread_news_tab.visibility = View.VISIBLE
        read_news_tab.visibility = View.VISIBLE
        updateToolbarTitle(getString(R.string.news_title))
    }

    private fun updateToolbarTitle(title: String) {
        toolbar_title_text.text = title
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

    fun getUnReadNoticeCount() {
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

    override fun onBackPressed() {
        val fragment =supportFragmentManager.findFragmentByTag(NewsDetailFragment::class.java.simpleName)
        if (fragment != null) {
            hideNewsDetails()
        }
        super.onBackPressed()
    }
}