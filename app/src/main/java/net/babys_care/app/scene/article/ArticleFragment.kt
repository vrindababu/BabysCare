package net.babys_care.app.scene.article

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_article.*
import net.babys_care.app.R
import net.babys_care.app.utils.debugLogInfo

class ArticleFragment : BaseFragment() {

    override val layout= R.layout.fragment_article
    private lateinit var articleFragment: ArticleTopFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        articleFragment = ArticleTopFragment()
        addFragment(articleFragment)
        setupBackPressHandler()
    }

    /**
     * Adds fragment dynamically to top view
     * @param fragment that will be added
     * @param addToBackStack flag to determine whether to add into backStack or not
     */
    fun addFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        fragmentTransaction.replace(R.id.fragment_container, fragment, fragment::class.simpleName)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(fragment::class.java.simpleName)
        }
        fragmentTransaction.commit()
    }

    private fun backFromDetails() {
        fragment_container.removeAllViews()
        val manager = childFragmentManager
        val fragments = manager.fragments
        for (fragment in fragments) {
            manager.popBackStackImmediate()
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) {
                val fragment = childFragmentManager.findFragmentByTag(ArticleDetailFragment::class.java.simpleName)
                if(fragment != null) {
                    debugLogInfo("Called")
                    backFromDetails()
                } else {
                    debugLogInfo("Called")
                    if (isEnabled) {
                        isEnabled = false
                        activity?.onBackPressed()
                    }
                }
//                if (web_view.canGoBack()) {
//                    debugLogInfo("Called")
//                    web_view.goBack()
//                } else {
//                    debugLogInfo("Called")
//                    if (isEnabled) {
//                        isEnabled = false
//                        requireActivity()?.onBackPressed()
//                    }
//                }
            }
    }
}