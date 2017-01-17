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

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import propoid.ui.list.MatchLookup;
import propoid.util.content.Preference;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;


public class PerformanceFragment extends Fragment implements View.OnClickListener {

    private Gym gym;

    private List<Performance> pendings = new ArrayList<>();

    private Map<String, Performance> performances = new HashMap<>();

    private int highlight;

    private TextView titleView;

    private TimelineView timelineView;

    private PerformanceLookup lookup;

    private Preference<Long> windowPreference;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);

        windowPreference = Preference.getLong(activity, R.string.preference_performance_window).fallback(28 * TimelineView.DAY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_performance, container, false);

        titleView = (TextView) root.findViewById(R.id.performance_title);
        updateTitle();

        timelineView = (TimelineView) root.findViewById(R.id.performance_timeline);
        timelineView.setWindow(24 * TimelineView.DAY);
        timelineView.setPeriods(new PerformancePeriods());
        timelineView.setOnClickListener(this);

        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        windowPreference.set(timelineView.getWindow());
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        timelineView.setWindow(windowPreference.get());
    }

    private Performance getMax(Class<?> unit) {
        String key = unit.getSimpleName();

        Performance performancestic = this.performances.get(key);
        if (performancestic == null) {
            performancestic = new Performance();

            performances.put(key, performancestic);
        }

        return performancestic;
    }

    private Performance getPerformance(Class<?> unit, long from, long to) {
        String key = from + ":" + to;

        Performance performance = this.performances.get(key);
        if (performance == null) {
            performance = new Performance();

            performances.put(key, performance);
            pendings.add(performance);
        }

        if (pendings.contains(performance) && lookup == null) {
            lookup = new PerformanceLookup(from, to, performance, getMax(unit));
            lookup.restartLoader(0, this);
        }

        performance.animation = Math.min(performance.animation + 0.05f, 1.0f);
        if (performance.animation < 1.0f) {
            timelineView.postInvalidate();
        }

        return performance;
    }

    @Override
    public void onClick(View v) {
        highlight = (highlight + 1) % 4;

        timelineView.invalidate();
        updateTitle();
    }

    private void updateTitle() {
        switch (highlight) {
            case 0:
                titleView.setText(R.string.duration_label);
                break;
            case 1:
                titleView.setText(R.string.distance_label);
                break;
            case 2:
                titleView.setText(R.string.strokes_label);
                break;
            case 3:
                titleView.setText(R.string.energy_label);
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

    }

    private class Performance {

        float animation;

        public boolean found;

        public int duration;
        public int distance;
        public int strokes;
        public int energy;
    }

    private class PerformanceLookup extends MatchLookup<Workout> {

        private final Performance pending;

        private final Performance max;

        public PerformanceLookup(long from, long to, Performance pending, Performance max) {
            super(gym.getWorkouts(from, to));

            this.pending = pending;
            this.max = max;
        }

        @Override
        protected void onLookup(List<Workout> workouts) {
            // reset in case the lookup is done twice
            pending.distance = 0;
            pending.strokes = 0;
            pending.energy = 0;
            pending.duration = 0;
            pending.found = false;

            for (Workout workout : workouts) {
                pending.distance += workout.distance.get();
                pending.strokes += workout.strokes.get();
                pending.energy += workout.energy.get();
                pending.duration += workout.duration.get();
                pending.found = true;
            }

            max.distance = Math.max(max.distance, pending.distance);
            max.strokes = Math.max(max.strokes, pending.strokes);
            max.energy = Math.max(max.energy, pending.energy);
            max.duration = Math.max(max.duration, pending.duration);

            timelineView.postInvalidate();

            // no longer pending
            pendings.remove(pending);

            lookup = null;

            // recover cursor
            workouts.clear();
        }
    }

    private class PerformancePeriods implements TimelineView.Periods {

        private Paint paint = new Paint();

        private Paint.FontMetrics metrics = new Paint.FontMetrics();

        private float textSize = Utils.dpToPx(getActivity(), 20);

        private float padding = Utils.dpToPx(getActivity(), 4);

        @Override
        public long min() {
            return 0;
        }

        @Override
        public long max() {
            return System.currentTimeMillis();
        }

        @Override
        public long minWindow() {
            return TimelineView.DAY;
        }

        @Override
        public long maxWindow() {
            return 356 * TimelineView.DAY;
        }

        @Override
        public TimelineView.Unit unit(long time, long window) {
            long windowDays = window / TimelineView.DAY;
            if (windowDays > 60) {
                return new TimelineView.MonthUnit(time);
            } else if (windowDays > 10) {
                return new TimelineView.WeekUnit(time);
            } else {
                return new TimelineView.DayUnit(time);
            }
        }

        @Override
		public void paint(Class<?> unit, long from, long to, Canvas canvas, RectF rect) {
			Performance performance = getPerformance(unit, from, to);
			Performance max = getMax(unit);

			float headerHeight = paintHeader(from, to, canvas, rect, performance);

            rect.left += padding;
            rect.top += padding + headerHeight + padding;
            rect.right -= padding;
            rect.bottom -= padding;
			paintBar(performance.duration, max.duration, performance.animation, canvas, rect, 0);
			paintBar(performance.distance, max.distance, performance.animation, canvas, rect, 1);
			paintBar(performance.strokes, max.strokes, performance.animation, canvas, rect, 2);
			paintBar(performance.energy, max.energy, performance.animation, canvas, rect, 3);
		}

        private float paintHeader(long from, long to, Canvas canvas, RectF rect, Performance performance) {
            paint.setTextSize(textSize);
            paint.getFontMetrics(metrics);

            if (performance.found) {
                String what;
                switch (highlight) {
                    case 0:
                        what = String.format(getString(R.string.duration_minutes), performance.duration / 60);
                        break;
                    case 1:
                        what = String.format(getString(R.string.distance_meters), performance.distance);
                        break;
                    case 2:
                        what = String.format(getString(R.string.strokes_count), performance.strokes);
                        break;
                    case 3:
                        what = String.format(getString(R.string.energy_calories), performance.energy);
                        break;
                    default:
                        throw new IndexOutOfBoundsException();
                }
                float whatWidth = paint.measureText(what);

                paint.setColor(0xff3567ed);
                canvas.drawText(what, rect.right - padding - whatWidth, rect.top + padding - metrics.top, paint);
            }

            String when = DateUtils.formatDateRange(getActivity(), from, to, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
            paint.setColor(timelineView.getForegroundColor());
            paint.setTextSize(textSize);
            canvas.drawText(when, rect.left + padding, rect.top + padding - metrics.top, paint);

            return -metrics.top;
        }

        private void paintBar(int value, int max, float animation, Canvas canvas, RectF rect, int index) {
			paint.setStyle(Paint.Style.FILL);
			if (index == highlight) {
				paint.setColor(0x803567ed);
			} else {
				paint.setColor(0x403567ed);
			}

			float height = (rect.bottom - rect.top);
			float width = (rect.right - rect.left);

			float left = rect.left;
			float right = rect.left + (width * value * animation / max);
			float top = rect.top + (index * height / 5) + (index * height / 15);
			float bottom = rect.top + ((index + 1) * height / 5) + (index * height / 15);

			canvas.drawRect(left, top, right, bottom, paint);
		}
    }
}