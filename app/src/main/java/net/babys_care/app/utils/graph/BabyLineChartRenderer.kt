package net.babys_care.app.utils.graph

import android.graphics.Canvas
import android.graphics.Path
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

class BabyLineChartRenderer(
    chart: LineDataProvider?,
    animator: ChartAnimator?,
    viewPortHandler: ViewPortHandler?
): LineChartRenderer(chart, animator, viewPortHandler) {

    //This method is same as it's parent implementation
    override fun drawLinearFill(
        c: Canvas?,
        dataSet: ILineDataSet,
        trans: Transformer,
        bounds: XBounds
    ) {
        val filled: Path = mGenerateFilledPathBuffer
        val startingIndex = bounds.min
        val endingIndex = bounds.range + bounds.min
        val indexInterval = 128

        var currentStartIndex: Int
        var currentEndIndex: Int
        var iterations = 0

        // Doing this iteratively in order to avoid OutOfMemory errors that can happen on large bounds sets.
        do {
            currentStartIndex = startingIndex + iterations * indexInterval
            currentEndIndex = currentStartIndex + indexInterval
            currentEndIndex = if (currentEndIndex > endingIndex) endingIndex else currentEndIndex
            if (currentStartIndex <= currentEndIndex) {
                if (dataSet.fillFormatter is MyFillFormatter) {
                    super.drawLinearFill(c, dataSet, trans, bounds)
                } else {
                    generateFilledPath(dataSet, currentStartIndex, currentEndIndex, filled)
                }
                trans.pathValueToPixel(filled)
                val drawable = dataSet.fillDrawable
                if (drawable != null) {
                    drawFilledPath(c, filled, drawable)
                } else {
                    drawFilledPath(c, filled, dataSet.fillColor, dataSet.fillAlpha)
                }
            }
            iterations++
        } while (currentStartIndex <= currentEndIndex)
    }

    //This is where we define the area to be filled.
    private fun generateFilledPath(
        dataSet: ILineDataSet,
        startIndex: Int,
        endIndex: Int,
        outputPath: Path
    ) {
        //Call the custom method to retrieve the dataSet for other line
        val boundaryEntry: List<Entry>? =
            (dataSet.fillFormatter as BabyFillFormatter).fillLineBoundary
        val phaseY = mAnimator.phaseY
        val filled: Path = outputPath
        filled.reset()
        val entry: Entry = dataSet.getEntryForIndex(startIndex)
        boundaryEntry?.get(0)?.y?.let { filled.moveTo(entry.x, it) }
        filled.lineTo(entry.x, entry.y * phaseY)

        // create a new path
        var currentEntry: Entry? = null
        var previousEntry: Entry? = null
        for (x in startIndex + 1..endIndex) {
            currentEntry = dataSet.getEntryForIndex(x)
            filled.lineTo(currentEntry.x, currentEntry.y * phaseY)
        }

        // close up
        if (currentEntry != null && previousEntry != null) {
            filled.lineTo(currentEntry.x, previousEntry.y)
        }

        //Draw the path towards the other line
        for (x in endIndex downTo startIndex + 1) {
            if (previousEntry != null) {
                previousEntry = boundaryEntry?.get(x)
            }
            previousEntry?.x?.let { filled.lineTo(it, previousEntry.y * phaseY) }
        }
        filled.close()
    }
}
