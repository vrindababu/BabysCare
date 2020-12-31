package net.babys_care.app.scene.settings

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseActivity
import kotlinx.android.synthetic.main.activity_app_settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.R
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.scene.news.NewsActivity
import java.text.SimpleDateFormat
import java.util.*

class AppSettingsActivity : BaseActivity() {

    override val layout = R.layout.activity_app_settings

    private var notificationBadge: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(tool_bar)
        setRecyclerView()
        toolbar_back_button.setOnClickListener {
            onBackPressed()
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

    private fun setRecyclerView() {
        val dataList = listOf(getString(R.string.notification_japanese))
        app_settings_recycler.layoutManager = LinearLayoutManager(this)
        app_settings_recycler.setHasFixedSize(true)
        app_settings_recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        app_settings_recycler.adapter = AppSettingsAdapter(dataList).apply {
            onItemClickListener = {
                transitToNotificationSettings()
                showNotificationSettingsView(true)
            }
        }
    }

    /**
     * toggle the UI view for main contents and fragments
     * @param isShow boolean value to decide whether to show main contents or fragments
     */
    fun showNotificationSettingsView(isShow: Boolean) {
        if (isShow) {
            app_settings_recycler.visibility = View.GONE
            fragment_container.visibility = View.VISIBLE
            toolbar_title_text.text = getString(R.string.notification_japanese)
        } else {
            fragment_container.visibility = View.GONE
            app_settings_recycler.visibility = View.VISIBLE
            toolbar_title_text.text = getString(R.string.app_settings_title)
            fragment_container.removeAllViews()
            supportFragmentManager.popBackStack()
        }
    }

    private fun transitToNotificationSettings() {
        val settingsFragment = NotificationSettingsFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, settingsFragment, NotificationSettingsFragment::class.java.simpleName)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack(NotificationSettingsFragment::class.java.simpleName)
            .commit()
    }

    inner class AppSettingsAdapter(private val dataList: List<String>) : RecyclerView.Adapter<AppSettingViewHolder>() {

        var onItemClickListener: ((String) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppSettingViewHolder {
            return AppSettingViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_app_settings_recycler, parent, false))
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun onBindViewHolder(holder: AppSettingViewHolder, position: Int) {
            holder.settingText.text = dataList[position]
            holder.settingText.setOnClickListener {
                onItemClickListener?.invoke(dataList[position])
            }
        }
    }

    inner class AppSettingViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val settingText: TextView = itemView.findViewById(R.id.app_settings_text)
    }
}