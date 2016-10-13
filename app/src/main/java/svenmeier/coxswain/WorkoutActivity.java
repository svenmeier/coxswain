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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import propoid.ui.list.MatchLookup;
import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.view.BindingDialogFragment;
import svenmeier.coxswain.view.BindingView;
import svenmeier.coxswain.view.DashLayout;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.SegmentsData;
import svenmeier.coxswain.view.SegmentsView;
import svenmeier.coxswain.view.ValueBinding;


/**
 */
public class WorkoutActivity extends AbstractActivity implements View.OnSystemUiVisibilityChangeListener, Gym.Listener, BindingDialogFragment.Callback {

    private static final List<ValueBinding> DEFAULT_BINDING = Arrays.asList(
            ValueBinding.DURATION,
            ValueBinding.DISTANCE,
            ValueBinding.STROKES,
            ValueBinding.SPEED,
            ValueBinding.PULSE,
            ValueBinding.STROKE_RATE);

    private static final List<ValueBinding> DEFAULT_PACE_BINDING = Arrays.asList(
            ValueBinding.DURATION,
            ValueBinding.DISTANCE,
            ValueBinding.STROKES,
            ValueBinding.SPEED,
            ValueBinding.DELTA_DISTANCE,
            ValueBinding.STROKE_RATE);

    private static final int LEAN_BACK =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    private Gym gym;

    private DismissablePaceBoat paceBoat;

    private Preference<ValueBinding> bindingPreference;

    private SegmentsView segmentsView;

    private LevelView progressView;

    private List<BindingView> bindingViews = new ArrayList<>();

    private Runnable returnToLeanBack = new Runnable() {
        @Override
        public void run() {
            getWindow().getDecorView().setSystemUiVisibility(LEAN_BACK);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        gym = Gym.instance(this);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(LEAN_BACK);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        setContentView(R.layout.layout_workout);
        segmentsView = (SegmentsView) findViewById(R.id.workout_segments);
        segmentsView.setData(new SegmentsData(gym.program));
        progressView = (LevelView) findViewById(R.id.workout_progress);

        List<ValueBinding> defaultBinding;
        if (gym.pace == null) {
            defaultBinding = DEFAULT_BINDING;
            bindingPreference = Preference.getEnum(this, ValueBinding.class, R.string.preference_workout_binding);

            paceBoat = new SelfPaceBoat();
        } else {
            defaultBinding = DEFAULT_PACE_BINDING;
            bindingPreference = Preference.getEnum(this, ValueBinding.class, R.string.preference_workout_binding_pace);

            paceBoat = new WorkoutPaceBoat();
        }

        try {
            fillDash(bindingPreference.getList());
        } catch (Exception ex) {
            fillDash(defaultBinding);
        }
    }

    private void fillDash(List<ValueBinding> binding) {
        if (binding == null || binding.isEmpty()) {
            throw new IllegalArgumentException("binding must not be empty");
        }

        DashLayout dashView = (DashLayout)findViewById(R.id.workout_dash);
        dashView.removeAllViews();
        bindingViews.clear();
        for (int b = 0; b < binding.size(); b++) {
            ValueBinding temp = binding.get(b);

            final BindingView bindingView = new BindingView(this, null, R.style.BindingView);
            bindingView.setBinding(temp);

            final int index = b;
            bindingView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    leanBack(false);

                    BindingDialogFragment fragment = BindingDialogFragment.create(index, bindingView.getBinding());

                    fragment.show(getFragmentManager(), "bindingPreference");

                    return true;
                }
            });

