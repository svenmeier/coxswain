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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

import propoid.db.Order;
import propoid.db.Reference;
import propoid.ui.list.MatchLookup;
import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.rower.Distance;
import svenmeier.coxswain.rower.Duration;
import svenmeier.coxswain.rower.Energy;
import svenmeier.coxswain.rower.Stroke;
import svenmeier.coxswain.util.ChartUtils;
import svenmeier.coxswain.view.charts.LimitArea;
import svenmeier.coxswain.view.charts.TimeValueFormatter;
import svenmeier.coxswain.view.charts.XAxisRenderer2;


public class SnapshotsActivity extends AbstractActivity implements CompoundButton.OnCheckedChangeListener {

    public static final int VALUE_TEXT_SIZE = 12;

    public static final int LEGEND_TEXT_SIZE = 15;

    private Gym gym;

    private Workout workout;

    private LineChart chartView;

    private Preference<Boolean> showRests;
    private Preference<Integer> splitDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        showRests = Preference.getBoolean(this, R.string.preference_snapshots_show_rests).fallback(true);
        splitDistance = Preference.getInt(this, R.string.preference_split_distance).fallback(500);

        Reference<Workout> reference = Reference.from(getIntent());
        workout = gym.get(reference);
        if (workout == null) {
            finish();
            return;
        } else {
            setTitle(DateUtils.formatDateTime(this, workout.start.get(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
        }

        setContentView(R.layout.layout_snapshots);

        chartView = findViewById(R.id.chart);
        chartView.setScaleYEnabled(false);
        chartView.getDescription().setEnabled(false);
        chartView.getLegend().setTextColor(ContextCompat.getColor(this, R.color.design_default_color_primary));
        chartView.getLegend().setTextSize(LEGEND_TEXT_SIZE);
        chartView.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chartView.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);

        chartView.getAxisLeft().setAxisMinimum(0f);
        chartView.getAxisLeft().setDrawGridLines(false);
        chartView.getAxisLeft().setTextSize(VALUE_TEXT_SIZE);

        chartView.getAxisRight().setAxisMinimum(0f);
        chartView.getAxisRight().setDrawGridLines(false);
        chartView.getAxisRight().setTextSize(VALUE_TEXT_SIZE);

        chartView.getXAxis().setValueFormatter(new TimeValueFormatter());
        chartView.getXAxis().setTextSize(VALUE_TEXT_SIZE);
        chartView.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartView.getXAxis().setAxisMinimum(0f);
        chartView.setXAxisRenderer(new  XAxisRenderer2(chartView));

        ChartUtils.setTextColor(this, chartView);

        CheckBox checkBox = findViewById(R.id.snapshots_show_rests);
        checkBox.setChecked(showRests.get());
        checkBox.setOnCheckedChangeListener(this);

        reload();
    }

    private void reload() {
        new SnapshotLookup().restartLoader(0, this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        showRests.set(isChecked);

        reload();
    }

    private class SnapshotLookup extends MatchLookup<Snapshot> {

        SnapshotLookup() {
            super(gym.getSnapshots(workout));

            setOrder(Order.ascendingByInsert());
        }

        @Override
        protected void onLookup(List<Snapshot> lookup) {

            chartView.getXAxis().removeAllLimitLines();

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

            final boolean showRests = SnapshotsActivity.this.showRests.get();
            int distance = workout.distance.get();
            int duration = workout.duration.get();
            int powerSum = 0;
            int strokeRateSum = 0;

            int restStart = -1;
            int second = 0;
            Snapshot previous = new Snapshot();
            for (Snapshot snapshot : lookup) {
                if (restStart == -1 && snapshot.difficulty.get() == Difficulty.REST) {
                    restStart = second;
                } else if (restStart != -1 && snapshot.difficulty.get() != Difficulty.REST) {
                    addRest(restStart, second, showRests);
                    restStart = -1;
                }

                if (restStart == -1 || showRests) {
                    pulse.addEntry(new Entry(second, snapshot.pulse.get()));
                    strokeRate.addEntry(new Entry(second, snapshot.strokeRate.get()));
                    power.addEntry(new Entry(second, snapshot.power.get()));
                    speed.addEntry(new Entry(second, snapshot.speed.get() / 100f));
                    powerSum += snapshot.power.get();
                    strokeRateSum += snapshot.strokeRate.get();
                    second++;
                } else {
                    duration -= 1;
                    distance -= snapshot.distance.get() - previous.distance.get();
                }

                previous = snapshot;
            }

            if (restStart != -1) {
                addRest(restStart, second, showRests);
            }

            LineData data = new LineData(dataSets);
            data.setHighlightEnabled(false);
            chartView.setData(data);

            chartView.invalidate();

            summary(duration, distance, powerSum, strokeRateSum);
        }

        private void addRest(int restStart, int second, boolean showRests) {
            if (showRests) {
                LimitArea limitArea = new LimitArea(restStart, second);
                limitArea.setLineColor(getResources().getColor(R.color.chart_rest_show));
                chartView.getXAxis().addLimitLine(limitArea);
            } else {
                LimitLine limitLine = new LimitLine(second);
                limitLine.setLineColor(getResources().getColor(R.color.chart_rest_hide));
                limitLine.setLineWidth(4f);
                chartView.getXAxis().addLimitLine(limitLine);
            }
        }

        private LineDataSet createDataSet(int size, int label, int color) {
            LineDataSet set = new LineDataSet(new ArrayList<Entry>(size), getString(label));
            set.setColor(color);
            set.setDrawCircles(false);
            set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

            return set;
        }
    }

    private void summary(int duration, int distance, int powerSum, int strokeRateSum) {
        this.<TextView>findViewById(R.id.snapshots_distance).setText(Distance.m(this, distance).formatted().replace(" ", "\u0009\u0009"));
        this.<TextView>findViewById(R.id.snapshots_duration).setText(Duration.seconds(this, duration).formatted());
        this.<TextView>findViewById(R.id.snapshots_energy).setText(Energy.kcal(this, workout.energy.get()).formatted());
        this.<TextView>findViewById(R.id.snapshots_strokes).setText(Stroke.count(this, workout.strokes.get()).formatted());
        this.<TextView>findViewById(R.id.snapshots_average_speed).setText(String.format(getString(R.string.speed_metersPerSecond_abbr), 1f * distance / duration));
        this.<TextView>findViewById(R.id.snapshots_average_split).setText(Duration.seconds(this, devide(splitDistance.get() * duration, distance)).formatted());
        this.<TextView>findViewById(R.id.snapshots_average_power).setText(String.format(getString(R.string.power_watts_abbr), devide(powerSum,  duration)));
        this.<TextView>findViewById(R.id.snapshots_average_strokerate).setText(String.format(getString(R.string.strokeRate_strokesPerMinute_abbr), devide(strokeRateSum, duration)));
    }

    private int devide(int dividend, int divisor) {
        if (divisor == 0) {
            return 0;
        }

        return dividend / divisor;
    }

    public static Intent createIntent(Context context, Workout workout) {
        Intent intent = new Intent(context, SnapshotsActivity.class);

        intent.setData(new Reference<>(workout).toUri());

        return intent;
    }

    public static Intent createIntent(Context context, Reference<Workout> workout) {
        Intent intent = new Intent(context, SnapshotsActivity.class);

        intent.setData(workout.toUri());

        return intent;
    }
}