package net.babys_care.app.utils.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.babys_care.app.AppManager
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.extensions.toDateJapaneseWithYear
import net.babys_care.app.models.ChildGrowth
import net.babys_care.app.utils.DateUtils
import net.babys_care.app.utils.JapaneseCharacterUtils
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

class BabyInfoAdapter : RecyclerView.Adapter<BabyInfoAdapter.BabyInfoViewHolder>() {

    private var childData: List<ChildGrowth> = listOf()
    var onProfileSettingClick: ((ChildGrowth)-> Unit)? = null
    var onInputLinkClick: ((ChildGrowth)-> Unit)? = null
    private var dateUtils: DateUtils = DateUtils()
    private val dateFormat1 = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
    private val dateFormat2 = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
    private val currentDate = Calendar.getInstance()
    private val birthCalender = Calendar.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BabyInfoViewHolder {
        return BabyInfoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_baby_info_with_growth_data, parent, false))
    }

    override fun getItemCount(): Int {
        return childData.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BabyInfoViewHolder, position: Int) {
        val child = childData[position]
        holder.babyName.text = child.firstNameKana
        holder.birthdayGender.text = "生年月日：${child.birthDay.toDateJapaneseWithYear()}\n" +
                "性別：${getFormattedGender(child.gender)}　${getFormattedBirthOrder(child.birthOrder)}（${getFormattedSiblingOrder(child.siblingOrder, child.gender)}）"
        holder.height.text = "${child.growthHistories.lastOrNull()?.height ?: "ー"}"
        holder.weight.text = "${child.growthHistories.lastOrNull()?.weight ?: "ー"}"
        val formatter = if (child.birthDay.contains("/")) dateFormat1 else dateFormat2
        birthCalender.time = formatter.parse(child.birthDay) ?: currentDate.time
        holder.ageInMonth.text = "${dateUtils.getDifferenceInMonths(birthCalender, currentDate)}"
        birthCalender.add(Calendar.MONTH, -1)
        holder.ageInDays.text = "${dateUtils.getDayDifference(currentDate, birthCalender)}"

        val defaultImage = when(child.gender) {
            "male" -> R.drawable.baby_boy_default_image
            else -> R.drawable.baby_girl_default_image
        }
        val defaultHonorificTitle = when(child.gender) {
            "male" -> context.getString(R.string.honorific_title_boy)
            else -> context.getString(R.string.honorific_title_girl)
        }
        holder.honorificTitle.text = defaultHonorificTitle
        child.image?.let { image ->
            debugLogInfo("Image-> $image")
            Glide.with(holder.babyImage).load("${BuildConfig.URL_IMAGE_DIRECTORY}$image").apply(RequestOptions().placeholder(defaultImage)).into(holder.babyImage)
        } ?: kotlin.run {
            holder.babyImage.setImageResource(defaultImage)
        }
        holder.profileSettingLink.setOnClickListener {
            onProfileSettingClick?.invoke(child)
        }
        holder.inputLink.setOnClickListener {
            onInputLinkClick?.invoke(child)
        }
    }

    private fun getFormattedGender(gender: String): String {
        return when(gender) {
            "male" -> "男"
            "female" -> "女"
            else -> "その他"
        }
    }

    fun setBabyInfoData(list: List<ChildGrowth>) {
        childData = list
        notifyDataSetChanged()
    }

    inner class BabyInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val babyName: TextView = itemView.findViewById(R.id.baby_name)
        val honorificTitle: TextView = itemView.findViewById(R.id.honorific_title)
        val babyImage: ImageView = itemView.findViewById(R.id.baby_image)
        val birthdayGender: TextView = itemView.findViewById(R.id.birthday_gender)
        val profileSettingLink: TextView = itemView.findViewById(R.id.profile_settings_link)
        val height: TextView = itemView.findViewById(R.id.height_value)
        val ageInMonth: TextView = itemView.findViewById(R.id.after_birth_month_value)
        val ageInDays: TextView = itemView.findViewById(R.id.after_birth_day_value)
        val weight: TextView = itemView.findViewById(R.id.weight_value)
        val inputLink: TextView = itemView.findViewById(R.id.weigh_height_input_link)
    }

    companion object {
        private val japaneseCounting = JapaneseCharacterUtils().getJapaneseNumbers(1, 10)
        val context = AppManager.context

        fun getFormattedBirthOrder(birthOrder: Int): String {
            val index = if (birthOrder > 10) 9 else { birthOrder - 1 }
            return String.format(AppManager.context.getString(R.string.child_birth_order), japaneseCounting[index])
        }

        fun getFormattedSiblingOrder(siblingOrder: Int, gender: String): String {
            val index = if (siblingOrder > 10) 9 else { siblingOrder - 1 }
            return when (gender) {
                "male" -> {
                    when (siblingOrder) {
                        1 -> context.getString(R.string.eldest_son)
                        2 -> context.getString(R.string.eldest_son)
                        else -> String.format(context.getString(R.string.n_son), japaneseCounting[index])
                    }
                }
                "female" -> {
                    when (siblingOrder) {
                        1 -> context.getString(R.string.eldest_daughter)
                        2 -> context.getString(R.string.eldest_daughter)
                        else -> String.format(context.getString(R.string.n_daughter), japaneseCounting[index])
                    }
                }
                else -> {
                    when {
                        siblingOrder == 1 -> context.getString(R.string.eldest_son)
                        siblingOrder == 2 -> context.getString(R.string.second_son)
                        siblingOrder < 6 -> String.format(context.getString(R.string.n_son), japaneseCounting[index])
                        else -> String.format(context.getString(R.string.n_daughter), japaneseCounting[index])
                    }
                }
            }
        }
    }
}