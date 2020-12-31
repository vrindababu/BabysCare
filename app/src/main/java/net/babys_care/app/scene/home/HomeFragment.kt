package net.babys_care.app.scene.home

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseFragment
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.R
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.scene.settings.BabyInfoCheckFragment
import net.babys_care.app.scene.settings.BabyInfoEditFragment
import net.babys_care.app.scene.settings.UserSettingsFragment

class HomeFragment : BaseFragment() {

    override val layout = R.layout.fragment_home
    var count: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupBackPressHandler()
        updateUI()
    }

    fun showFragment(fragment: Fragment) {
        childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.home_fragment_container, fragment, fragment::class.java.simpleName)
            .addToBackStack(fragment::class.java.simpleName)
            .commit()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) {
                val fragment = childFragmentManager.findFragmentByTag(ArticleDetailFragment::class.java.simpleName)
                if(fragment != null) {
                    childFragmentManager.popBackStack()
                    return@addCallback
                }

                val growthInputFragment = childFragmentManager.findFragmentByTag(GrowthDataInputFragment::class.java.simpleName)
                if (growthInputFragment != null) {
                    childFragmentManager.popBackStack()
                    return@addCallback
                }

                val babyInfoEditFragment = childFragmentManager.findFragmentByTag(BabyInfoEditFragment::class.java.simpleName)
                if (babyInfoEditFragment != null) {
                    childFragmentManager.popBackStack()
                    return@addCallback
                }

                val babyInfoCheckFragment = childFragmentManager.findFragmentByTag(BabyInfoCheckFragment::class.java.simpleName)
                if (babyInfoCheckFragment != null) {
                    childFragmentManager.popBackStack()
                    checkAndUpdateHomeTop()
                    return@addCallback
                }

                val userSettingsFragment = childFragmentManager.findFragmentByTag(UserSettingsFragment::class.java.simpleName)
                if (userSettingsFragment != null) {
                    childFragmentManager.popBackStack()
                    return@addCallback
                }

                if (isEnabled) {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
    }

    fun refreshBabyInfo() {
        val fragment = childFragmentManager.findFragmentByTag(HomeTopFragment::class.java.simpleName)
        if (fragment != null && fragment is HomeTopFragment) {
            fragment.setBabyInfoData()
        } else {
            updateUI()
        }
    }

    private fun checkAndUpdateHomeTop() {
        if (AppSetting(requireContext()).babyInfoChanged == true) {
            AppSetting(AppManager.context).babyInfoChanged = null
            val homeTop = childFragmentManager.findFragmentByTag(HomeTopFragment::class.java.simpleName)
            if (homeTop != null) {
                childFragmentManager.beginTransaction().remove(homeTop).commitNow()
            }

            val homePreMama = childFragmentManager.findFragmentByTag(HomePreMamaFragment::class.java.simpleName)
            if (homePreMama != null) {
                childFragmentManager.beginTransaction().remove(homePreMama).commitNow()
            }

            updateUI()
        }
    }

    private fun updateUI() {
        val realm = Realm.getDefaultInstance()
        val user = realm.where<UserModel>().findFirst()
        val topFragment = if (user?.isPremama == 1) {
            HomePreMamaFragment().apply {
                onFragmentAdd = {fragment -> showFragment(fragment) }
            }
        } else {
            HomeTopFragment().apply {
                onFragmentAdd = {fragment -> showFragment(fragment) }
            }
        }
        showFragment(topFragment)
        realm.close()
    }
}