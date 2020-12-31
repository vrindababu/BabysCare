package net.babys_care.app.utils.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.babys_care.app.R
import net.babys_care.app.api.responses.Article
import net.babys_care.app.api.responses.Tag
import net.babys_care.app.extensions.toDotDateFormat

class ArticleAdapter(val context: Context): RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    private val viewPool = RecyclerView.RecycledViewPool()
    var onItemClickListener: ((Article) -> Unit)? = null
    var articleList: List<Article> = listOf()
    var tagList: List<Tag> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        return ArticleViewHolder(LayoutInflater.from(context).inflate(R.layout.item_article_recycler_view, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articleList[position]
        holder.title.text = article.title
        holder.viewCount.text = "${article.view}view"
        holder.date.text = article.date.toDotDateFormat()
        if (article.isFavourite) {
            holder.favouriteIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_filled_24))
        } else {
            holder.favouriteIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_24))
        }
        Glide.with(context).load(article.sourceUrl).into(holder.image)
        if (article.tagIds.isNotEmpty()) {
            setTag(holder, article, viewPool)
        } else {
            holder.tagRecyclerView.adapter = null
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(article)
        }
    }

    private fun setTag(
        holder: ArticleViewHolder,
        article: Article,
        viewPool: RecyclerView.RecycledViewPool
    ) {
        var threshHold = 15
        if (tagList.isNotEmpty()) {
            val tags = mutableListOf<Tag>()
            for (tagId in article.tagIds) {
                for (tagItem in tagList) {
                    if (tagItem.tagId == tagId && tagItem.name.length < threshHold) {
                        tags.add(tagItem)
                        threshHold -= (tagItem.name.length + 2)
                        break
                    }
                }
            }

            holder.tagRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            holder.tagRecyclerView.adapter = TagAdapter((tags))
            holder.tagRecyclerView.setRecycledViewPool(viewPool)
        }
    }

    override fun getItemCount(): Int {
        return articleList.size
    }

    fun setArticleData(list: List<Article>) {
        articleList = list
    }

    fun setTagData(list: List<Tag>) {
        tagList = list
    }

    inner class ArticleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.article_image)
        val title: TextView = itemView.findViewById(R.id.article_title)
        val tagRecyclerView: RecyclerView = itemView.findViewById(R.id.tag_recycler_view)
        val viewCount: TextView = itemView.findViewById(R.id.article_view_count)
        val favouriteIcon: ImageView = itemView.findViewById(R.id.favourite_icon)
        val date: TextView = itemView.findViewById(R.id.article_date)
    }
}