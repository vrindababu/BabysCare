package net.babys_care.app.utils

class JapaneseCharacterUtils {

    /**
     * Function that will give corresponding japanese count characters for english numbers
     * @param start the starting value of count digits. Start range is limited to 1-20.
     * If passed start value is not in range, it will be adjusted to range.
     * @param end the ending value of count digits. The end value is limited to 1-20.
     * If passed end value is not in range, it will be adjusted to range.
     * @return it will return an Array<String> containing corresponding japanese characters
     */
    fun getJapaneseNumbers(start: Int = 1, end: Int = 20): Array<String> {
        val japaneseCounting = arrayOf(
            "一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十"
        )
        val startLimit = if (start < 1) 1 else if (start > 20) 20 else if (start > end) end else start
        val endLimit = if (end > 20) 20 else if (end < 1) 1 else if (end < startLimit) startLimit else end

        return japaneseCounting.copyOfRange(startLimit - 1, endLimit)
    }
}