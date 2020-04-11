/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.MenuItem;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

import propoid.db.Order;
import propoid.db.Reference;
import propoid.ui.list.MatchLookup;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.util.ChartUtils;


public class SnapshotsActivity extends AbstractActivity {

    public static final int VALUE_TEXT_SIZE = 12;

    public static final int LEGEND_TEXT_SIZE = 15;

    private Gym gym;

    private Workout workout;

    private LineChart chartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        Reference<Workout> reference = Reference.from(getIntent());
        workout = gym.get(reference);
        if (workout == null) {
            finish();
        } else {
            setTitle(DateUtils.formatDateTime(this, workout.start.get(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        }

        setContentView(R.layout.layout_snapshots);

        chartView = findViewById(R.id.chart);
        chartView.setScaleYEnabled(false);
        chartView.getDescription().setEnabled(false);
        chartView.getLegend().setTextColor(ContextCompat.getColor(this, R.color.design_default_color_primary));
        chartView.getLegend().setTextSize(LEGEND_TEXT_SIZE);

        chartView.getAxisLeft().setDrawGridLines(false);
        chartView.getAxisLeft().setTextSize(VALUE_TEXT_SIZE);

        chartView.getAxisRight().setDrawGridLines(false);
        chartView.getAxisRight().setTextSize(VALUE_TEXT_SIZE);

        chartView.getXAxis().setValueFormatter(new TimeValueFormatter());
        chartView.getXAxis().setTextSize(VALUE_TEXT_SIZE);
        chartView.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartView.getXAxis().setAxisMinimum(0f);

        ChartUtils.setTextColor(this, chartView);

        new SnapshotLookup().restartLoader(0, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class SnapshotLookup extends MatchLookup<Snapshot> {

        SnapshotLookup() {
            super(gym.getSnapshots(workout));

            setOrder(Order.ascendingByInsert());
        }

        @Override
        protected void onLookup(List<Snapshot> lookup) {

            List<ILineDataSet> dataSets = new ArrayList<>();

            LineDataSet pulse = createDataSet(lookup.size(), R.string.pulse_label, getResources().getColor(R.color.chart1));
            dataSets.add(pulse);

            LineDataSet strokeRate = createDataSet(lookup.size(), R.string.strokeRate_label, getResources().getColor(R.color.chart2));
            dataSets.add(strokeRate);

            LineDataSet power = createDataSet(lookup.size(), R.string.power_label, getResources().getColor(R.color.chart3));
            dataSets.add(power);

            LineDataSet speed = createDataSet(lookup.size(), R.string.speed_label, getResources().getColor(R.color.chart4));
            speed.setAxisDependency(YAxis.AxisDependency.RIGHT);
            dataSets.add(speed);

            for (int second = 0; second < lookup.size(); second++) {
                Snapshot snapshot = lookup.get(second);

                pulse.addEntry(new Entry(second, snapshot.pulse.get()));
                strokeRate.addEntry(new Entry(second, snapshot.strokeRate.get()));
                power.addEntry(new Entry(second, snapshot.power.get()));
                speed.addEntry(new Entry(second, snapshot.speed.get() / 100f));
            }

            chartView.setData(new LineData((dataSets)));
            chartView.invalidate();
        }

        private LineDataSet createDataSet(int size, int label, int color) {
            LineDataSet set = new LineDataSet(new ArrayList<Entry>(size), getString(label));
            set.setColor(color);
            set.setDrawCircles(false);
            set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

            return set;
        }
    }

    public static Intent createIntent(Context context, Workout workout) {
        Intent intent = new Intent(context, SnapshotsActivity.class);

        intent.setData(new Reference<>(workout).toUri());

        return intent;
    }

    private class TimeValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {

            int minutes = (int)(value / 60);
            int seconds = (int)(value % 60);

            return String.format("%d:%02d", minutes, seconds);
        }
    }
}