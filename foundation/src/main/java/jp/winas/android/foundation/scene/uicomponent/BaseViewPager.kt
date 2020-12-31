package jp.winas.android.foundation.scene.uicomponent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class BaseViewPager(context: Context, attrs: AttributeSet? = null): ViewPager(context, attrs) {

//    constructor(context: Context): this(context, null)

    var pagingEnabled: Boolean = false

    override fun onTouchEvent(ev: MotionEvent?): Boolean = when (pagingEnabled) {
        true -> super.onTouchEvent(ev)
        else -> false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = when (pagingEnabled) {
        true -> super.onInterceptTouchEvent(ev)
        else -> false
    }

}