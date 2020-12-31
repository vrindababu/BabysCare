package jp.winas.android.foundation

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.*

open class BaseApplication : Application() {
    protected var appStatus: AppStatus = AppStatus.FOREGROUND
    protected val appStatusCallbackList = ArrayList<AppStatusCallback>()

    inner class BaseActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        // running activity count
        private var running = 0

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            this@BaseApplication.onActivityCreated(activity, savedInstanceState)
        }

        override fun onActivityStarted(activity: Activity) {
            ++running
            if (running == 1) {
                if (appStatus == AppStatus.BACKGROUND) {
                    // running activity is 1,
                    // app must be returned from background just now (or first launch)
                    appStatus = AppStatus.RETURNED_TO_FOREGROUND
                    appStatusCallbackList.onEach { it.onAppStatusChanged(appStatus, activity) }
                    onReturnForeground(activity)
                }
            } else if (running > 1) {
                // 2 or more running activities,
                // should be foreground already.
                appStatus = AppStatus.FOREGROUND
                appStatusCallbackList.onEach { it.onAppStatusChanged(appStatus, activity) }
            }
            this@BaseApplication.onActivityStarted(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            this@BaseApplication.onActivityResumed(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            this@BaseApplication.onActivityPaused(activity)
        }

        override fun onActivityStopped(activity: Activity) {
            --running
            if (running == 0) {
                // no active activity
                // app goes to background
                appStatus = AppStatus.BACKGROUND
                appStatusCallbackList.onEach { it.onAppStatusChanged(appStatus, activity) }
                onEnterBackground(activity)
            }
            this@BaseApplication.onActivityStopped(activity)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
            this@BaseApplication.onActivitySaveInstanceState(activity, outState)
        }

        override fun onActivityDestroyed(activity: Activity) {
            this@BaseApplication.onActivityDestroyed(activity)
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(BaseActivityLifecycleCallbacks())
    }

    fun addAppStatusCallback(appStatusCallback: AppStatusCallback) {
        appStatusCallbackList.add(appStatusCallback)
    }

    fun removeAppStatusCallback(appStatusCallback: AppStatusCallback): Boolean {
        return appStatusCallbackList.remove(appStatusCallback)
    }

    open fun onEnterBackground(activity: Activity) = Unit
    open fun onReturnForeground(activity: Activity) = Unit

    open fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    open fun onActivityStarted(activity: Activity) = Unit
    open fun onActivityResumed(activity: Activity) = Unit
    open fun onActivityPaused(activity: Activity) = Unit
    open fun onActivityStopped(activity: Activity) = Unit
    open fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) = Unit
    open fun onActivityDestroyed(activity: Activity) = Unit
}

enum class AppStatus {
    BACKGROUND, // app is background
    RETURNED_TO_FOREGROUND, // app returned to foreground(or first launch)
    FOREGROUND // app is foreground
}

interface AppStatusCallback {
    fun onAppStatusChanged(appStatus: AppStatus, activity: Activity)
}
