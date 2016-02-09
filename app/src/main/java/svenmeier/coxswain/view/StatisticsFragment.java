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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;


public class StatisticsFragment extends Fragment {

    private Gym gym;

    private Statistic max = new Statistic();

    private Map<String, Statistic> statistics = new HashMap<>();

    private int highlight;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_statistics, container, false);

        TimelineView timelineView = (TimelineView) root.findViewById(R.id.timeline);

        timelineView.setPainter(new TimelineView.Painter() {

            private Paint paint = new Paint();

            @Override
            public void paint(long from, long to, Canvas canvas, RectF rect) {
                Statistic statistic = getStatistics(from, to);

                paint(statistic.distance, max.distance, canvas, rect, 0);
                paint(statistic.strokes, max.strokes, canvas, rect, 1);
                paint(statistic.energy, max.energy, canvas, rect, 2);
                paint(statistic.duration, max.duration, canvas, rect, 3);
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

    private Statistic getStatistics(long from, long to) {
        String key = from + ":" + to;

        Statistic statistic = this.statistics.get(key);
        if (statistic == null) {
            statistic = new Statistic();

            statistic.distance = (int)(6000 * (1 + Math.random()) / 2);
            statistic.strokes = (int)(700 * (1 + Math.random()) / 2);
            statistic.energy = (int)(300 * (1 + Math.random()) / 2);
            statistic.duration = (int)(25 * 60 * (1 + Math.random()) / 2);

            max.distance = Math.max(max.distance, statistic.distance);
            max.strokes = Math.max(max.strokes, statistic.strokes);
            max.energy = Math.max(max.energy, statistic.energy);
            max.duration = Math.max(max.duration, statistic.duration);

            statistics.put(key, statistic);
        }

        return statistic;
    }

    private class Statistic {
        public int duration;
        public int distance;
        public int strokes;
        public int energy;
    }
}
