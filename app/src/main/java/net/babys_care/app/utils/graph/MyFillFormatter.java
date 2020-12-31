package net.babys_care.app.utils.graph;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.List;

/**
 * Created by Vrinda R Babu on 09 November, 2020.
 * Package net.babys_care.app.scene.average;
 * Project MpChartApp
 */
public class MyFillFormatter implements IFillFormatter {
    private final ILineDataSet boundaryDataSet;

    //Pass the dataSet of other line in the Constructor
    public MyFillFormatter(ILineDataSet boundaryDataSet) {
        this.boundaryDataSet = boundaryDataSet;
    }

    //Define a new method which is used in the LineChartRenderer
    public List<Entry> getFillLineBoundary() {
        if(boundaryDataSet != null) {
            return ((LineDataSet) boundaryDataSet).getValues();
        }
        return null;
    }

    @Override
    public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
        return 0;
    }
}