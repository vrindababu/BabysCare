package net.babys_care.app.scene.initial

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.google.firebase.messaging.FirebaseMessaging
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.extension.onChanged
import jp.winas.android.foundation.scene.BaseActivity
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.R
import net.babys_care.app.api.responses.ChildData
import net.babys_care.app.api.responses.GrowthHistory
import net.babys_care.app.api.responses.News
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.models.realmmodels.GrowthHistories
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.scene.login.LoginActivity
import net.babys_care.app.utils.Encrypt
import net.babys_care.app.utils.NotificationBootReceiver
import java.text.SimpleDateFormat
import java.util.*

class SplashActivity : BaseActivity(), ViewModelable<InitialViewModel> {

    override val layout: Int = R.layout.activity_splash
    override val viewModelClass = InitialViewModel::class
    private lateinit var realm: Realm
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()

        if (AppManager.sharedPreference.getBoolean("is_first_run", true)) {
            Handler().postDelayed({
                navigateAsNewTask(Intent(this, TutorialActivity::class.java))
            }, 1000)
        } else {
            deleteExistingGrowthInfo()
            getUserCredentialFromLocalDB()
            setupObservers()
            setAlarmForNotification()
        }
    }

    private fun setupObservers() {
        viewModel?.userInfoLiveData?.onChanged(this) {response ->
            if (response != null && response.result) {

                val data = response.data

                val user = UserModel(data.user_id, data.email, password, data.user_type, data.status,
                AppManager.apiToken, data.parent_id, data.first_name, data.last_name,
                    data.first_name_kana, data.last_name_kana, data.image, data.gender, data.birth_day,
                data.postal_code, data.prefecture, data.city, data.building, data.is_notifiable_local,
                data.is_notifiable_remote, data.is_premama)

                saveUserInfoIntoLocalDb(user, data.children)

                if (viewModel?.growthHistoryLiveData?.value == null) {
                   transitToHomeScreen(1500)
                } else {
                    transitToHomeScreen(1000)
                }
            }
        }

        viewModel?.growthHistoryLiveData?.onChanged(this) {response ->
            response?.data?.growth_histories?.let { list ->
                if (list.isNotEmpty()) {
                    saveGrowthDataToLocal(list)
                }
            }
        }

        viewModel?.newsData?.onChanged(this) {response ->
            response?.data?.news?.let { list ->
                createNewsList(list)
            }
        }

        viewModel?.readNewsLiveData?.onChanged(this) {response ->
            val readIdList = response?.data?.read_news ?: listOf()
            if (readIdList.isNotEmpty()) {
                val newsList = viewModel?.newsData?.value?.data?.news ?: listOf()
                if (newsList.isNotEmpty()) {
                    createNewsList(newsList)
                }
            }
        }

        viewModel?.errorResponse?.onChanged(this) {errorResponse ->
            if (errorResponse != null && errorResponse.code == 100) {
                //show App update dialog
                showAppUpdateDialog(getString(R.string.app_update_message))
            } else {
                showErrorAndTransitToLogin(errorResponse?.message ?: "エラー")
            }
        }
    }

    private fun getUserCredentialFromLocalDB() {
        val query = realm.where<UserModel>()
        val user = query.findFirst()
        if (user != null && user.password.isNotEmpty()) {
            password = getDecryptedPassword(user.password)
            AppSetting(this).fcmToken?.let { token ->
                getApiToken(user.email, password, token)
            } ?: kotlin.run {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    getApiToken(user.email, password, token)
                }
            }
        } else {
            Handler().postDelayed({
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("isFromInitial", true)
                navigateAsNewTask(intent)
            }, 1500)
        }
    }

    private fun deleteExistingGrowthInfo() {
        realm.executeTransactionAsync { bgRealm ->
            bgRealm.delete(GrowthHistories::class.java)
        }
    }

    private fun getDecryptedPassword(dataToDecrypt: String): String {
        val encryptPass = getString(R.string.encryption_pass)
        val hashMap = HashMap<String, ByteArray>()
        val sharedPreferences = AppManager.sharedPreference
        val iv = sharedPreferences.getString("p_iv", "")
        val salt = sharedPreferences.getString("p_salt", "")

        if (iv.isNullOrEmpty() || salt.isNullOrEmpty()) {
            return dataToDecrypt
        }

        hashMap["iv"] = Base64.decode(iv, Base64.NO_WRAP)
        hashMap["salt"] = Base64.decode(salt, Base64.NO_WRAP)
        hashMap["encrypted"] = Base64.decode(dataToDecrypt, Base64.NO_WRAP)

        return Encrypt().decrypt(hashMap, encryptPass) ?: dataToDecrypt
    }

    private fun getApiToken(email: String, password: String, fcmToken: String) {
        viewModel?.fetchApiToken(email, password, fcmToken) { response, error ->
            if (response != null && response.data?.api_token != null) {
                AppManager.apiToken = response.data.api_token
                AppManager.isLoggedIn = true
                getReadNewsIds(response.data.api_token)
                getNews(response.data.api_token)
                getGrowthHistory(response.data.api_token)
                getUserInfo(response.data.api_token)
            } else {
                showErrorAndTransitToLogin(response?.message ?: response?.data?.message ?: error ?: getString(R.string.api_data_error_ea006))
            }
        }
    }

    private fun getUserInfo(apiToken: String) {
        viewModel?.fetchUserInfo(apiToken)
    }

    private fun getGrowthHistory(apiToken: String) {
        viewModel?.fetchGrowthHistory(apiToken)
    }

    private fun getNews(apiToken: String) {
        viewModel?.getNewsList(apiToken)
    }

    private fun getReadNewsIds(apiToken: String) {
        viewModel?.getReadNewsList(apiToken)
    }

    private fun createNewsList(newsList: List<News>) {
        GlobalScope.launch {
            val newsData: MutableList<NewsModel> = mutableListOf()
            val readNewsIdList = viewModel?.readNewsLiveData?.value?.data?.read_news
            for (item  in newsList) {
                val newsModel = NewsModel(item.newsId, item.title, item.releaseStartAt, item.releaseEndAt, item.isRelease, item.listImage, 0)
                newsData.add(newsModel)
            }
            if (!readNewsIdList.isNullOrEmpty()) {
                saveNewsDataToLocalDB(newsData, readNewsIdList)
            } else {
                saveNewsDataToLocalDB(newsData)
            }
        }
    }

    private fun saveNewsDataToLocalDB(newsList: List<NewsModel>, readNewsIdList: List<Int> = listOf()) {
        if (readNewsIdList.isNotEmpty()) {
            for (newsId in readNewsIdList) {
                for (news in newsList) {
                    if (news.newsId == newsId) {
                        news.isRead = 1
                        break
                    }
                }
            }
        }
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        realm.insertOrUpdate(newsList)
        realm.commitTransaction()
        realm.close()
    }

    private fun saveUserInfoIntoLocalDb(user: UserModel, children: List<ChildData>) {
        val encryptedPass = getEncryptedPassword(user.password)
        user.password = encryptedPass
        realm.beginTransaction()

        realm.insertOrUpdate(user)

        val childList = mutableListOf<ChildrenModel>()
        for (childUser in children) {
            val id = childUser.childId
            val firstName = childUser.first_name
            val lastName = childUser.last_name
            val firstNameKana = childUser.first_name_kana
            val lastNameKana = childUser.last_name_kana
            val image = childUser.image
            val gender = childUser.gender
            val birthday = childUser.birth_day
            val birthOrder = childUser.birth_order
            val siblingOrder = childUser.sibling_order

            val child = ChildrenModel(id, lastName, firstName, lastNameKana, firstNameKana, image, gender, birthday, birthOrder, siblingOrder)
            childList.add(child)
        }

        if (childList.size > 0) {
            realm.insertOrUpdate(childList)
        }

        realm.commitTransaction()
    }

    private fun getEncryptedPassword(dataToEncrypt: String): String {
        val encryptPass = getString(R.string.encryption_pass)

        val encryptedMap = Encrypt().encrypt(dataToEncrypt, encryptPass) ?: return dataToEncrypt

        val editor = AppManager.sharedPreference.edit()
        editor.putString("p_iv", Base64.encodeToString(encryptedMap["iv"], Base64.NO_WRAP))
        editor.putString("p_salt", Base64.encodeToString(encryptedMap["salt"], Base64.NO_WRAP))
        editor.apply()

        return Base64.encodeToString(encryptedMap["encrypted"], Base64.NO_WRAP)
    }

    private fun saveGrowthDataToLocal(growthHistories: List<GrowthHistory>) {
        val formatter1 = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        val formatter2 = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
        realm.executeTransactionAsync { backgroundRealm ->
            val growthDataList: MutableList<GrowthHistories> = mutableListOf()
            for (history in growthHistories) {
                val dateFormatter = if (history.measuredAt.contains("/")) {
                    formatter1
                } else {
                    formatter2
                }
                val growth = GrowthHistories("${history.childId}${history.measuredAt}", history.childId, dateFormatter.parse(history.measuredAt) ?: Date(), history.height, history.weight)
                growthDataList.add(growth)
            }
            backgroundRealm.insertOrUpdate(growthDataList)
        }
    }

    private fun showErrorAndTransitToLogin(message: String) {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("isFromInitial", true)
                navigateAsNewTask(intent)
            }
            .show()
    }

    private fun showAppUpdateDialog(message: String) {
        val dialog = AlertDialog.Builder(this)
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_ok_dialog_without_title, null, false)
        dialog.setView(layout)
        dialog.setCancelable(false)

        val alert = dialog.create()
        layout.findViewById<TextView>(R.id.message).text = message
        layout.findViewById<Button>(R.id.ok_button).setOnClickListener {
            alert.dismiss()
            openGooglePlay()
        }

        alert.show()
        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
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

    private fun transitToHomeScreen(delay: Long) {
        Handler().postDelayed({
            navigateAsNewTask(Intent(this, MainActivity::class.java))
        }, delay)
    }

    private fun setAlarmForNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(this, NotificationBootReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT)
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
        }
        // Set the alarm to start at approximately 12:00 p.m.
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 12)
        }

        alarmManager?.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun onDestroy() {
        realm.close()
        super.onDestroy()
    }
}