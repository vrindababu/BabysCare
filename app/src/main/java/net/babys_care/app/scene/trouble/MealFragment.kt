package net.babys_care.app.scene.trouble

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_meal.*
import net.babys_care.app.R
import net.babys_care.app.models.MealDataModel
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.utils.GridSpaceItemDecoration
import net.babys_care.app.utils.adapters.MealAdapter
import net.babys_care.app.utils.debugLogInfo

class MealFragment : BaseFragment() {

    override val layout = R.layout.fragment_meal
    private val itemNum: Int = 8
    private val images: List<Int> = listOf(
        R.drawable.breast_feeding,
        R.drawable.milk,
        R.drawable.graduation,
        R.drawable.weaning,
        R.drawable.early_baby_food,
        R.drawable.mid_term_baby_food,
        R.drawable.late_baby_food,
        R.drawable.baby_food_completion_period
    )
    private val names: List<String> = listOf(
        "授乳",
        "ミルク",
        "卒乳",
        "断乳",
        "離乳食初期",
        "離乳食中期",
        "離乳食後期",
        "離乳食完了期"
    )
    private val ids: List<Int> = listOf(
        7017,
        7002,
        6875,
        7065,
        1015,
        1023,
        1033,
        1045
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mealData = getData()
        meal_recycler_view.layoutManager = GridLayoutManager(requireContext(), 2)
        meal_recycler_view.adapter = MealAdapter(mealData).apply {
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
        meal_recycler_view.setHasFixedSize(true)
        meal_recycler_view.addItemDecoration(GridSpaceItemDecoration(25, 2))
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
        if (fragment is MealFragment) {
            updateTopUI(true)
        } else {
            updateTopUI(false)
        }
    }

    private fun updateTopUI(isTop: Boolean) {
        hideTabLayout(!isTop)
        if (isTop) {
            child_fragment_container.visibility = View.GONE
            meal_recycler_view.visibility = View.VISIBLE
        } else {
            meal_recycler_view.visibility = View.GONE
            child_fragment_container.visibility = View.VISIBLE
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) {
                debugLogInfo("Count: ${childFragmentManager.backStackEntryCount}")
                val fragment = childFragmentManager.findFragmentByTag(ArticleDetailFragment::class.java.simpleName)
                if(fragment != null) {
                    childFragmentManager.popBackStackImmediate(fragment::class.java.simpleName, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    val articleFragment = childFragmentManager.findFragmentByTag(ArticleListFragment::class.java.simpleName) ?: ArticleListFragment()
                    showFragment(articleFragment, false)
                    return@addCallback
                }

                val articleFragment = childFragmentManager.findFragmentByTag(ArticleListFragment::class.java.simpleName)
                if (articleFragment != null) {
                    childFragmentManager.popBackStackImmediate(articleFragment::class.java.simpleName, FragmentManager.POP_BACK_STACK_INCLUSIVE)
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