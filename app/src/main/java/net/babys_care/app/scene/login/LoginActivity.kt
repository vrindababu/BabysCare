package net.babys_care.app.scene.login

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.firebase.messaging.FirebaseMessaging
import io.realm.Realm
import jp.winas.android.foundation.extension.onChanged
import jp.winas.android.foundation.scene.BaseActivity
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.R
import net.babys_care.app.api.responses.ChildData
import net.babys_care.app.api.responses.GrowthHistory
import net.babys_care.app.api.responses.News
import net.babys_care.app.extensions.enableErrorColor
import net.babys_care.app.extensions.hideKeyBoard
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.models.realmmodels.GrowthHistories
import net.babys_care.app.models.realmmodels.NewsModel
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.scene.initial.TutorialActivity
import net.babys_care.app.utils.Encrypt
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : BaseActivity(), ViewModelable<LoginViewModel> {

    override val layout = R.layout.activity_login
    override val viewModelClass = LoginViewModel::class

    private var loadingDialog: AlertDialog? = null
    private var authenticatedDialog: AlertDialog? = null
    private lateinit var realm: Realm
    var password: String = ""
    private var isPasswordVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()
        realm.executeTransaction {realmBg ->
            realmBg.deleteAll()
        }

        setupObservers()
        addTextWatcher()
        login_button.setOnClickListener {
            password_input_field.hideKeyBoard()
            validateInputAndProceed()
        }

        forgot_password_link.setOnClickListener {
            navigate(Intent(this, ForgotPasswordActivity::class.java))
        }

        toolbar_back_button.setOnClickListener {
            if (intent.getBooleanExtra("isFromInitial", false)) {
                val intent = createIntent<TutorialActivity>()
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            } else {
                onBackPressed()
            }
        }
        password_toggle.setOnClickListener {
            togglePasswordVisibility()
        }

        email_input_field.setOnFocusChangeListener { _, hasFocus ->
            val email = email_input_field.text.toString().trim()
            if (!hasFocus && !TextUtils.isEmpty(email)) {
                email_input_field.visibility = View.GONE
                email_value_text_view.text = email
                email_value_text_view.visibility = View.VISIBLE
                val params = password_input_field.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(params.leftMargin, 22, params.rightMargin, params.bottomMargin)
                password_input_field.layoutParams = params
            } else {
                email_value_text_view.visibility = View.GONE
                email_input_field.visibility = View.VISIBLE
                val params = password_input_field.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(params.leftMargin, 5, params.rightMargin, params.bottomMargin)
                password_input_field.layoutParams = params
            }
        }

        email_value_text_view.setOnClickListener {
            email_value_text_view.visibility = View.GONE
            email_input_field.visibility = View.VISIBLE
            email_input_field.requestFocus()
        }
    }

    private fun addTextWatcher() {
        email_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enableNextButton()
            }
        })

        password_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enableNextButton()
            }
        })
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
                    transitToHomeScreen(500)
                } else {
                    transitToHomeScreen(100)
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

        viewModel?.errorResponse?.onChanged(this) {
            if (it != null && it.code == 100) {
                //show App update dialog
                showAppUpdateDialog(getString(R.string.app_update_message))
            } else {
                transitToHomeScreen(0)
            }
        }
    }

    private fun enableNextButton() {
        val email = email_input_field.text.toString().trim()
        val password = password_input_field.text.toString().trim()
        login_button.isEnabled = !(TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password))
    }

    private fun validateInputAndProceed() {
        val email = email_input_field.text.toString().trim()
        if (!isValidEmail(email)) {
            return
        }

        val password = password_input_field.text.toString().trim()
        if (!isValidPassword(password)) {
            return
        }

        showLoadingDialog(true)
        this.password = password
        AppSetting(this).fcmToken?.let { token ->
            loginUser(email, password, token)
        } ?: kotlin.run {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                loginUser(email, password, token)
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        if (TextUtils.isEmpty(email)) {
            email_error.visibility = View.VISIBLE
            email_error.text = String.format(getString(R.string.enter_at_least_n_character), 6)
            email_input_field.enableErrorColor(true)
            return false
        } else if (email.length > 255) {
            email_error.visibility = View.VISIBLE
            email_error.text = String.format(getString(R.string.enter_at_most_n_character), 255)
            email_input_field.enableErrorColor(true)
            return false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_error.visibility = View.VISIBLE
            email_error.text = getString(R.string.enter_in_email_address_format)
            email_input_field.enableErrorColor(true)
            return false
        }

        email_input_field.enableErrorColor(false)
        email_error.visibility = View.GONE
        return true
    }

    private fun isValidPassword(password: String): Boolean {
        if (TextUtils.isEmpty(password) || password.length < 6) {
            password_error.visibility = View.VISIBLE
            password_error.text = String.format(getString(R.string.enter_at_least_n_character), 6)
            return false
        } else if (password.length > 255) {
            password_error.visibility = View.VISIBLE
            password_error.text = String.format(getString(R.string.enter_at_most_n_character), 255)
            return false
        }

        password_error.visibility = View.GONE
        return true
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            ImageViewCompat.setImageTintList(password_toggle, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.azure)))
            password_input_field.transformationMethod = null
            password_input_field.setSelection(password_input_field.text.length)
        } else {
            ImageViewCompat.setImageTintList(password_toggle, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.whiteFour)))
            password_input_field.transformationMethod = PasswordTransformationMethod()
            password_input_field.setSelection(password_input_field.text.length)
        }
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

    private fun showErrorDialog(message: String) {
        val dialog = AlertDialog.Builder(this)
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_ok_dialog_with_title, null, false)
        dialog.setView(layout)

        val alert = dialog.create()
        layout.findViewById<TextView>(R.id.message).text = message
        layout.findViewById<Button>(R.id.ok_button).setOnClickListener {
            alert.dismiss()
        }

        alert.show()
        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
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

    private fun showAuthenticatedDialog() {
        val dialog = AlertDialog.Builder(this)
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_authenticated_dialog, null, false)
        dialog.setView(layout)

        authenticatedDialog = dialog.create()

        authenticatedDialog?.show()
        authenticatedDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        authenticatedDialog?.window?.setLayout(
            resources.getDimension(R.dimen.authenticated_dialog_width).toInt(),
            resources.getDimension(R.dimen.authenticated_dialog_width).toInt()
        )
    }

    private fun loginUser(email: String, password: String, fcmToken: String) {
        viewModel?.loginUser(email, password, fcmToken) { response, error ->
            showLoadingDialog(false)
            debugLogInfo("Response: $response, $error")
            if (response != null && response.data?.api_token != null) {
                AppManager.apiToken = response.data.api_token
                AppManager.isLoggedIn = true
                getNews(response.data.api_token)
                getReadNewsIds(response.data.api_token)
                showAuthenticatedDialog()
                getGrowthHistory(response.data.api_token)
                getUserInfo(response.data.api_token)
            } else {
                showErrorDialog(response?.message ?: response?.data?.message ?: error ?: getString(R.string.email_or_password_error_message))
            }
        }
    }

    private fun getUserInfo(apiToken: String) {
        viewModel?.fetchUserInfo(apiToken)
    }

    private fun getGrowthHistory(apiToken: String) {
        viewModel?.fetchGrowthHistory(apiToken)
    }

    private fun saveUserInfoIntoLocalDb(user: UserModel, children: List<ChildData>) {
        val encryptedPass = getEncryptedPassword(user.password)
        user.password = encryptedPass

        realm.executeTransactionAsync {bgRealm ->
            bgRealm.insertOrUpdate(user)

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
                bgRealm.insertOrUpdate(childList)
            }
        }
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

    private fun transitToHomeScreen(delay: Long) {
        if (intent.getBooleanExtra("isFromInitial", false)) {
            Handler().postDelayed({
                navigateAsNewTask(Intent(this, MainActivity::class.java))
            }, delay)
        } else {
            Handler().postDelayed({
                finish()
            }, delay)
        }
    }

    override fun onStop() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        if (authenticatedDialog != null) {
            authenticatedDialog?.dismiss()
            authenticatedDialog = null
        }
        realm.close()
        super.onStop()
    }
}