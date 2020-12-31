package net.babys_care.app.utils.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.babys_care.app.R
import net.babys_care.app.models.ChildGrowth

class LegendAdapter(val context: Context): RecyclerView.Adapter<LegendAdapter.LegendViewHolder>() {

    private val dataList: MutableList<ChildGrowth> = mutableListOf()
    private val heightColorList: MutableList<Int> = mutableListOf()
    private val weightColorList: MutableList<Int> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LegendViewHolder {
        return LegendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_growth_data, parent, false))
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LegendViewHolder, position: Int) {
        val child = dataList[position]
        holder.height.text = "${child.growthHistories.lastOrNull()?.height ?: "--"}"
        holder.weight.text = "${child.growthHistories.lastOrNull()?.weight ?: "--"}"
        holder.heightIndicator.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, heightColorList[position])))
        holder.weightIndicator.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, weightColorList[position])))
        val honorificTitle = when(child.gender) {
            "male" -> BabyInfoAdapter.context.getString(R.string.honorific_title_boy)
            else -> BabyInfoAdapter.context.getString(R.string.honorific_title_girl)
        }
        holder.heightLabel.text = "${child.firstNameKana}${honorificTitle}の身長(cm)"
        holder.weightLabel.text = "${child.firstNameKana}${honorificTitle}の体重(kg)"
    }

    fun addData(childGrowth: ChildGrowth, heightColor: Int, weightColor: Int) {
        dataList.add(childGrowth)
        heightColorList.add(heightColor)
        weightColorList.add(weightColor)
    }

    fun clearData() {
        dataList.clear()
        heightColorList.clear()
        weightColorList.clear()
    }

    inner class LegendViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val heightIndicator: CardView = itemView.findViewById(R.id.height_indicator_color)
        val weightIndicator: CardView = itemView.findViewById(R.id.weight_indicator_color)
        val height: TextView =  itemView.findViewById(R.id.baby_height_value)
        val weight: TextView =  itemView.findViewById(R.id.baby_weight_value)
        val heightLabel: TextView =  itemView.findViewById(R.id.baby_height_label)
        val weightLabel: TextView =  itemView.findViewById(R.id.baby_weight_label)
    }
}