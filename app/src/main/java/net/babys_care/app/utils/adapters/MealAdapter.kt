package net.babys_care.app.utils.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.babys_care.app.R
import net.babys_care.app.models.MealDataModel

class MealAdapter(private val dataList: List<MealDataModel>) :
    RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    var onItemClick: ((MealDataModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        return MealViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_meal_recycler_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val data = dataList[position]
        holder.mealName.text = data.name
        Glide.with(holder.mealImage).load(data.image).apply(RequestOptions().skipMemoryCache(true))
            .into(holder.mealImage)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(data)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealImage: ImageView = itemView.findViewById(R.id.meal_image)
        val mealName: TextView = itemView.findViewById(R.id.meal_name)
    }

}