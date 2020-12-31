package jp.winas.android.foundation.scene

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

abstract class BaseFragment : Fragment(), CoroutineScope by MainScope() {

    abstract val layout: Int

    val navController: NavController? get() = try { findNavController() } catch (e: Exception) { null }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout, null)
    }

    inline fun <reified A : Activity> createIntent() = Intent(activity, A::class.java)

    fun navigate(intent: Intent, requestCode: Int? = null, anim: Pair<Int, Int>? = null) {
        when (requestCode) {
            null -> startActivity(intent)
            else -> startActivityForResult(intent, requestCode)
        }
        anim?.let { activity?.overridePendingTransition(it.first, it.second) }
    }

    // Fragmentの画面遷移はこれを利用
    // resId is action or destination in Navigation Graph
    fun navigate(resId: Int, args: Bundle? = null, navOptions: NavOptions? = null) = navController?.navigate(resId, args, navOptions)

    /**
     * A simple function for manually navigate to another fragment
     * @param fragment destination fragment
     * @param resId fragment container layout id
     * @param addToBackStack boolean value to determine if add to backStack or not
     * @param nameForBackStack name for adding to back stack. Default is null
     */
    fun navigate(fragment: Fragment, @IdRes resId: Int, addToBackStack: Boolean? = true, nameForBackStack: String? = null) {
        val fragmentManager = requireActivity().supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        fragmentTransaction.replace(resId, fragment, fragment::class.simpleName)
        if (addToBackStack == true) {
            fragmentTransaction.addToBackStack(nameForBackStack)
        }
        fragmentTransaction.commit()
    }

    override fun onStart() {
        super.onStart()

        setOnBackPressed(null)
    }

    open fun setOnBackPressed(onBackAlternative: (() -> Unit)?) {
        (activity as BaseActivity).onBackPressAlternative = onBackAlternative
    }
}