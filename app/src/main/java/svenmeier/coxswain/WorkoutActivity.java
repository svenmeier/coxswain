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
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.view.BindingDialogFragment;
import svenmeier.coxswain.view.BindingView;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.SegmentsData;
import svenmeier.coxswain.view.SegmentsView;
import svenmeier.coxswain.view.Utils;
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

    private Preference<String> bindingPreference;

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
        Utils.collect(BindingView.class, getWindow().getDecorView(), bindingViews);

        List<ValueBinding> binding;
        if (gym.pace == null) {
            bindingPreference = Preference.getString(this, R.string.preference_workout_binding);
            binding = DEFAULT_BINDING;

            paceBoat = new SelfPaceBoat();
        } else {
            bindingPreference = Preference.getString(this, R.string.preference_workout_binding_pace);
            binding = DEFAULT_PACE_BINDING;

            paceBoat = new DebouncePaceBoat(new WorkoutPaceBoat());
        }

        List<String> previousBinding = bindingPreference.getList();
        for (int b = 0; b < Math.min(binding.size(), bindingViews.size()); b++) {
            ValueBinding temp = binding.get(b);
            try {
                temp = ValueBinding.valueOf(previousBinding.get(b));
            } catch (Exception ex) {
            }
            bindingViews.get(b).setBinding(temp);

            bindingViews.get(b).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    leanBack(false);

                    BindingDialogFragment fragment = BindingDialogFragment.create(view.getId(), ((BindingView)view).getBinding());

                    fragment.show(getFragmentManager(), "bindingPreference");

                    return true;
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        List<String> bindings = new ArrayList<>();
        for (BindingView valueView : bindingViews) {
            bindings.add(valueView.getBinding().name());
        }
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
    public void onBinding(int viewId, ValueBinding binding) {
        if (binding != null) {
            ((BindingView)findViewById(viewId)).setBinding(binding);
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
        public int getDistanceDelta(int currentDuration, int currentDistance) {
            if (snapshots.isEmpty() || currentDuration == 0) {
                return 0;
            }

            Snapshot snapshot = snapshots.get(Math.min(snapshots.size() - 1, currentDuration - 1));

            return (currentDistance - snapshot.distance.get());
        }

        @Override
        public int getDurationDelta(int currentDuration, int currentDistance) {
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

            return (currentDuration - this.duration);
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
        public int getDistanceDelta(int duration, int distance) {
            return 0;
        }

        @Override
        public int getDurationDelta(int duration, int distance) {
            return 0;
        }

        @Override
        public void dismiss() {
        }
    }

	/**
     * {@link }WorkoutPaceBoat}'s pace changes each second only, while the current
     * {@link Workout} is available continuously. The latter has to be transferred
     * to seconds unit too, to prevent unwanted bouncing of the deltas.
     */
    private class DebouncePaceBoat implements DismissablePaceBoat {

        private WorkoutPaceBoat pace;

        private int duration;

        private int durationDelta;

        private int distanceDelta;

        public DebouncePaceBoat(WorkoutPaceBoat pace) {
            this.pace = pace;
        }

        @Override
        public int getDurationDelta(int duration, int distance) {
            check(duration, distance);

            return durationDelta;
        }

        @Override
        public int getDistanceDelta(int duration, int distance) {
            check(duration, distance);

            return distanceDelta;
        }

        private void check(int duration, int distance) {
            if (this.duration != duration) {
                this.duration = duration;

                durationDelta = pace.getDurationDelta(duration, distance);
                distanceDelta = pace.getDistanceDelta(duration, distance);
            }
        }

        @Override
        public void dismiss() {
            pace.dismiss();
        }
    }
}
