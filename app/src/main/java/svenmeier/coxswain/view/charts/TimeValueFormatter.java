package svenmeier.coxswain.view.charts;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class TimeValueFormatter extends ValueFormatter {
    @Override
    public String getFormattedValue(float value) {

        int minutes = (int)(value / 60);
        int seconds = (int)(value % 60);

        return String.format("%d:%02d", minutes, seconds);
    }
}
