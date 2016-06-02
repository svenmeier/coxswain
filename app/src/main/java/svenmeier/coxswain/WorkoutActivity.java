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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.SegmentsData;
import svenmeier.coxswain.view.SegmentsView;
import svenmeier.coxswain.view.ValueContainer;


/**
 */
public class WorkoutActivity extends AbstractActivity implements View.OnSystemUiVisibilityChangeListener, Gym.Listener {

    private static final int DELAY_MILLIS = 250;

    private static final int LEAN_BACK =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    private Gym gym;

    private SegmentsView segmentsView;

    private ValueContainer durationView;
    private ValueContainer distanceView;
    private ValueContainer strokesView;
    private ValueContainer speedView;
    private ValueContainer strokeRateView;
    private ValueContainer pulseView;

    private LevelView levelView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setContentView(R.layout.layout_workout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(LEAN_BACK);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        segmentsView = (SegmentsView) findViewById(R.id.workout_segments);
        segmentsView.setData(new SegmentsData(gym.program));

        durationView = (ValueContainer) findViewById(R.id.workout_value_0);
        durationView.labelPattern(R.string.target_duration, R.string.duration_pattern);
        durationView.value(0);

        distanceView = (ValueContainer) findViewById(R.id.workout_value_1);
        distanceView.labelPattern(R.string.target_distance, R.string.distance_pattern);
        distanceView.value(0);

        strokesView = (ValueContainer) findViewById(R.id.workout_value_2);
        strokesView.labelPattern(R.string.target_strokes, R.string.strokes_pattern);
        strokesView.value(0);

        speedView = (ValueContainer) findViewById(R.id.workout_value_3);
        speedView.labelPattern(R.string.limit_speed, R.string.speed_pattern);
        speedView.value(0);

        strokeRateView = (ValueContainer) findViewById(R.id.workout_value_4);
        strokeRateView.labelPattern(R.string.limit_strokeRate, R.string.strokeRate_pattern);
        strokeRateView.value(0);

        pulseView = (ValueContainer) findViewById(R.id.workout_value_5);
        pulseView.labelPattern(R.string.limit_pulse, R.string.pulse_pattern);
        pulseView.value(0);

        levelView = (LevelView) findViewById(R.id.workout_progress);
    }

    @Override
    public void onResume() {
        super.onResume();

        changed();
        gym.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        gym.removeListener(this);
    }

    @Override
    public void changed() {
        if (gym.program == null) {
            finish();
            return;
        }

        updateValues();
        updateLevel();
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

        int targetDuration = 0;
        int targetDistance = 0;
        int targetStrokes = 0;
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

            targetDuration = segment.duration.get();
            targetDistance = segment.distance.get();
            targetStrokes = segment.strokes.get();
            speed = segment.speed.get();
            strokeRate = segment.strokeRate.get();
            pulse = segment.pulse.get();
        }

        int duration = 0;
        if (gym.workout != null) {
            duration = gym.workout.duration.get();
        }

        durationView.target(duration, targetDuration, achieved);
        distanceView.target(snapshot.distance.get(), targetDistance, achieved);
        strokesView.target(snapshot.strokes.get(), targetStrokes, achieved);
        speedView.limit(snapshot.speed.get(), speed);
        strokeRateView.limit(snapshot.strokeRate.get(), strokeRate);
        pulseView.limit(snapshot.pulse.get(), pulse);
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

    public static void start(Context context) {
        Intent intent = new Intent(context, WorkoutActivity.class);

        context.startActivity(intent);
    }
}
