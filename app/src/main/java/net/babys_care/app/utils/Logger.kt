package net.babys_care.app.utils

import android.util.Log
import jp.winas.android.foundation.BuildConfig

internal object Logger {

    private const val tag = BuildConfig.logTag

    fun log(priority: Int, msg: String?, function: String? = null) {
        Log.println(priority, tag, "[$function] $msg")
    }

}

// Using getStackTrace may cause a little time lag
private inline val caller: String? get() = Throwable().stackTrace.let { if (it.size > 2) it[2].toString() else null }

// Print log message for debug build only
internal fun debugLogInfo(msg: Any?) = if (BuildConfig.DEBUG) logInfo(msg) else Unit
internal fun debugLogError(msg: Any?) = if (BuildConfig.DEBUG) logError(msg) else Unit

// Print log message
internal fun logInfo(msg: Any?) = Logger.log(Log.INFO, if (msg is String) msg else msg.toString(), caller)
internal fun logError(msg: Any?) = Logger.log(Log.ERROR, if (msg is String) msg else msg.toString(), caller)