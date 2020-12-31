package net.babys_care.app.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpaceItemDecoration(private val space: Int, private val spanCount: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position % 2 == 0) {
            outRect.left  = space
            outRect.right = space / 2
        } else {
            outRect.right = space
            outRect.left = space / 2
        }
        outRect.bottom = space

        // Add top margin only for the first item to avoid double space between items
        if (position < spanCount) {
            outRect.top = space
        } else {
            outRect.top = 0
        }
    }
}