
package svenmeier.coxswain.view.charts;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.utils.Utils;

/**
 * The limit area is an additional feature for all Line-, Bar- and
 * ScatterCharts. It allows the displaying of an additional area in the chart
 * that marks a certain minium to maximum on the specified axis (x- or y-axis).
 *
 * @author Philipp Jahoda
 */
public class LimitArea extends LimitLine {

    /** minimum (the y-value or xIndex) */
    private float mMinimum = 0f;

    /**
     * Constructor with limit.
     */
    public LimitArea(float minimum, float maximum) {
        super(maximum);

        mMinimum = minimum;
    }

    /**
     * Constructor with limit and label.
     *
     * @param label - provide "" if no label is required
     */
    public LimitArea(float minimum, float maximum, String label) {
        super(maximum, label);

        mMinimum = mMinimum;
    }

    /**
     * Returns the minimum that is set for this line.
     *
     * @return
     */
    public float getMinimum() {
        return mMinimum;
    }
}
