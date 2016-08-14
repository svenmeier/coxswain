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
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.SegmentsData;
import svenmeier.coxswain.view.SegmentsView;
import svenmeier.coxswain.view.Utils;
import svenmeier.coxswain.view.ValueBinding;
import svenmeier.coxswain.view.BindingView;


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

    private PaceLookup paceLookup;

    private Preference<String> binding;

    private SegmentsView segmentsView;

    private LevelView levelView;

    private List<BindingView> bindingViews = new ArrayList<>();

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

        Utils.collect(BindingView.class, getWindow().getDecorView(), bindingViews);

        levelView = (LevelView) findViewById(R.id.workout_progress);

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
        for (int b = 0; b < Math.min(bindings.size(), bindingViews.size()); b++) {
            bindingViews.get(b).setBinding(bindings.get(b));
            bindingViews.get(b).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    leanBack(false);

                    BindingDialogFragment fragment = BindingDialogFragment.create(view.getId(), ((BindingView)view).getBinding());

                    fragment.show(getFragmentManager(), "binding");

                    return true;
                }
            });
        }

        if (gym.pace != null) {
            paceLookup = new PaceLookup();
            paceLookup.restartLoader(0, this);
        }
    }

    @Override
    protected void onDestroy() {
        List<String> bindings = new ArrayList<>();
        for (BindingView valueView : bindingViews) {
            bindings.add(valueView.getBinding().name());
        }
        binding.set(TextUtils.join(",", bindings));

        if (paceLookup != null) {
            paceLookup.destroy(0, this);
            paceLookup = null;
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
            bindingViews.get(v).changed(gym, paceLookup);
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
        levelView.setLevel(Math.round(value * 10000 / total));
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

    public static void start(Context context) {
        Intent intent = new Intent(context, WorkoutActivity.class);

        context.startActivity(intent);
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

        activity.startActivity(new Intent(activity, WorkoutActivity.class));
    }

    private class PaceLookup extends MatchLookup<Snapshot> implements BindingView.PaceBoat {

        private List<Snapshot> snapshots = new ArrayList<>();

        private int duration;

        protected PaceLookup() {
            super(gym.getSnapshots(gym.pace));
        }

        @Override
        public int getDistance(int duration) {
            if (snapshots.isEmpty()) {
                return -1;
            }

            int index = Math.min(snapshots.size() - 1, duration);
            return snapshots.get(index).distance.get();
        }

        @Override
        public int getDuration(int distance) {
            while (duration < snapshots.size()) {
                if (snapshots.get(duration).distance.get() >= distance) {
                    break;
                }

                duration++;
            }

            if (duration == snapshots.size()) {
                return -1;
            } else {
                return duration;
            }
        }

        @Override
        protected void onLookup(List<Snapshot> propoids) {
            this.snapshots = propoids;
        }
    }
}
