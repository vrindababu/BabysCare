package jp.winas.android.foundation.scene

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

abstract class BaseActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    abstract val layout: Int
    var onBackPressAlternative: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
    }

    inline fun <reified A : Activity> createIntent() = Intent(this, A::class.java)

    fun navigate(intent: Intent, requestCode: Int? = null, anim: Pair<Int, Int>? = null) {
        when (requestCode) {
            null -> startActivity(intent)
            else -> startActivityForResult(intent, requestCode)
        }
        anim?.let { overridePendingTransition(it.first, it.second) }
    }

    /**
     * Common method for navigating to another activity as new task. Clears the activity stack
     * @param intent the destination intent
     * @param anim optional animation for transition
     */
    fun navigateAsNewTask(intent: Intent, anim: Pair<Int, Int>? = null) {
        startActivity(intent)
        finish()
        anim?.let { overridePendingTransition(it.first, it.second) }
    }

    /**
     * A simple function for manually navigate to another fragment
     * @param fragment destination fragment
     * @param resId fragment container layout id
     * @param addToBackStack boolean value to determine if add to backStack or not
     * @param nameForBackStack name for adding to back stack. Default is null
     */
    fun navigate(fragment: Fragment, @IdRes resId: Int, addToBackStack: Boolean? = true, nameForBackStack: String? = null) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        fragmentTransaction.replace(resId, fragment, fragment::class.simpleName)
        if (addToBackStack == true) {
            fragmentTransaction.addToBackStack(nameForBackStack)
        }
        fragmentTransaction.commit()
    }

    override fun onBackPressed() {
        onBackPressAlternative?.let { it() } ?: kotlin.run {
            super.onBackPressed()
        }
    }

}