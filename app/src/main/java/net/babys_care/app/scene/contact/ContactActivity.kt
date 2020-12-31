package net.babys_care.app.scene.contact

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseActivity
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.scene.initial.TutorialActivity
import net.babys_care.app.scene.news.NewsActivity
import java.text.SimpleDateFormat
import java.util.*

class ContactActivity : BaseActivity() {

    override val layout: Int = R.layout.activity_contact

    private val needle = "babys-care.net/contact"
    private val editViewItems = arrayOf(
        "document.getElementsByTagName('header')[0].style.display = 'none';",
        "document.getElementsByClassName('breadcrumb-area')[0].style.display = 'none';",
        "document.getElementsByClassName('l-pageBody')[0].style.paddingTop = '5px';",
        "document.getElementsByName('submitConfirm')[0].style.marginBottom = '50px';",
        "document.getElementsByTagName('footer')[0].style.display = 'none';"
    )
    private val editButtonItems by lazy { arrayOf(
        "document.getElementsByClassName('c-btn-center')[0].children[0].text = '${getString(R.string.to_home_button)}';",
        "document.getElementsByClassName('c-btn-center')[0].children[0].href='#';",
        "document.getElementsByClassName('c-btn-center')[0].children[0].style.color = '${getColorCode(R.color.whiteTwo)}';",
        "document.getElementsByClassName('c-btn-center')[0].children[0].style.border = '${getColorCode(R.color.red)}';",
        "document.getElementsByClassName('c-btn-center')[0].children[0].style.backgroundColor = '${getColorCode(R.color.red)}';"
    ) }
    private var notificationBadge: TextView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(tool_bar)
        contact_body.settings.javaScriptEnabled = true
        contact_body.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String) {
                super.onPageFinished(view, url)
                for (item in editViewItems) view?.evaluateJavascript(item, null)
                if (url == BuildConfig.URL_CONTACT_COMP) {
                    for (item in editButtonItems) view?.evaluateJavascript(item, null)
                }
                //TODO:TutorialではなくHomeに変更(Home未実装のため)
                if (url.contains("#")) navigateAsNewTask(Intent(view?.context, TutorialActivity::class.java))
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request!!.url.toString()
                return if (url.contains(needle)) {
                    false
                } else {
                    view?.context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                }
            }
        }
        contact_body.loadUrl(BuildConfig.URL_CONTACT_TOP)

        toolbar_back_button.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getColorCode(resource: Int): String {
        return java.lang.String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, resource))
    }

    override fun onResume() {
        getUnReadNoticeCount()
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
}
