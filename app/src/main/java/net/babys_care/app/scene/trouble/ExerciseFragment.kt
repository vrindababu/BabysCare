package net.babys_care.app.scene.trouble

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_exercise.*
import net.babys_care.app.R
import net.babys_care.app.models.ExerciseData
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.utils.GridSpaceItemDecoration
import net.babys_care.app.utils.debugLogInfo

class ExerciseFragment : BaseFragment() {

    override val layout = R.layout.fragment_exercise
    private val itemNum: Int = 6
    private val images: List<Int> = listOf(
        R.drawable.turn_over,
        R.drawable.hai_hai,
        R.drawable.grabbing,
        R.drawable.baby_massage,
        R.drawable.rhythmic,
        R.drawable.walk_play_in_park
    )
    private val names: List<String> = listOf(
        "寝返り",
        "はいはい",
        "つかまり立ち",
        "ベビーマッサージ",
        "リトミック",
        "お散歩/公園遊び"
    )
    private val ids: List<Int> = listOf(
        371,
        525,
        1221,
        115,
        119,
        453
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mealData = getData()
        exercise_recycler_view.layoutManager = GridLayoutManager(requireContext(), 2)
        exercise_recycler_view.adapter = ExerciseAdapter(mealData).apply {
            onItemClick = {exerciseData ->
                val articleList = ArticleListFragment().apply {
                    parentId = exerciseData.wpId
                    onArticleClick = {fragment ->
                        showFragment(fragment, true)
                    }
                }
                showFragment(articleList, true)
            }
        }
        exercise_recycler_view.setHasFixedSize(true)
        exercise_recycler_view.addItemDecoration(GridSpaceItemDecoration(25, 2))
        setupBackPressHandler()
    }

    private fun getData(): List<ExerciseData> {
        val dataList = mutableListOf<ExerciseData>()
        for (i in 0 until itemNum) {
            val meal = ExerciseData(ids[i], names[i], images[i])
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
        if (fragment is ExerciseFragment) {
            updateTopUI(true)
        } else {
            updateTopUI(false)
        }
    }

    private fun updateTopUI(isTop: Boolean) {
        hideTabLayout(!isTop)
        if (isTop) {
            child_fragment_container.visibility = View.GONE
            exercise_recycler_view.visibility = View.VISIBLE
        } else {
            exercise_recycler_view.visibility = View.GONE
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
//                    val topFragment = childFragmentManager.findFragmentByTag(ExerciseFragment::class.java.simpleName) ?: ExerciseFragment()
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

    inner class ExerciseAdapter(private val dataList: List<ExerciseData>) : RecyclerView.Adapter<ExerciseViewHolder>() {

        var onItemClick: ((ExerciseData) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            return ExerciseViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_meal_recycler_view, parent, false))
        }

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            val data = dataList[position]
            holder.mealName.text = data.name
            Glide.with(holder.mealImage).load(data.image).apply(RequestOptions().skipMemoryCache(true)).into(holder.mealImage)
            holder.itemView.setOnClickListener {
                onItemClick?.invoke(data)
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

    }

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealImage: ImageView = itemView.findViewById(R.id.meal_image)
        val mealName: TextView = itemView.findViewById(R.id.meal_name)
    }
}