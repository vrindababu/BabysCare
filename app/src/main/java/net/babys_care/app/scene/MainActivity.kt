package net.babys_care.app.scene

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseActivity
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.models.NavigationDrawerModel
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.about.AboutAppActivity
import net.babys_care.app.scene.article.ArticleFragment
import net.babys_care.app.scene.clinic.ClinicSearchFragment
import net.babys_care.app.scene.contact.ContactActivity
import net.babys_care.app.scene.favourite.FavouriteFragment
import net.babys_care.app.scene.history.BrowsingHistoryActivity
import net.babys_care.app.scene.home.HomeFragment
import net.babys_care.app.scene.initial.TutorialActivity
import net.babys_care.app.scene.login.LoginActivity
import net.babys_care.app.scene.news.NewsActivity
import net.babys_care.app.scene.settings.AccountSettingsActivity
import net.babys_care.app.scene.settings.AppSettingsActivity
import net.babys_care.app.scene.trouble.TroubleFragment
import net.babys_care.app.utils.Toaster.showToast
import net.babys_care.app.utils.adapters.NavigationDrawerAdapter
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity(), ViewModelable<MainViewModel> {

    override val layout = R.layout.activity_main
    override val viewModelClass = MainViewModel::class

    private var notificationBadge: TextView? = null
    private lateinit var drawer: DrawerLayout
    private lateinit var realm: Realm
    private var user: UserModel? = null
    private var currentlySelectedItemId: Int = R.id.home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()

        setSupportActionBar(top_app_bar)
        drawer = findViewById(R.id.drawer_layout)
        val drawerToggle = object : ActionBarDrawerToggle(this, drawer, top_app_bar, (R.string.open), (R.string.close)){}
        drawer.addDrawerListener(drawerToggle)
        drawerToggle.isDrawerIndicatorEnabled = false
        drawerToggle.syncState()
        top_app_bar.setNavigationOnClickListener {
            toggleDrawer()
        }

        bottom_navigation_bar.setOnNavigationItemSelectedListener { item->
            if (currentlySelectedItemId == item.itemId) {
                return@setOnNavigationItemSelectedListener true
            } else {
                clearBackStack()
                currentlySelectedItemId = item.itemId
            }
            when(item.itemId) {
                R.id.home -> {
                    //Display home content
                    navigate(HomeFragment(), R.id.fragment_container, false)
                    true
                }
                R.id.trouble -> {
                    //Display trouble content
                    navigate(TroubleFragment(), R.id.fragment_container, false)
                    updateToolbarTitle(getString(R.string.solve_problems))
                    true
                }
                R.id.article -> {
                    //Display article content
                    navigate(ArticleFragment(), R.id.fragment_container, false)
                    true
                }
                R.id.clinic -> {
                    //Display clinic search option
                    navigate(ClinicSearchFragment(), R.id.fragment_container, false)
                    true
                }
                R.id.favourite -> {
                    //Display favourite content
                    navigate(FavouriteFragment(), R.id.fragment_container, false)
                    true
                }
                else -> false
            }
        }

        //Set home screen as default
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, HomeFragment(), HomeFragment::class.simpleName)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()

        user = getUserInfoFromLocalDb()
        setNavHeader()
        setNavItem()
    }

    fun setNavHeader() {
        if (user == null) {
            showErrorAndTransitToLogin(getString(R.string.user_info_not_found))
        } else {
            val userNameTextView = header.findViewById(R.id.nav_header_user_name) as? TextView
            userNameTextView?.text = String.format(getString(R.string.user_name), user?.firstName)
            val userImage = header.findViewById(R.id.nav_header_profile_image) as ImageView

            val defaultImage = if (user?.gender == "male") {
                ContextCompat.getDrawable(this, R.drawable.default_profile_male)
            } else {
                ContextCompat.getDrawable(this, R.drawable.default_profile_female)
            }

            user?.image?.let { imageUrl ->
                Glide.with(this).load("${BuildConfig.URL_IMAGE_DIRECTORY}$imageUrl").apply(RequestOptions().placeholder(defaultImage)).into(userImage)
            } ?: kotlin.run {
                userImage.setImageDrawable(defaultImage)
            }

            val closeButton = header.findViewById(R.id.close_drawer) as ImageView
            closeButton.setOnClickListener(null)
            closeButton.setOnClickListener {
                toggleDrawer()
            }
        }
    }

    private fun setNavItem() {
        drawer_item_recycler.layoutManager = LinearLayoutManager(this)
        drawer_item_recycler.setHasFixedSize(true)
        val drawerItems = getDrawerItem()
        drawer_item_recycler.adapter = NavigationDrawerAdapter(drawerItems).apply {
            onItemClick = {position ->
                handleDrawerItemClick(position)
            }
        }
    }

    private fun handleDrawerItemClick(position: Int) {
        when(position) {
            0 -> {
                navigate(Intent(this, AccountSettingsActivity::class.java), 100)
            }
            1 -> {
                navigate(Intent(this, BrowsingHistoryActivity::class.java))
            }
            2 -> {
                navigate(Intent(this, AppSettingsActivity::class.java))
            }
            3 -> {
                gotoHelpPage()
            }
            4 -> {
                shareAppLink()
            }
            5 -> {
                reviewTheApp()
            }
            6 -> {
                navigate(Intent(this, AboutAppActivity::class.java))
            }
            7 -> {
                navigate(Intent(this, ContactActivity::class.java))
            }
            8 -> {
                showLogoutDialog()
            }
        }
        toggleDrawer()
    }

    private fun getDrawerItem(): List<NavigationDrawerModel> {
        return listOf(
            NavigationDrawerModel(R.drawable.icon_account, getString(R.string.account)),
            NavigationDrawerModel(R.drawable.icon_history, getString(R.string.browse_history)),
            NavigationDrawerModel(R.drawable.icon_settings, getString(R.string.setting)),
            NavigationDrawerModel(R.drawable.icon_faq, getString(R.string.help)),
            NavigationDrawerModel(R.drawable.icon_friends, getString(R.string.share)),
            NavigationDrawerModel(R.drawable.icon_evaluate, getString(R.string.evaluate)),
            NavigationDrawerModel(R.drawable.icon_app, getString(R.string.about)),
            NavigationDrawerModel(R.drawable.icon_contact, getString(R.string.contact)),
            NavigationDrawerModel(R.drawable.icon_logout, getString(R.string.logout))
        )
    }

    private fun getUserInfoFromLocalDb(): UserModel? {
        return realm.where(UserModel::class.java).findFirst()
    }

    private fun showErrorAndTransitToLogin(message: String) {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("isFromInitial", true)
                navigate(intent)
                finishAffinity()
            }
            .show()
    }

    /**
     * Changes the title of toolbar according to current screen
     * @param title that will be shown
     */
    fun updateToolbarTitle(title: String) {
        toolbar_title.text = title
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

    private fun toggleDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    fun addBackButtonAndActionToMain(isAdd: Boolean) {
        if (isAdd) {
            top_app_bar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_back_30)
            top_app_bar.setNavigationOnClickListener {
                onBackPressed()
            }
        } else {
            top_app_bar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu_24)
            top_app_bar.setNavigationOnClickListener {
                toggleDrawer()
            }
        }
    }

    private fun showLogoutDialog() {
        val dialog = AlertDialog.Builder(this)
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_custom_dialog, null, false)
        dialog.setView(layout)

        val alert = dialog.create()
        layout.findViewById<TextView>(R.id.title).text = getString(R.string.logout)
        layout.findViewById<TextView>(R.id.message).text = getString(R.string.want_to_logout)
        layout.findViewById<Button>(R.id.ok_button).text = getString(R.string.logout)
        layout.findViewById<Button>(R.id.ok_button).setOnClickListener {
            alert.dismiss()
            logoutFromServer()
        }
        layout.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            alert.dismiss()
        }

        alert.show()
        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun getUnReadNoticeCount() {
        launch {
            val dateFormatter1 = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            val dateFormatter2 = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
            val dateFormatter3 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.JAPAN)
            val dateFormatter4 = SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss", Locale.JAPAN)
            val currentDate = Date()
            var count = 0
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

            launch(Dispatchers.Main) {
                updateBadge(count)
            }
        }
    }

    private fun logoutFromServer() {
        viewModel?.logoutUser(AppManager.apiToken) {response, error ->
            if (response != null) {
                AppManager.isLoggedIn = false
                realm.executeTransactionAsync {realmBg ->
                    realmBg.deleteAll()
                }
                showToast(response.data.message ?: "ログアウトしました。")
                navigate(Intent(this, TutorialActivity::class.java))
                finishAffinity()
            } else {
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun reviewTheApp() {
        val manager = if (BuildConfig.FLAVOR == "prod") {
            ReviewManagerFactory.create(this)
        } else {
            FakeReviewManager(this)
        }
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener {requestListener ->
            if (requestListener.isSuccessful) {
                val reviewInfo = requestListener.result
                manager.launchReviewFlow(this, reviewInfo)
            } else {
                openGooglePlay()
            }
        }
    }

    private fun openGooglePlay() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (ex: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun gotoHelpPage() {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.start_external_browser))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                try {
                    val url = resources.getStringArray(R.array.about_app_urls)[3]
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                } catch (ex: Exception) {
                    debugLogInfo("Exception: $ex")
                }
            }
            .show()
    }

    private fun shareAppLink() {
        val title = getString(R.string.app_share_message)
        val message = "$title\n\nhttps://play.google.com/store/apps/details?id=$packageName"
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, title)
        startActivity(shareIntent)
    }

    private fun clearBackStack() {
        val manager = supportFragmentManager
        while (manager.backStackEntryCount > 0) {
            manager.popBackStackImmediate()
        }
    }

    /**
     * function for selecting bottom navigation item dynamically
     * @param tabId is the of bottom navigation item (tab)
     */
    fun selectTab(tabId: Int) {
        when(tabId) {
            R.id.home, R.id.trouble, R.id.article, R.id.clinic, R.id.favourite -> {
                bottom_navigation_bar.selectedItemId = tabId
            }
        }
    }

    override fun onResume() {
        if (notificationBadge != null) {
            getUnReadNoticeCount()
        }
        if (AppSetting(this).userInfoUpdated == true) {
            AppSetting(this).userInfoUpdated = null
            setNavHeader()
        }
        super.onResume()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100) {
            if (resultCode == RESULT_UPDATED) {
                val fragment = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName)
                if (fragment != null && fragment is HomeFragment) {
                    fragment.refreshBabyInfo()
                }
                if (AppSetting(this).babyInfoChanged == true) {
                    AppSetting(this).babyInfoChanged = null
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        realm.close()

        super.onDestroy()
    }

    companion object {
        const val RESULT_UPDATED: Int = 111
    }
}