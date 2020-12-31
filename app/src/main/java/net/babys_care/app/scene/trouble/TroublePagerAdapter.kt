package net.babys_care.app.scene.trouble

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class TroublePagerAdapter(
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> Fragment()
            1 -> Fragment()
            7 -> Fragment()
            8 -> Fragment()
            2 -> ExerciseFragment()
            3 -> MealFragment()
            4 -> SymptomSearchFragment()
            5 -> ExcretionFragment()
            6 -> SleepFragment()
            else -> Fragment()
        }
    }

    override fun getCount(): Int {
        return 9
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when(position) {
            0 -> ""
            1 -> ""
            7 -> ""
            8 -> ""
            2 -> "運動"
            3 -> "食事"
            4 -> "症状から探す"
            5 -> "排泄"
            6 -> "睡眠"
            else -> ""
        }
    }
}