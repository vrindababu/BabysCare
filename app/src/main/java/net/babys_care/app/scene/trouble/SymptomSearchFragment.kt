package net.babys_care.app.scene.trouble

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_symptom_search.*
import net.babys_care.app.R
import net.babys_care.app.models.MealDataModel
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.utils.GridSpaceItemDecoration
import net.babys_care.app.utils.adapters.MealAdapter
import net.babys_care.app.utils.debugLogInfo

class SymptomSearchFragment : BaseFragment() {

    override val layout = R.layout.fragment_symptom_search
    private val itemNum: Int = 16
    private val images: List<Int> = listOf(
        R.drawable.fever,
        R.drawable.burn,
        R.drawable.vomiting,
        R.drawable.diarrhea,
        R.drawable.accidental_ingestion,
        R.drawable.rash,
        R.drawable.convulsions,
        R.drawable.constipation,
        R.drawable.cough,
        R.drawable.runny_nose_stuffy_nose,
        R.drawable.injury,
        R.drawable.swelling,
        R.drawable.sunburn,
        R.drawable.heat_stroke,
        R.drawable.rough_skin,
        R.drawable.bruise
    )
    private val names: List<String> = listOf(
        "発熱",
        "やけど",
        "嘔吐",
        "下痢",
        "誤飲",
        "発疹",
        "痙攣",
        "便秘",
        "咳",
        "鼻水鼻づまり",
        "ケガ",
        "腫れ",
        "日焼け",
        "熱中症",
        "肌荒れ",
        "打撲"
    )
    private val ids: List<Int> = listOf(
        620,
        6856,
        6852,
        6970,
        1201,
        1171,
        1189,
        1183,
        6867,
        6972,
        6986,
        6913,
        531,
        1186,
        6873,
        6974
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mealData = getData()
        symptom_recycler_view.layoutManager = GridLayoutManager(requireContext(), 2)
        symptom_recycler_view.adapter = MealAdapter(mealData).apply {
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
        symptom_recycler_view.setHasFixedSize(true)
        symptom_recycler_view.addItemDecoration(GridSpaceItemDecoration(25, 2))
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
        if (fragment is SymptomSearchFragment) {
            updateTopUI(true)
        } else {
            updateTopUI(false)
        }
    }

    private fun updateTopUI(isTop: Boolean) {
        hideTabLayout(!isTop)
        if (isTop) {
            child_fragment_container.visibility = View.GONE
            symptom_recycler_view.visibility = View.VISIBLE
        } else {
            symptom_recycler_view.visibility = View.GONE
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
//                    val topFragment = childFragmentManager.findFragmentByTag(SymptomSearchFragment::class.java.simpleName) ?: SymptomSearchFragment()
//                    showFragment(topFragment, false)
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