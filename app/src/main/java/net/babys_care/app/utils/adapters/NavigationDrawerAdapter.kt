package net.babys_care.app.utils.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.babys_care.app.R
import net.babys_care.app.models.NavigationDrawerModel

class NavigationDrawerAdapter(private val drawerItemList: List<NavigationDrawerModel>): RecyclerView.Adapter<NavigationDrawerAdapter.NavigationViewHolder>() {

    var onItemClick: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NavigationViewHolder {
        return if (viewType == VIEW_TYPE_FOOTER) {
            NavigationFooterViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.nav_footer_layout, parent, false)
            )
        } else {
            NavigationItemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_nav_drawer, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: NavigationViewHolder, position: Int) {
        if (position < drawerItemList.size) {
            val itemHolder = holder as NavigationItemViewHolder
            val item = drawerItemList[position]
            itemHolder.icon.setImageResource(item.icon)
            itemHolder.title.text = item.title

            itemHolder.itemView.setOnClickListener {
                onItemClick?.invoke(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return drawerItemList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == drawerItemList.size) {
            VIEW_TYPE_FOOTER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    open inner class NavigationViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    inner class NavigationItemViewHolder(itemView: View): NavigationViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.drawer_item_icon)
        val title: TextView = itemView.findViewById(R.id.drawer_item_title)
    }

    inner class NavigationFooterViewHolder(itemView: View): NavigationViewHolder(itemView)

    companion object {
        const val VIEW_TYPE_ITEM: Int = 0
        const val VIEW_TYPE_FOOTER: Int = 1
    }
}