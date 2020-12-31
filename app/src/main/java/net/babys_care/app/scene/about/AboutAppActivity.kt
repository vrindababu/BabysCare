package net.babys_care.app.scene.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseActivity
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.R
import net.babys_care.app.extensions.addDividerItem
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.scene.news.NewsActivity
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

class AboutAppActivity : BaseActivity() {

    override val layout: Int = R.layout.activity_about

    private var notificationBadge: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(tool_bar)
        val items = resources.getStringArray(R.array.about_app_items)
        val urls = resources.getStringArray(R.array.about_app_urls)
        val adapter = AboutAppAdapter(items)
        val layoutManager = LinearLayoutManager(this)

        about_app_body.layoutManager = layoutManager
        about_app_body.adapter = adapter
        about_app_body.setHasFixedSize(true)
        about_app_body.addDividerItem(this)

        adapter.setOnItemClickListener(object:AboutAppAdapter.OnItemClickListener{
            override fun onItemClickListener(view: View, position: Int, clickedText: String) {
                if (position != items.lastIndex) browserOpenConfirmationDialog(urls[position])
            }
        })
        toolbar_back_button.setOnClickListener {
            onBackPressed()
        }
    }

    private fun openBrowser(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (ex: Exception) {
            debugLogInfo("Exception: $ex")
        }
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

    private fun browserOpenConfirmationDialog(url: String) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.start_external_browser))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                openBrowser(url)
            }
            .show()
    }
}