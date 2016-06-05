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
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.rower.water.RatioCalculator;
import svenmeier.coxswain.view.BindingDialogFragment;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.SegmentsData;
import svenmeier.coxswain.view.SegmentsView;
import svenmeier.coxswain.view.Utils;
import svenmeier.coxswain.view.ValueBinding;
import svenmeier.coxswain.view.ValueContainer;


/**
 */
public class WorkoutActivity extends AbstractActivity implements View.OnSystemUiVisibilityChangeListener, Gym.Listener, BindingDialogFragment.Callback {

    private static final int DELAY_MILLIS = 250;

    private static final int LEAN_BACK =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    private Gym gym;

    private SegmentsView segmentsView;

    private List<ValueContainer> valueViews = new ArrayList<>();

    private LevelView levelView;

    private Preference<String> binding;

    private Runnable returnToLeanBack = new Runnable() {
        @Override
        public void run() {
            getWindow().getDecorView().setSystemUiVisibility(LEAN_BACK);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        binding = Preference.getString(this, R.string.preference_workout_binding);

        setContentView(R.layout.layout_workout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(LEAN_BACK);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        segmentsView = (SegmentsView) findViewById(R.id.workout_segments);
        segmentsView.setData(new SegmentsData(gym.program));

        Utils.collect(ValueContainer.class, getWindow().getDecorView(), valueViews);

        levelView = (LevelView) findViewById(R.id.workout_progress);
    }

    @Override
    public void onResume() {
        super.onResume();

        List<ValueBinding> bindings = new ArrayList<>();
        try {
            for (String name : TextUtils.split(binding.get(), ",")) {
                bindings.add(ValueBinding.valueOf(name));
            }
        } catch (Exception useDefault) {
            bindings = Arrays.asList(
                ValueBinding.DURATION,
                ValueBinding.DISTANCE,
                ValueBinding.STROKES,
                ValueBinding.SPEED,
                ValueBinding.PULSE,
                ValueBinding.STROKE_RATE);
        }
        for (int b = 0; b < Math.min(bindings.size(), valueViews.size()); b++) {
            valueViews.get(b).setBinding(bindings.get(b));
            valueViews.get(b).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    leanBack(false);

                    BindingDialogFragment fragment = BindingDialogFragment.create(view.getId());

                    fragment.show(getFragmentManager(), "binding");

                    return true;
                }
            });
        }

        changed();
        gym.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        List<String> bindings = new ArrayList<>();
        for (ValueContainer valueView : valueViews) {
            bindings.add(valueView.getBinding().name());
        }
        binding.set(TextUtils.join(",", bindings));

        gym.removeListener(this);
    }

    @Override
    public void changed() {
        if (gym.program == null) {
            finish();
            return;
        }

//        getWindow().getDecorView().setBackgroundColor(RatioCalculator.pulling ? Color.RED : Color.GREEN);

        updateValues();
        updateLevel();
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        leanBack(true);
    }

    private void updateValues() {
        for (int v = 0; v < valueViews.size(); v++) {
            valueViews.get(v).update(gym);
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

    @Override
    public void onBinding(int viewId, ValueBinding binding) {
        if (binding != null) {
            ((ValueContainer)findViewById(viewId)).setBinding(binding);
        }

        leanBack(true);
    }

    private void leanBack(boolean yes) {
        if (yes) {
            getWindow().getDecorView().postDelayed(returnToLeanBack, 3000);
        } else {
            getWindow().getDecorView().getHandler().removeCallbacks(returnToLeanBack);
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, WorkoutActivity.class);

        context.startActivity(intent);
    }
}
