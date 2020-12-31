package net.babys_care.app.scene.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.realm.Realm
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_baby_info_check.*
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.extensions.toDateJapaneseWithYear
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.utils.JapaneseCharacterUtils

class BabyInfoCheckFragment : BaseFragment() {

    override val layout = R.layout.fragment_baby_info_check

    private lateinit var realm: Realm
    private val childList: MutableList<ChildrenModel> = mutableListOf()
    var onFragmentAdd: ((Fragment) -> Unit)? = null

    override fun onResume() {
        when(val activity = activity) {
            is MainActivity -> {
                activity.addBackButtonAndActionToMain(true)
                activity.updateToolbarTitle(getString(R.string.confirm_baby_info))
            }
            is AccountSettingsActivity -> {
                activity.updateToolbarTitle(getString(R.string.confirm_baby_info))
            }
        }
        if (AppSetting(AppManager.context).babyInfoChanged == true) {
            setData()
        }
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        realm = Realm.getDefaultInstance()

        baby_info_add_link.setOnClickListener {
            if (activity is MainActivity) {
                onFragmentAdd?.invoke(BabyInfoEditFragment())
            } else {
                (activity as? AccountSettingsActivity)?.replaceFragment(BabyInfoEditFragment())
            }
        }

        populateChildRecyclerView()
        setData()
    }

    private fun populateChildRecyclerView() {
        child_info_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        child_info_recycler_view.adapter = ChildInfoAdapter().apply {
            onEditButtonClick = {childId ->
                val babyInfoEdit = BabyInfoEditFragment()
                babyInfoEdit.childId = childId
                if (activity is MainActivity) {
                    onFragmentAdd?.invoke(babyInfoEdit)
                } else {
                    (activity as? AccountSettingsActivity)?.replaceFragment(babyInfoEdit)
                }
            }
        }
    }

    private fun setData() {
        childList.clear()
        childList.addAll(realm.where(ChildrenModel::class.java).findAll())
        if (childList.isEmpty()) {
            button_add_baby_info.setOnClickListener {
                if (activity is MainActivity) {
                    onFragmentAdd?.invoke(BabyInfoEditFragment())
                } else {
                    (activity as? AccountSettingsActivity)?.replaceFragment(BabyInfoEditFragment())
                }
            }
            baby_info_add_link.visibility = View.GONE
            button_add_baby_info.visibility = View.VISIBLE
            no_baby_info.visibility = View.VISIBLE
        } else {
            button_add_baby_info.visibility = View.GONE
            no_baby_info.visibility = View.GONE
            baby_info_add_link.visibility = View.VISIBLE
        }
        child_info_recycler_view.adapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        realm.close()

        super.onDestroyView()
    }

    inner class ChildInfoAdapter: RecyclerView.Adapter<ChildInfoViewHolder>() {

        var onEditButtonClick: ((Int) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildInfoViewHolder {
            return ChildInfoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_child_info_list, parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ChildInfoViewHolder, position: Int) {
            val child = childList[position]
            holder.displayName.text = child.firstName
            holder.fullName.text = "${child.lastName} ${child.firstName}"
            holder.fullNameKana.text = "${child.lastNameKana} ${child.firstNameKana}"
            holder.gender.text = getChildGenderValue(child.gender)
            holder.birthday.text = child.birthDay.toDateJapaneseWithYear()
            holder.birthOrder.text = getChildBirthOrder(child.birthOrder)
            holder.siblingOrder.text = getChildSiblingOrder(child.siblingOrder, child.gender)

            val defaultImage = when(child.gender) {
                "male" -> ContextCompat.getDrawable(requireContext(), R.drawable.baby_boy_default_image)
                else -> ContextCompat.getDrawable(requireContext(), R.drawable.baby_girl_default_image)
            }
            val defaultHonorificTitle = when(child.gender) {
                "male" -> getString(R.string.honorific_title_boy)
                else -> getString(R.string.honorific_title_girl)
            }
            holder.honorificTitle.text = defaultHonorificTitle
            child.image?.let {
                Glide.with(requireContext()).load("${BuildConfig.URL_IMAGE_DIRECTORY}$it").apply(RequestOptions().placeholder(defaultImage)).into(holder.profileImage)
            } ?: kotlin.run {
                holder.profileImage.setImageDrawable(defaultImage)
            }

            holder.editChildInfoButton.setOnClickListener {
                onEditButtonClick?.invoke(child.childId)
            }
        }

        override fun getItemCount(): Int {
            return childList.size
        }

        private fun getChildGenderValue(gender: String): String {
            return when (gender) {
                "male" -> getString(R.string.baby_boy)
                "female" -> getString(R.string.baby_girl)
                else -> getString(R.string.other)
            }
        }

        private fun getChildBirthOrder(birthOrder: Int): String {
            val japaneseCounting = JapaneseCharacterUtils().getJapaneseNumbers(1, 10)
            return String.format(getString(R.string.child_birth_order), japaneseCounting[if (birthOrder > 10) 9 else birthOrder - 1])
        }

        private fun getChildSiblingOrder(siblingOrder: Int, gender: String): String {
            val japaneseNumbers = JapaneseCharacterUtils().getJapaneseNumbers(1, 10)
            val order = if (siblingOrder > 10) 10 else siblingOrder
            return if (gender == "male") {
                when(order) {
                    1 -> getString(R.string.eldest_son)
                    2 -> getString(R.string.second_son)
                    else -> String.format(getString(R.string.n_son), japaneseNumbers[order - 1])
                }
            } else {
                when(order) {
                    1 -> getString(R.string.eldest_daughter)
                    2 -> getString(R.string.second_daughter)
                    else -> String.format(getString(R.string.n_daughter), japaneseNumbers[order - 1])
                }
            }
        }

    }

    inner class ChildInfoViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val displayName: TextView = itemView.findViewById(R.id.display_name)
        val honorificTitle: TextView = itemView.findViewById(R.id.honorific_title)
        val fullName: TextView = itemView.findViewById(R.id.full_name)
        val fullNameKana: TextView = itemView.findViewById(R.id.full_name_kana)
        val gender: TextView = itemView.findViewById(R.id.gender)
        val birthday: TextView = itemView.findViewById(R.id.birthday)
        val birthOrder: TextView = itemView.findViewById(R.id.birth_order)
        val siblingOrder: TextView = itemView.findViewById(R.id.sibling_order)
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val editChildInfoButton: Button = itemView.findViewById(R.id.button_edit)
    }
}