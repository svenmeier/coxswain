package svenmeier.coxswain.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import svenmeier.coxswain.R;


public class ChartUtils {
	public static void setTextColor(Context context, Chart chartView) {

		TypedValue typedValue = new TypedValue();

		TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] { R.attr.editTextColor });
		int color = a.getColor(0, 0);
		a.recycle();

		if (chartView instanceof BarChart) {
			((BarChart)chartView).getAxisLeft().setTextColor(color);
			((BarChart)chartView).getAxisRight().setTextColor(color);
		}
		if (chartView instanceof BarLineChartBase) {
			((BarLineChartBase)chartView).getAxisLeft().setTextColor(color);
			((BarLineChartBase)chartView).getAxisRight().setTextColor(color);
		}

		chartView.getXAxis().setTextColor(color);
		chartView.getLegend().setTextColor(color);
		chartView.getDescription().setTextColor(color);
	}
}
