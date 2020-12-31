package net.babys_care.app.scene.trouble

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_sleep.*
import net.babys_care.app.R
import net.babys_care.app.models.MealDataModel
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.utils.GridSpaceItemDecoration
import net.babys_care.app.utils.adapters.MealAdapter
import net.babys_care.app.utils.debugLogInfo

class SleepFragment : BaseFragment() {

    override val layout = R.layout.fragment_sleep
    private val itemNum: Int = 5
    private val images: List<Int> = listOf(
        R.drawable.crying_at_night,
        R.drawable.put_to_sleep,
        R.drawable.environment,
        R.drawable.clothes,
        R.drawable.time_of_sleeping
    )
    private val names: List<String> = listOf(
        "夜泣き",
        "寝かしつけ",
        "環境",
        "服装",
        "睡眠時間"
    )
    private val ids: List<Int> = listOf(
        110,
        1054,
        7437,
        7525,
        1022
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mealData = getData()
        sleep_recycler_view.layoutManager = GridLayoutManager(requireContext(), 2)
        sleep_recycler_view.adapter = MealAdapter(mealData).apply {
            onItemClick = {mealDataModel ->
                val articleList = ArticleListFragment().apply {
                    parentId = mealDataModel.wpId
                    onArticleClick = {fragment ->
                        showFragment(fragment, true)
                    }
                }
                showFragment(articleList, true)
            }
        }
        sleep_recycler_view.setHasFixedSize(true)
        sleep_recycler_view.addItemDecoration(GridSpaceItemDecoration(25, 2))
        setupBackPressHandler()
    }

    private fun getData(): List<MealDataModel> {
        val dataList = mutableListOf<MealDataModel>()
        for (i in 0 until itemNum) {
            val meal = MealDataModel(ids[i], names[i], images[i])
            dataList.add(meal)
        }

        return dataList
    }

    private fun showFragment(fragment: Fragment, addToBackStack: Boolean) {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        fragmentTransaction.replace(R.id.child_fragment_container, fragment, fragment::class.simpleName)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(fragment::class.java.simpleName)
        }
        fragmentTransaction.commit()
        if (fragment is SleepFragment) {
            updateTopUI(true)
        } else {
            updateTopUI(false)
        }
    }

    private fun updateTopUI(isTop: Boolean) {
        hideTabLayout(!isTop)
        if (isTop) {
            child_fragment_container.visibility = View.GONE
            sleep_recycler_view.visibility = View.VISIBLE
        } else {
            sleep_recycler_view.visibility = View.GONE
            child_fragment_container.visibility = View.VISIBLE
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) {
                debugLogInfo("Count: ${childFragmentManager.backStackEntryCount}")
                val fragment = childFragmentManager.findFragmentByTag(ArticleDetailFragment::class.java.simpleName)
                if(fragment != null) {
                    childFragmentManager.popBackStackImmediate()
                    val articleFragment = childFragmentManager.findFragmentByTag(ArticleListFragment::class.java.simpleName) ?: ArticleListFragment()
                    showFragment(articleFragment, false)
                    return@addCallback
                }

                val articleFragment = childFragmentManager.findFragmentByTag(ArticleListFragment::class.java.simpleName)
                if (articleFragment != null) {
                    childFragmentManager.popBackStackImmediate()
                    updateTopUI(true)
                    return@addCallback
                }

                if (isEnabled) {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
    }

    fun hideTabLayout(isHidden: Boolean) {
        (parentFragment as? TroubleFragment)?.hideTabLayout(isHidden)
    }
}