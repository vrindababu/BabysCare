package net.babys_care.app.scene.news

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.extensions.toDotDateFormat
import net.babys_care.app.models.realmmodels.NewsModel

class NewsAdapter : RecyclerView.Adapter<NewsAdapter.CustomViewHolder>() {

    var onItemClick: ((NewsModel, Int) -> Unit)? = null
    var newsList: List<NewsModel> = listOf()

    // ViewHolderクラス(別ファイルに書いてもOK)
    class CustomViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val releaseStartAt: TextView = view.findViewById(R.id.release_start_at)
        val image: ImageView = view.findViewById(R.id.list_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val item = layoutInflater.inflate(R.layout.adapter_news, parent, false)
        return CustomViewHolder(item)
    }

    override fun getItemCount(): Int {
        return newsList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val news = newsList[position]
        holder.title.text = news.title
        holder.releaseStartAt.text = news.releaseStartAt.toDotDateFormat()
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(news, position)
        }

        news.listImage?.let {imageUrl ->
            Glide.with(holder.itemView).load("${BuildConfig.URL_IMAGE_DIRECTORY}$imageUrl").apply(RequestOptions()
                .placeholder(R.drawable.ic_default_news_list)).into(holder.image)
        } ?: kotlin.run {
            Glide.with(holder.itemView).load(R.drawable.ic_default_news_list).into(holder.image)
        }
    }

    fun setData(list: List<NewsModel>) {
        newsList = list
    }
}
