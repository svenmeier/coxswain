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

import java.text.DecimalFormat;
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


public class StatisticsFragment extends Fragment implements View.OnClickListener {

    private Gym gym;

    private List<Statistic> pendings = new ArrayList<>();

    private Map<String, Statistic> statistics = new HashMap<>();

    private int highlight;

    private TextView titleView;

    private TimelineView timelineView;

    private StatisticLookup lookup;

    private Preference<Long> windowPreference;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);

        windowPreference = Preference.getLong(activity, R.string.preference_statistics_window).fallback(28 * TimelineView.DAY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_statistics, container, false);

        titleView = (TextView) root.findViewById(R.id.statistics_title);
        updateTitle();

        timelineView = (TimelineView) root.findViewById(R.id.statistics_timeline);
        timelineView.setPeriods(new StatisticPeriods());
        timelineView.setWindow(24 * TimelineView.DAY);
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

    private Statistic getMax(Class<?> unit) {
        String key = unit.getSimpleName();

        Statistic statistic = this.statistics.get(key);
        if (statistic == null) {
            statistic = new Statistic();

            statistics.put(key, statistic);
        }

        return statistic;
    }

    private Statistic getStatistics(Class<?> unit, long from, long to) {
        String key = from + ":" + to;

        Statistic statistic = this.statistics.get(key);
        if (statistic == null) {
            statistic = new Statistic();

            statistics.put(key, statistic);
            pendings.add(statistic);
        }

        if (pendings.contains(statistic) && lookup == null) {
            lookup = new StatisticLookup(from, to, statistic, getMax(unit));
            lookup.restartLoader(0, this);
        }

        statistic.animation = Math.min(statistic.animation + 0.05f, 1.0f);
        if (statistic.animation < 1.0f) {
            timelineView.postInvalidate();
        }

        return statistic;
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
                titleView.setText(R.string.target_duration);
                break;
            case 1:
                titleView.setText(R.string.target_distance);
                break;
            case 2:
                titleView.setText(R.string.target_strokes);
                break;
            case 3:
                titleView.setText(R.string.target_energy);
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

    }

    private class Statistic {

        float animation;

        public boolean found;

        public int duration;
        public int distance;
        public int strokes;
        public int energy;
    }

    private class StatisticLookup extends MatchLookup<Workout> {

        private final Statistic pending;

        private final Statistic max;

        public StatisticLookup(long from, long to, Statistic pending, Statistic max) {
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

            // release cursor
            workouts.clear();
        }
    }

    private class StatisticPeriods implements TimelineView.Periods {

        private NumberFormat numberFormat = NumberFormat.getNumberInstance();

        private Paint paint = new Paint();

        private float textSize = Utils.dpToPx(getActivity(), 20);

        private int border = Utils.dpToPx(getActivity(), 4);

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
			Statistic statistic = getStatistics(unit, from, to);
			Statistic max = getMax(unit);

			paintHeader(from, to, canvas, rect, statistic);

            rect.left += border;
            rect.top += border + textSize + border;
            rect.right -= border;
            rect.bottom -= border;
			paintBar(statistic.duration, max.duration, statistic.animation, canvas, rect, 0);
			paintBar(statistic.distance, max.distance, statistic.animation, canvas, rect, 1);
			paintBar(statistic.strokes, max.strokes, statistic.animation, canvas, rect, 2);
			paintBar(statistic.energy, max.energy, statistic.animation, canvas, rect, 3);
		}

        private void paintHeader(long from, long to, Canvas canvas, RectF rect, Statistic statistic) {
            if (statistic.found) {
                String what;
                switch (highlight) {
                    case 0:
                        what = numberFormat.format(statistic.duration / 60);
                        break;
                    case 1:
                        what = numberFormat.format(statistic.distance);
                        break;
                    case 2:
                        what = numberFormat.format(statistic.strokes);
                        break;
                    case 3:
                        what = numberFormat.format(statistic.energy);
                        break;
                    default:
                        throw new IndexOutOfBoundsException();
                }
                float whatWidth = paint.measureText(what);

                paint.setColor(0xff3567ed);
                paint.setTextSize(textSize);
                canvas.drawText(what, rect.right - border - whatWidth, rect.top + border + textSize, paint);
            }

            String when = DateUtils.formatDateRange(getActivity(), from, to, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
            paint.setColor(0xff000000);
            paint.setTextSize(textSize);
            canvas.drawText(when, rect.left + border, rect.top + border + textSize, paint);
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