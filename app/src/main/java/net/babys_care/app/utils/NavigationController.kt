package net.babys_care.app.utils

import android.app.Activity
import android.content.Intent
import net.babys_care.app.scene.login.LoginActivity

class NavigationController(val activity: Activity) {

    fun navigateToLogin() {
        val intent = Intent(activity, LoginActivity::class.java)
        intent.putExtra("isFromInitial", true)
        activity.startActivity(intent)
        activity.finishAffinity()
    }
}