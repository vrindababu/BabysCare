package net.babys_care.app.utils.graph

import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class BabyXAxisValueFormatter(private val labels: List<String>, private val interval: Int) : ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        val valueInt = value.toInt()
        return if (valueInt % interval == 0 && valueInt >= 0) {
            if (labels.size in 28..31 && valueInt<= labels.size) {
               labels[valueInt - 1]
            } else {
                val index = valueInt % labels.size
                labels[index]
            }
        } else {
            ""
        }
    }
}