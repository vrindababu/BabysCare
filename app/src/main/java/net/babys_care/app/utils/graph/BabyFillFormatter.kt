package net.babys_care.app.utils.graph

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

class BabyFillFormatter @JvmOverloads constructor(private val iLineDataSet: ILineDataSet? = null): IFillFormatter {

    override fun getFillLinePosition(
        dataSet: ILineDataSet?,
        dataProvider: LineDataProvider?
    ): Float {
        return 0f
    }

    val fillLineBoundary: List<Entry>?
        get() = if (iLineDataSet != null) {
            (iLineDataSet as LineDataSet).values
        } else {
            null
        }
}