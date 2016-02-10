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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import propoid.db.Match;
import propoid.ui.list.MatchLookup;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;


public class StatisticsFragment extends Fragment implements View.OnClickListener {

    private Gym gym;

    private List<Statistic> pendings = new ArrayList<>();

    private Map<String, Statistic> statistics = new HashMap<>();

    private int highlight;

    private TimelineView timelineView;

    private StatisticLookup lookup;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_statistics, container, false);

        timelineView = (TimelineView) root.findViewById(R.id.timeline);
        timelineView.setOnClickListener(this);
        timelineView.setPainter(new TimelineView.Painter() {

            private float textSize;
            private Paint paint = new Paint();

            private int border;

            {
                border = Utils.dpToPx(getActivity(), 4);
                textSize = Utils.dpToPx(getActivity(), 20);
            }

            @Override
            public void paint(long from, long to, Canvas canvas, RectF rect) {
                Statistic statistic = getStatistics(from, to);
                Statistic max = getMax(to - from);

                rect.left += border;
                rect.top += border;
                rect.right -= border;
                rect.bottom -= border;

                paintHeader(from, to, canvas, rect, statistic);

                rect.top += textSize + border;

                paint(statistic.duration, max.duration, canvas, rect, 0);
                paint(statistic.distance, max.distance, canvas, rect, 1);
                paint(statistic.strokes, max.strokes, canvas, rect, 2);
                paint(statistic.energy, max.energy, canvas, rect, 3);
            }

            private void paintHeader(long from, long to, Canvas canvas, RectF rect, Statistic statistic) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xff000000);
                paint.setTextSize(textSize);

                String when = DateUtils.formatDateRange(getActivity(), from, to, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
                canvas.drawText(when, rect.left, rect.top + textSize, paint);

                String what;
                switch (highlight) {
                    case 0:
                        what = getString(R.string.duration_minutes, statistic.duration / 60);
                        break;
                    case 1:
                        what = getString(R.string.distance_meters, statistic.distance);
                        break;
                    case 2:
                        what = getString(R.string.strokes_count, statistic.strokes);
                        break;
                    case 3:
                        what = getString(R.string.energy_calories, statistic.energy);
                        break;
                    default:
                        throw new IndexOutOfBoundsException();
                }
                canvas.drawText(what, rect.right - paint.measureText(what), rect.top + textSize, paint);
            }

            private void paint(int value, int max, Canvas canvas, RectF rect, int index) {
                paint.setStyle(Paint.Style.FILL);
                if (index == highlight) {
                    paint.setColor(0x803567ed);
                } else {
                    paint.setColor(0x403567ed);
                }

                float height = (rect.bottom - rect.top);
                float width = (rect.right - rect.left);

                float left = rect.left;
                float right = rect.left + (width * value / max);
                float top = rect.top + (index * height / 5) + (index * height / 15);
                float bottom = rect.top + ((index + 1) * height / 5) + (index * height / 15);

                canvas.drawRect(left, top, right, bottom, paint);
            }
        });

        return root;
    }

    private Statistic getMax(long duration) {
        String key = "d" + duration;

        Statistic statistic = this.statistics.get(key);
        if (statistic == null) {
            statistic = new Statistic(0, 0);

            statistics.put(key, statistic);
        }

        return statistic;
    }

    private Statistic getStatistics(long from, long to) {
        String key = from + ":" + to;

        Statistic statistic = this.statistics.get(key);
        if (statistic == null) {
            statistic = new Statistic(from, to);

            statistics.put(key, statistic);
            pendings.add(statistic);
        }

        if (pendings.contains(statistic) && lookup == null) {
            lookup = new StatisticLookup(statistic);
            lookup.restartLoader(0, this);
        }

        return statistic;
    }

    @Override
    public void onClick(View v) {
        highlight = (highlight + 1) % 4;

        timelineView.invalidate();
    }

    private class Statistic {

        public long from;
        public long to;

        public int duration;
        public int distance;
        public int strokes;
        public int energy;

        public Statistic(long from, long to) {
            this.from = from;
            this.to = to;
        }
    }

    private class StatisticLookup extends MatchLookup<Workout> {

        private Statistic pending;

        public StatisticLookup(Statistic pending) {
            super(gym.getWorkouts(pending.from, pending.to));

            this.pending = pending;
        }

        @Override
        protected void onLookup(List<Workout> workouts) {
            Statistic max = getMax(pending.to - pending.from);

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

            timelineView.postInvalidate();

            // no longer pending
            pendings.remove(pending);

            lookup = null;

            // release cursor
            workouts.clear();
        }
    }
}