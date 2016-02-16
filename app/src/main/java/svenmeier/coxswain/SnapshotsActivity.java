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
import android.graphics.RectF;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.MenuItem;

import propoid.db.Reference;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.view.TimelineView;
import svenmeier.coxswain.view.Utils;


public class SnapshotsActivity extends Activity {

    private Gym gym;

    private Workout workout;

    private TimelineView timelineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

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
        timelineView.setPeriods(new SnapshotPeriods());
        timelineView.setWindow(TimelineView.MINUTE * 10);
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

    private class SnapshotPeriods implements TimelineView.Periods {

        private Paint paint = new Paint();

        private float textSize = Utils.dpToPx(SnapshotsActivity.this, 20);

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
            return new TimelineView.MinuteUnit(time);
        }

        @Override
        public void paint(Class<?> unit, long from, long to, Canvas canvas, RectF rect) {
            paintHeader(from, to, canvas, rect);
        }

        private void paintHeader(long from, long to, Canvas canvas, RectF rect) {
            String when = DateUtils.formatDateTime(SnapshotsActivity.this, from, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_UTC);

            paint.setColor(0xff000000);
            paint.setTextSize(textSize);
            canvas.drawText(when, rect.left, rect.top + textSize, paint);
        }
    }

    public static Intent createIntent(Context context, Workout workout) {
        Intent intent = new Intent(context, SnapshotsActivity.class);

        intent.setData(new Reference<Workout>(workout).toUri());

        return intent;
    }
}