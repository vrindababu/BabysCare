package net.babys_care.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import io.realm.Realm
import jp.winas.android.foundation.BaseApplication
import net.babys_care.app.utils.debugLogInfo

class AppManager: BaseApplication() {

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreference = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)
        Realm.init(applicationContext)
    }

    override fun onEnterBackground(activity: Activity) {
        debugLogInfo("onEnterBackground")
    }

    override fun onReturnForeground(activity: Activity) {
        debugLogInfo("onReturnForeground")
    }

    companion object {
        private var instance: AppManager? = null
        lateinit var sharedPreference: SharedPreferences

        val context: Context get() = instance!!.applicationContext

        val appId: String? get() = context.packageName
        val appName: String? get() = context.getString(R.string.app_name)

        var apiToken: String = ""
        var isLoggedIn: Boolean = false
    }
}