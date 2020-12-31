package net.babys_care.app.scene.initial

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import net.babys_care.app.scene.login.LoginActivity
import net.babys_care.app.utils.debugLogInfo

class AppLinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onResume() {
        super.onResume()
        onLaunch()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        onLaunch()
    }

    private fun onLaunch() {
        intent.data?.let { uri ->
            debugLogInfo("Called: $uri")
            when (uri.path) {
                "/login" -> {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("isFromInitial", true)
                    startActivity(intent)
                }
                else -> startActivity(Intent(this, SplashActivity::class.java))
            }
        }
        finish()
    }
}