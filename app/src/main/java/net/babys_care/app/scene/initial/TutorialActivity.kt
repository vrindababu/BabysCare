package net.babys_care.app.scene.initial

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import jp.winas.android.foundation.scene.BaseActivity
import kotlinx.android.synthetic.main.activity_tutorial.*
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.scene.login.LoginActivity
import net.babys_care.app.scene.registration.UserRegistrationActivity
import net.babys_care.app.utils.ZoomOutPageTransformer

private const val NUM_PAGES = 4

class TutorialActivity : BaseActivity() {

    override val layout = R.layout.activity_tutorial
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageTitles: List<String> = listOf(
            getString(R.string.tutorial_page_title1),
            getString(R.string.tutorial_page_title2),
            getString(R.string.tutorial_page_title3),
            getString(R.string.tutorial_page_title4)
        )

        val pageImages = listOf(
            R.drawable.tutorial_01,
            R.drawable.tutorial_02,
            R.drawable.tutorial_03,
            R.drawable.tutorial_04
        )

        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = ScreenSlidePagerAdapter(pageTitles, pageImages, this)
        viewPager.setPageTransformer(ZoomOutPageTransformer())

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == NUM_PAGES - 1) {
                    button_start_app.text = getString(R.string.start_app)
                } else {
                    button_start_app.text = getString(R.string.skip)
                }
            }
        })

        TabLayoutMediator(tab_layout, viewPager) { _, _ ->
        }.attach()

        button_start_app.setOnClickListener {
            if (button_start_app.text.toString() == getString(R.string.skip)) {
                viewPager.currentItem = NUM_PAGES - 1
            } else {
                AppManager.sharedPreference.edit().putBoolean("is_first_run", false).apply()
                navigateAsNewTask(Intent(this, UserRegistrationActivity::class.java))
            }
        }

        go_to_login_text_view.setOnClickListener {
            AppManager.sharedPreference.edit().putBoolean("is_first_run", false).apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("isFromInitial", true)
            navigateAsNewTask(intent)
        }
    }

    private class ScreenSlidePagerAdapter(
        val titles: List<String>,
        val images: List<Int>,
        fm: FragmentActivity
    ) : FragmentStateAdapter(fm) {

        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment {
            val fragment = SlidePageFragment()
            fragment.imageResource = images[position]
            fragment.title = titles[position]
            fragment.position = position
            return fragment
        }
    }
}