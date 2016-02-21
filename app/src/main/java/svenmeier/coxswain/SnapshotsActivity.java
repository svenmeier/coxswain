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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import propoid.db.Order;
import propoid.db.Reference;
import propoid.ui.list.MatchLookup;
import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.view.TimelineView;
import svenmeier.coxswain.view.Utils;


public class SnapshotsActivity extends Activity implements View.OnClickListener {

    private Gym gym;

    private Workout workout;

    private Preference<Long> windowPreference;

    private int highlight;

    private List<Snapshot> snapshots;

    private Snapshot max;

    private TimelineView timelineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        windowPreference = Preference.getLong(this, R.string.preference_snapshots_window).fallback(2 * TimelineView.MINUTE);

        Reference<Workout> reference = Reference.from(getIntent());
        workout = gym.getWorkout(reference);
        if (workout == null) {
            finish();
        } else {
            setTitle(DateUtils.formatDateTime(this, workout.start.get(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.layout_snapshots);

        timelineView = (TimelineView) findViewById(R.id.snapshots_timeline);
        timelineView.setOnClickListener(this);
        timelineView.setPeriods(new SnapshotPeriods());

        new SnapshotLookup().restartLoader(0, this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        timelineView.setWindow(windowPreference.get());
    }

    @Override
    protected void onStop() {
        windowPreference.set(timelineView.getWindow());

        super.onStop();
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

    @Override
    public void onClick(View v) {
        highlight = (highlight + 1) % 3;

        timelineView.invalidate();
    }


    private class SnapshotPeriods implements TimelineView.Periods {

        private SimpleDateFormat format;

        private Paint paint = new Paint();

        private int border = Utils.dpToPx(SnapshotsActivity.this, 4);

        private float textSize = Utils.dpToPx(SnapshotsActivity.this, 20);

        private float strokeWidth = Utils.dpToPx(SnapshotsActivity.this, 3);

        private Path path = new Path();

        public SnapshotPeriods() {
            format = new SimpleDateFormat("H:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public long min() {
            return 0;
        }

        @Override
        public long max() {
            return workout.duration.get() * 1000;
        }

        @Override
        public long minWindow() {
            return TimelineView.MINUTE;
        }

        @Override
        public long maxWindow() {
            return TimelineView.HOUR;
        }


        @Override
        public TimelineView.Unit unit(long time, long window) {
            if (window > 10 * TimelineView.MINUTE) {
                return new TimelineView.MinuteUnit(time, 5);
            } else if (window > 2 * TimelineView.MINUTE) {
                return new TimelineView.MinuteUnit(time);
            } else {
                return new TimelineView.SecondUnit(time, 10);
            }
        }

        @Override
        public void paint(Class<?> unit, long from, long to, Canvas canvas, RectF rect) {

            int start = (int)(from / 1000);
            int end = (int)(to / 1000);

            paintCurve(canvas, rect, start, end);

            paintHeader(to, canvas, rect);
        }

        private void paintHeader(long to, Canvas canvas, RectF rect) {
            int index = (int)(to / 1000);

            if (index >= 0 && index < snapshots.size()) {
                Snapshot snapshot = snapshots.get(index);

                String what;
                switch (highlight) {
                    case 0:
                        what = getString(R.string.speed_metersPerSecond, snapshot.speed.get());
                        break;
                    case 1:
                        what = getString(R.string.pulse_beatsPerMinute, snapshot.pulse.get());
                        break;
                    case 2:
                        what = getString(R.string.strokeRate_strokesPerMinute, snapshot.strokeRate.get());
                        break;
                    default:
                        throw new IndexOutOfBoundsException();
                }
                float whatWidth = paint.measureText(what);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xff3567ed);
                paint.setTextSize(textSize);
                canvas.drawText(what, rect.right - border - whatWidth, rect.top + border + textSize, paint);
            }

            String when = format.format(to);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff000000);
            paint.setTextSize(textSize);
            canvas.drawText(when, rect.left + border, rect.top + border + textSize, paint);
        }

        private void paintCurve(Canvas canvas, RectF rect, int start, int end) {
            path.reset();
            for (int current = start; current <= end; current++) {
                if (current < 0 || current >= snapshots.size()) {
                    continue;
                }

                Snapshot snapshot = snapshots.get(current);
                paintLine(path, rect, snapshot.speed.get(), max.speed.get(), current - start, end - start);
            }
            paint.setStyle(Paint.Style.STROKE);
            if (0 == highlight) {
                paint.setColor(0x803567ed);
            } else {
                paint.setColor(0x403567ed);
            }
            paint.setStrokeWidth(strokeWidth);
            canvas.drawPath(path, paint);
        }

        private void paintLine(Path path, RectF rect, int value, int max, int index, int count) {

            float left = rect.left + border;
            float width = rect.width() - border - border;
            float x = left + border + (width * value / max);
            float y = rect.bottom - (rect.height() * index / count);

            if (path.isEmpty()) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
    }

    private class SnapshotLookup extends MatchLookup<Snapshot> {

        public SnapshotLookup() {
            super(gym.getSnapshots(workout));

            setOrder(Order.ascendingByInsert());
        }

        @Override
        protected void onLookup(List<Snapshot> lookup) {
            snapshots = new ArrayList<>(lookup);

            if (snapshots.isEmpty()) {
                // TODO
                for (int i = 0; i < 27 * 60; i++) {
                    Snapshot object = new Snapshot();
                    object.speed.set((int)(Math.random() * 500));
                    object.strokeRate.set((int)(Math.random() * 500));
                    object.pulse.set((int)(Math.random() * 500));
                    snapshots.add(object);
                }
            }

            max = new Snapshot();
            for (Snapshot snapshot : snapshots) {
                max.speed.set(Math.max(max.speed.get(), snapshot.speed.get()));
                max.strokeRate.set(Math.max(max.strokeRate.get(), snapshot.strokeRate.get()));
                max.pulse.set(Math.max(max.pulse.get(), snapshot.pulse.get()));
            }

            timelineView.postInvalidate();
        }
    }

    public static Intent createIntent(Context context, Workout workout) {
        Intent intent = new Intent(context, SnapshotsActivity.class);

        intent.setData(new Reference<Workout>(workout).toUri());

        return intent;
    }
}