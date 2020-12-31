package jp.winas.android.foundation.scene

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import jp.winas.android.foundation.R
import jp.winas.android.foundation.scene.uicomponent.BaseViewPager

open class BaseTabActivity : BaseActivity() {
    final override val layout: Int = -1

    val TAB_ALIGN_BOTTOM = 0
    val TAB_ALIGN_TOP = 1

    open val tabHeight: Int by lazy { resources.getDimensionPixelSize(R.dimen.tabDefaultHeight) }
    open val tabAlign: Int = TAB_ALIGN_BOTTOM
    protected val tabLayout: TabLayout by lazy {
        TabLayout(this).apply { id = R.id.tabLayout }
    }
    protected val viewPager: BaseViewPager by lazy {
        BaseViewPager(this).apply { id = R.id.viewPager }
    }
    protected val tabPagerAdapter: TabPagerAdapter by lazy {
        TabPagerAdapter(supportFragmentManager).apply { notifyDataSetChanged() }
    }
    private val rootView: ConstraintLayout by lazy {
        ConstraintLayout(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    // Do not modify content views on subclass
    override fun addContentView(view: View?, params: ViewGroup.LayoutParams?) {
//        super.addContentView(view, params)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
//        super.setContentView(view, params)
    }

    override fun setContentView(layoutResID: Int) {
//        super.setContentView(layoutResID)
    }

    override fun setContentView(view: View?) {
//        super.setContentView(view)
    }

    private fun initView() {
        rootView.addView(tabLayout)
        rootView.addView(viewPager)
        super.setContentView(rootView)

        ConstraintSet().apply {
            constrainWidth(tabLayout.id, ConstraintSet.MATCH_CONSTRAINT)
            constrainHeight(tabLayout.id, tabHeight)
            constrainWidth(viewPager.id, ConstraintSet.MATCH_CONSTRAINT)
            constrainHeight(viewPager.id, ConstraintSet.MATCH_CONSTRAINT_SPREAD)
            connect(tabLayout.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            connect(tabLayout.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
            connect(viewPager.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            connect(viewPager.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

            if (tabAlign == TAB_ALIGN_BOTTOM) {
                connect(tabLayout.id, ConstraintSet.TOP, viewPager.id, ConstraintSet.BOTTOM)
                connect(tabLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                connect(viewPager.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(viewPager.id, ConstraintSet.BOTTOM, tabLayout.id, ConstraintSet.TOP)
            } else {
                connect(tabLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(tabLayout.id, ConstraintSet.BOTTOM, viewPager.id, ConstraintSet.TOP)
                connect(viewPager.id, ConstraintSet.TOP, tabLayout.id, ConstraintSet.BOTTOM)
                connect(viewPager.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            }
        }.applyTo(rootView)

        initTab()
        initViewPager()
    }

    fun addTab(tabView: TabView, fragment: Fragment) {
        tabLayout.addTab(tabLayout.newTab().setCustomView(tabView).setTag(fragment), false)
        tabPagerAdapter.apply { pages += fragment }.notifyDataSetChanged()
    }

    fun removeTab(index: Int) {
        tabLayout.removeTabAt(index)
        tabPagerAdapter.apply { pages.removeAt(index) }.notifyDataSetChanged()
    }

    fun clearAllTabs() {
        tabLayout.removeAllTabs()
        tabPagerAdapter.apply { pages.clear() }.notifyDataSetChanged()
    }

    private fun initTab() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.run { viewPager.currentItem = position }
            }
        })
    }

    private fun initViewPager() {
        viewPager.adapter = tabPagerAdapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
    }

    inner class TabView(@LayoutRes resId: Int, @DrawableRes val iconId: Int? = null, @StringRes val titleId: Int? = null) : ConstraintLayout(this) {
        var iconView: ImageView? = null
        var titleView: TextView? = null

        init {
            inflate(context, resId, this)
        }

        override fun onViewAdded(view: View?) {
            super.onViewAdded(view)
            view?.let { it as? ViewGroup }?.let { viewGroup ->
                for (i in 0 until viewGroup.childCount) {
                    when (val child = viewGroup.getChildAt(i)) {
                        is ImageView -> iconView = child
                        is TextView -> titleView = child
                    }
                }
            }
            iconId?.let { iconView?.setImageResource(it) }
            titleId?.let { titleView?.setText(it) }
        }

    }
}

open class TabPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    val pages = mutableListOf<Fragment>()

    override fun getItem(position: Int): Fragment = pages[position]

    override fun getCount(): Int = pages.size
}