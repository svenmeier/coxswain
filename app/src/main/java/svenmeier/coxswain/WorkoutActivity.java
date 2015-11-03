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
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.SegmentsData;
import svenmeier.coxswain.view.SegmentsView;
import svenmeier.coxswain.view.ValueContainer;
import svenmeier.coxswain.view.ValueView;


/**
 */
public class WorkoutActivity extends Activity implements View.OnSystemUiVisibilityChangeListener {

    private static final int DELAY_MILLIS = 250;

    private static final int LEAN_BACK =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    private Gym gym;

    private Phase pull = new Phase();

    private Phase release = new Phase();

    private SegmentsView segmentsView;

    private LevelView strokeView;

    private ValueView durationView;
    private ValueView distanceView;
    private ValueView strokesView;
    private ValueView speedView;
    private ValueView strokeRateView;
    private ValueView pulseView;

    private LevelView levelView;

    private Gym.Listener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setContentView(R.layout.layout_workout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(LEAN_BACK);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        segmentsView = (SegmentsView) findViewById(R.id.workout_segments);
        segmentsView.setData(new SegmentsData(gym.program));

        strokeView = (LevelView) findViewById(R.id.workout_stroke);

        durationView = (ValueView) findViewById(R.id.target_duration);
        distanceView = (ValueView) findViewById(R.id.target_distance);
        strokesView = (ValueView) findViewById(R.id.target_strokes);
        speedView = (ValueView) findViewById(R.id.target_speed);
        strokeRateView = (ValueView) findViewById(R.id.target_strokeRate);
        pulseView = (ValueView) findViewById(R.id.target_pulse);

        levelView = (LevelView) findViewById(R.id.workout_progress);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        super.onResume();

        listener = new Gym.Listener() {
            @Override
            public void changed() {
                if (gym.program == null) {
                    finish();
                    return;
                }

                updateStroke();
                updateValues();
                updateLevel();
            }
        };
        listener.changed();
        gym.addListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        gym.removeListener(listener);
        listener = null;
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().setSystemUiVisibility(LEAN_BACK);
            }
        }, 3000);
    }

    private void updateValues() {
        int achieved = 0;

        int duration = 0;
        int distance = 0;
        int strokes = 0;
        int speed = 0;
        int strokeRate = 0;
        int pulse = 0;

        Snapshot snapshot = gym.getLastSnapshot();
        if (snapshot == null) {
            snapshot = new Snapshot();
        }

        if (gym.current != null) {
            Segment segment = gym.current.segment;

            achieved = gym.current.achieved();

            duration = segment.duration.get();
            distance = segment.distance.get();
            strokes = segment.strokes.get();
            speed = segment.speed.get();
            strokeRate = segment.strokeRate.get();
            pulse = segment.pulse.get();
        }

        target(durationView, gym.workout.duration.get(), duration, achieved);
        target(distanceView, snapshot.distance, distance, achieved);
        target(strokesView, snapshot.strokes, strokes, achieved);
        limit(speedView, snapshot.speed, speed);
        limit(strokeRateView, snapshot.strokeRate, strokeRate);
        limit(pulseView, snapshot.pulse, pulse);
    }

    private void target(ValueView view, int memory, int segment, int achieved) {
        ValueContainer container = (ValueContainer)view.getParent();
        if (segment > 0) {
            container.setState(R.attr.field_target);

            view.setValue(Math.max(0, segment - achieved));
        } else {
            container.clearState();

            view.setValue(memory);
        }
    }

    private void limit(ValueView view, int memory, int segment) {
        ValueContainer container = (ValueContainer)view.getParent();
        if (segment > 0) {
            int difference = memory - segment;
            if (difference < 0) {
                container.setState(R.attr.field_low);
            } else {
                container.setState(R.attr.field_high);
            }

            view.setPattern(view.getPattern().replace('-', '+'));
            view.setValue(difference);
        } else {
            container.clearState();

            view.setPattern(view.getPattern().replace('+', '-'));
            view.setValue(memory);
        }
    }

    private void updateLevel() {
        float value = 0f;
        float total = 0f;
        Gym.Current current = gym.current;
        for (Segment segment : gym.program.segments.get()) {
            float segmentValue = segment.asDuration();

            if (current != null && current.segment == segment) {
                value = total + current.completion() * segmentValue;
            }

            total += segmentValue;
        }
        if (current == null) {
            value = total;
        }
        levelView.setLevel(Math.round(value * 10000 / total));
    }

    private void updateStroke() {
        Snapshot snapshot = gym.getLastSnapshot();
        if (snapshot == null) {
            snapshot = new Snapshot();
        }

        pull.update(snapshot.pull, true);
        release.update(snapshot.pull, false);

        int level = 0;
        if (release.duration > 0) {
            level = Math.round(pull.duration * 10000f / (pull.duration + release.duration));
        }

        strokeView.setLevel(level);
    }

    private class Phase {

        private long start;

        public long duration;

        private void update(boolean current, boolean expected) {
            long now = System.currentTimeMillis();

            if (current == expected) {
                if (start == 0) {
                    start = now;
                }
            } else {
                if (start > 0) {
                    duration = now - start;
                    start = 0;
                }
            }
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, WorkoutActivity.class);

        context.startActivity(intent);
    }
}
