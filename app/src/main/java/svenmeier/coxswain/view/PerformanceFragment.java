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
package svenmeier.coxswain.view;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import propoid.ui.list.GenericRecyclerAdapter;
import propoid.ui.list.MatchLookup;
import propoid.util.content.Preference;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.util.ChartUtils;

public class PerformanceFragment extends Fragment implements Gym.Listener {

    public static final int VALUE_TEXT_SIZE = 12;

    public static final int DESCRIPTION_TEXT_SIZE = 15;

    private Gym gym;

    private Preference<TimeUnit> unitPreference;

    private Performance max = new Performance(-1);

    private List<Performance> pendings = new ArrayList<>();

    private Map<Integer, Performance> performances = new HashMap<>();

    private PerformanceLookup lookup;

    private RecyclerView chartsView;

    private ChartsAdapter adapter;

    private TimeUnit unit = TimeUnit.DAY;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        gym = Gym.instance(context);

        unitPreference = Preference.getEnum(context, TimeUnit.class, R.string.preference_performance_unit).fallback(TimeUnit.DAY);
        unit = unitPreference.get();

        gym.addListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        gym.removeListener(this);

        super.onDetach();
    }


    @Override
    public void changed(Object scope) {
        if (scope == null) {
            max.reset();
            pendings.clear();
            performances.clear();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_performance, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        unit.prepare(menu.findItem(R.id.action_unit));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_unit) {
            unit = unit.next();
            unitPreference.set(unit);

            unit.prepare(item);

            changed(null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_performance, container, false);

        chartsView = root.findViewById(R.id.charts);
        chartsView.setLayoutManager(new LinearLayoutManager(getContext()));
        chartsView.setHasFixedSize(true);
        chartsView.setAdapter(adapter = new ChartsAdapter());

        return root;
    }

    private class ChartsAdapter extends GenericRecyclerAdapter<Long> {

        public ChartsAdapter() {
            super(R.layout.layout_performance_item, null);
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }

        @Override
        protected Long getItem(int position) {
            return Long.valueOf(position);
        }

        @Override
        protected GenericHolder createHolder(View v) {
            return new ChartHolder(v);
        }
    }

    private class ChartHolder extends GenericRecyclerAdapter.GenericHolder<Long> {

        private final HorizontalBarChart chartView;

        public ChartHolder(View view) {
            super(view);

            chartView = view.findViewById(R.id.chart);
            chartView.getLegend().setEnabled(false);
            chartView.setTouchEnabled(false);
            chartView.setDrawValueAboveBar(false);

            chartView.getDescription().setTextSize(DESCRIPTION_TEXT_SIZE);
            chartView.getDescription().setPosition(30, 30);
            chartView.getDescription().setTextAlign(Paint.Align.LEFT);

            final String[] strings = new String[]{
                    getString(R.string.duration_label),
                    getString(R.string.distance_label),
                    getString(R.string.energy_label),
                    getString(R.string.strokes_label)};
            chartView.getXAxis().setGranularity(1f);
            chartView.getXAxis().setValueFormatter(new IndexAxisValueFormatter(strings));
            chartView.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            chartView.getXAxis().setDrawGridLines(false);
            chartView.getXAxis().setTextSize(VALUE_TEXT_SIZE);

            chartView.getAxisLeft().setAxisMinimum(0f);
            chartView.getAxisLeft().setAxisMaximum(1f);
            chartView.getAxisLeft().setDrawGridLines(false);
            chartView.getAxisLeft().setDrawLabels(false);

            chartView.getAxisRight().setEnabled(false);

            ChartUtils.setTextColor(getActivity(), chartView);
        }

        @Override
        protected void onBind() {
            Performance performance = getPerformance(getAdapterPosition());

            BarDataSet dataSet = createDataSet(R.string.performance, 0);
            dataSet.addEntry(new BarEntry(0, relative(performance.duration, max.duration), formatSeconds(performance.duration)));
            dataSet.addEntry(new BarEntry(1, relative(performance.distance, max.distance), formatNumber(performance.distance)));
            dataSet.addEntry(new BarEntry(2, relative(performance.energy,max.energy), formatNumber(performance.energy)));
            dataSet.addEntry(new BarEntry(3, relative(performance.strokes, max.strokes), formatNumber(performance.strokes)));
            dataSet.setColors(getColor(R.color.chart1),
                    getColor(R.color.chart2),
                    getColor(R.color.chart3),
                    getColor(R.color.chart4));

            BarData data = new BarData(dataSet);
            data.setBarWidth(0.8f);
            data.setValueTextSize(VALUE_TEXT_SIZE);
            data.setValueTypeface(Typeface.DEFAULT_BOLD);
            data.setValueFormatter(new DataToStringFormatter());
            data.setValueTextColor(getColor(R.color.chart_value));
            chartView.setData(data);

            long from = unit.getFrom(performance);
            long to = unit.getTo(performance);
            String description = DateUtils.formatDateRange(getActivity(), from, to, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
            chartView.getDescription().setText(description);
        }

        private float relative(int actual, int max) {
            if (max == 0) {
                return 0;
            }

            return ((float)actual) / max;
        }

        private BarDataSet createDataSet(int label, int color) {
            BarDataSet set = new BarDataSet(new ArrayList<BarEntry>(), getString(label));

            set.setColor(color);

            return set;
        }
    }

    private Format numberFormat = NumberFormat.getNumberInstance();

    private String formatNumber(int value) {
        return numberFormat.format(value);
    }

    private String formatSeconds(int value) {

        int seconds = Math.round(value % 60);
        int minutes = Math.round((value / 60) % 60);
        int hours = Math.round((value / 60 / 60) % 60);

        if (hours == 0f) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    private int getColor(int p) {
        return getContext().getResources().getColor(p);
    }

    private Performance getPerformance(int time) {
        Performance performance = this.performances.get(time);
        if (performance == null) {
            performance = new Performance(time);

            performances.put(time, performance);
            pendings.add(performance);
        }

        checkPending();

        return performance;
    }

    private void checkPending() {
        if (pendings.isEmpty() == false && lookup == null) {
            Performance performance = pendings.get(pendings.size() - 1);

            lookup = new PerformanceLookup(performance, unit.getFrom(performance), unit.getTo(performance));
            lookup.restartLoader(0, this);
        }

    }

    private class Performance {

        int time;

        public int duration;
        public int distance;
        public int strokes;
        public int energy;

        public Performance(int time) {
            this.time = time;
        }

        public void reset() {
            distance = 0;
            strokes = 0;
            energy = 0;
            duration = 0;
        }
    }

    private class PerformanceLookup extends MatchLookup<Workout> {

        private final Performance pending;

        public PerformanceLookup(Performance pending, long from, long to) {
            super(gym.getWorkouts(from, to));

            this.pending = pending;
        }

        @Override
        protected void onLookup(List<Workout> workouts) {

            // reset in case the lookup is done twice
            pending.reset();
            for (Workout workout : workouts) {
                pending.distance += workout.distance.get();
                pending.strokes += workout.strokes.get();
                pending.energy += workout.energy.get();
                pending.duration += workout.duration.get();
            }
            max.distance = Math.max(max.distance, pending.distance);
            max.strokes = Math.max(max.strokes, pending.strokes);
            max.energy = Math.max(max.energy, pending.energy);
            max.duration = Math.max(max.duration, pending.duration);

            // no longer pending
            pendings.remove(pending);

            adapter.notifyDataSetChanged();

            // recover cursor
            workouts.clear();
            
            lookup = null;
            checkPending();
        }
    }

    private enum TimeUnit {

        DAY {
            @Override
            public void prepare(MenuItem item) {
                item.setTitle(R.string.action_day);
                item.setIcon(R.drawable.baseline_today_white_24);
            }

            @Override
            public long getFrom(Performance performance) {

                Calendar calendar = calendar();
                calendar.add(Calendar.DATE, -performance.time);
                return calendar.getTimeInMillis();
            }

            @Override
            public long getTo(Performance performance) {

                Calendar calendar = calendar();
                calendar.add(Calendar.DATE, -performance.time + 1);
                return calendar.getTimeInMillis();
            }

            @Override
            public TimeUnit next() {
                return WEEK;
            }
        },

        WEEK {

            @Override
            public void prepare(MenuItem item) {
                item.setTitle(R.string.action_week);
                item.setIcon(R.drawable.baseline_week_white_24);
            }

            @Override
            public long getFrom(Performance performance) {
                Calendar calendar = calendar();

                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.add(Calendar.WEEK_OF_YEAR, -performance.time);
                return calendar.getTimeInMillis();
            }

            @Override
            public long getTo(Performance performance) {
                Calendar calendar = calendar();

                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.add(Calendar.WEEK_OF_YEAR, -performance.time + 1);
                return calendar.getTimeInMillis();
            }

            @Override
            public TimeUnit next() {
                return MONTH;
            }
        },

        MONTH {

            @Override
            public void prepare(MenuItem item) {
                item.setTitle(R.string.action_month);
                item.setIcon(R.drawable.baseline_month_white_24);
            }

            @Override
            public long getFrom(Performance performance) {
                Calendar calendar = calendar();

                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.MONTH, -performance.time);
                return calendar.getTimeInMillis();
            }

            @Override
            public long getTo(Performance performance) {
                Calendar calendar = calendar();

                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.MONTH, -performance.time + 1);
                return calendar.getTimeInMillis();
            }

            @Override
            public TimeUnit next() {
                return DAY;
            }
        };

        private Calendar calendar = Calendar.getInstance();

        protected Calendar calendar() {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar;
        }

        public abstract void prepare(MenuItem item);

        public abstract long getFrom(Performance performance);

        public abstract long getTo(Performance performance);

        public abstract TimeUnit next();
    }

    private class DataToStringFormatter extends ValueFormatter {

        @Override
        public String getBarLabel(BarEntry barEntry) {
            if (barEntry.getY() < 0.2f) {
                // prevent label from overlapping into x-axis
                return "";
            }
            return barEntry.getData().toString();
        }
    }
}