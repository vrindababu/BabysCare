package net.babys_care.app.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import net.babys_care.app.R

fun RecyclerView.addDividerItem(context: Context, orientation: Int = DividerItemDecoration.VERTICAL) {
    val decorator = DividerItemDecoration(context, orientation)
    ContextCompat.getDrawable(context, R.drawable.divider)?.let { decorator.setDrawable(it) }
    this.addItemDecoration(decorator)
}