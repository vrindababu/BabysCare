package net.babys_care.app.utils.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.babys_care.app.R
import net.babys_care.app.api.responses.Tag

class TagAdapter(private val tagList: List<Tag>) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tag_recycler, parent, false))
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tagList[position]
        holder.tagName.text = tag.name
    }

    override fun getItemCount(): Int {
        return tagList.size
    }

    inner class TagViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tagName: TextView =itemView.findViewById(R.id.article_tag)
    }
}