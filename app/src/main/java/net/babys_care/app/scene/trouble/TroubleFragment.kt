package net.babys_care.app.scene.trouble

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_trouble.*
import net.babys_care.app.R
import net.babys_care.app.scene.MainActivity

class TroubleFragment : BaseFragment() {

    override val layout = R.layout.fragment_trouble

    private val pagerAdapter: TroublePagerAdapter by lazy {
        TroublePagerAdapter(childFragmentManager)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager.adapter = pagerAdapter
        viewPager.pagingEnabled = true
        tabLayout.setupWithViewPager(viewPager)
        viewPager.currentItem = 4
        (tabLayout.getChildAt(0) as ViewGroup).getChildAt(0).isEnabled = false
        (tabLayout.getChildAt(0) as ViewGroup).getChildAt(1).isEnabled = false
        (tabLayout.getChildAt(0) as ViewGroup).getChildAt(7).isEnabled = false
        (tabLayout.getChildAt(0) as ViewGroup).getChildAt(8).isEnabled = false

        viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                if (position < 2) {
                    viewPager.currentItem = 2
                } else if (position > 6) {
                    viewPager.currentItem = 6
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}

        })
    }

    fun hideTabLayout(isHidden: Boolean) {
        if (isHidden) {
            (activity as? MainActivity)?.addBackButtonAndActionToMain(true)
            tabLayout?.visibility = View.GONE
            viewPager.pagingEnabled = false
        } else {
            (activity as? MainActivity)?.addBackButtonAndActionToMain(false)
            tabLayout.visibility = View.VISIBLE
            viewPager.pagingEnabled = true
        }
    }
}