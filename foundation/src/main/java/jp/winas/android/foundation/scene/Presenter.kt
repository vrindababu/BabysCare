package jp.winas.android.foundation.scene

import android.content.Intent
import android.os.Bundle

interface Presenter {
    val activity: BaseActivity

    fun present(targetActivity: BaseActivity, data: Bundle? = null, requestCode: Int? = null) {
        Intent(activity, targetActivity::class.java).let { intent ->
            data?.let { intent.putExtras(it) }
            when (requestCode) {
                null -> activity.startActivity(intent)
                else -> activity.startActivityForResult(intent, requestCode)
            }
        }
    }

}