            dashView.addView(bindingView);
            bindingViews.add(bindingView);
        }

        dashView.requestLayout();
    }

    @NonNull
    private List<ValueBinding> getValueBindings() {
        List<ValueBinding> bindings = new ArrayList<>();
        for (BindingView valueView : bindingViews) {
            bindings.add(valueView.getBinding());
        }
        return bindings;
    }

    @Override
    protected void onDestroy() {
        List<ValueBinding> bindings = getValueBindings();
        bindingPreference.setList(bindings);

        if (paceBoat != null) {
            paceBoat.dismiss();
            paceBoat = null;
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        changed();
        gym.addListener(this);
    }

    @Override
    protected void onPause() {
        gym.removeListener(this);

        super.onPause();
    }

    @Override
    public void changed() {
        if (gym.program == null) {
            finish();
            return;
        }

        updateBindings();
        updateLevel();
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        leanBack(true);
    }

    private void updateBindings() {
        for (int v = 0; v < bindingViews.size(); v++) {
            bindingViews.get(v).changed(gym, paceBoat);
        }
    }

    private void updateLevel() {
        float value = 0f;
        float total = 0f;
        Gym.Progress progress = gym.progress;
        for (Segment segment : gym.program.segments.get()) {
            float segmentValue = segment.asDuration();

            if (progress != null && progress.segment == segment) {
                value = total + progress.completion() * segmentValue;
            }

            total += segmentValue;
        }
        if (progress == null) {
            value = total;
        }
        progressView.setLevel(Math.round(value * 10000 / total));
    }

    @Override
    public void onBinding(int index, ValueBinding binding) {
        if (binding != null) {
            bindingViews.get(index).setBinding(binding);
        }

        leanBack(true);
    }

    @Override
    public void addBinding(int index) {
        List<ValueBinding> bindings = getValueBindings();

        bindings.add(bindings.get(index));

        fillDash(bindings);
    }

    @Override
    public void removeBinding(int index) {
        List<ValueBinding> bindings = getValueBindings();

        bindings.remove(index);

        fillDash(bindings);
    }

    private void leanBack(boolean yes) {
        if (yes) {
            getWindow().getDecorView().postDelayed(returnToLeanBack, 3000);
        } else {
            getWindow().getDecorView().getHandler().removeCallbacks(returnToLeanBack);
        }
    }

    public static void start(Activity activity) {

        Preference<Boolean> intentPreference = Preference.getBoolean(activity, R.string.preference_integration_intent);
        if (intentPreference.get()) {
            Preference<String> intentUriPreference = Preference.getString(activity, R.string.preference_integration_intent_uri);
            String uri = intentUriPreference.get();

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                activity.startActivity(intent);
                return;
            } catch (Exception ex) {
            }
        }

        restart(activity);
    }

    public static void restart(Activity activity) {
        activity.startActivity(new Intent(activity, WorkoutActivity.class));
    }

    private interface DismissablePaceBoat extends BindingView.PaceBoat {
        void dismiss();
    }

	/**
     * Use a previous {@link svenmeier.coxswain.gym.Workout} as a pace boat.
     */
    private class WorkoutPaceBoat extends MatchLookup<Snapshot> implements DismissablePaceBoat {

        private List<Snapshot> snapshots = new ArrayList<>();

        private int duration;

        protected WorkoutPaceBoat() {
            super(gym.getSnapshots(gym.pace));

            restartLoader(0, WorkoutActivity.this);
        }

        @Override
        public int getDistanceDelta(long elapsed, int currentDistance) {
            if (snapshots.isEmpty()) {
                return 0;
            }

            int from = distanceAt(elapsed);
            int to = distanceAt(elapsed + 1000);

            int paceDistance = from + ((to - from) * (currentDistance % 1000) / 1000);

            return currentDistance - paceDistance;
        }

        private int distanceAt(long elapsed) {
            // increment by one, because first snapshot is written after one second only
            int index = (int)(elapsed / 1000) - 1;
            if (index < 0) {
                return 0;
            }

            if (index >= snapshots.size()) {
                index = snapshots.size() - 1;
            }
            return snapshots.get(index).distance.get();
        }

        @Override
        public int getDurationDelta(long elapsed, int currentDistance) {
            while (this.duration < snapshots.size()) {
                if (snapshots.get(this.duration).distance.get() >= currentDistance) {
                    break;
                }

                this.duration++;
            }

            if (this.duration >= snapshots.size() && this.duration > 0) {
                Snapshot snapshot = snapshots.get(snapshots.size() - 1);

                int distance = snapshot.distance.get();
                if (distance > 0) {
                    // estimate duration
                    this.duration = snapshots.size() * currentDistance / distance;
                }
            }

            return (int)(elapsed / 1000) - this.duration;
        }

        @Override
        protected void onLookup(List<Snapshot> propoids) {
            this.snapshots = propoids;

            // no updates needed
            dismiss();
        }

        @Override
        public void dismiss() {
            destroy(0, WorkoutActivity.this);
        }
    }

	/**
     * Use self as pace boat.
     */
    private class SelfPaceBoat implements DismissablePaceBoat {

        @Override
        public int getDistanceDelta(long elapsed, int distance) {
            return 0;
        }

        @Override
        public int getDurationDelta(long elapsed, int distance) {
            return 0;
        }

        @Override
        public void dismiss() {
        }
    }
}