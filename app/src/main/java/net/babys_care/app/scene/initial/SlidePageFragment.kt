package net.babys_care.app.scene.initial

import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_slide_page.*
import net.babys_care.app.R

class SlidePageFragment : BaseFragment() {

    override val layout = R.layout.fragment_slide_page

    var title: String = ""
    var imageResource: Int = 0
    var position: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tutorial_title.text = title
        tutorial_image.visibility =  View.VISIBLE
        Glide.with(tutorial_image).load(imageResource).into(tutorial_image)
    }
}