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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayout;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import propoid.ui.list.MatchLookup;
import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.view.BindingDialogFragment;
import svenmeier.coxswain.view.BindingView;
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

	private BindingView.PaceBoat paceBoat;

	private Preference<ValueBinding> bindingPreference;

	private GridLayout gridView;

	private SegmentsView segmentsView;

	private LevelView progressView;

	private Runnable returnToLeanBack = new Runnable() {
		@Override
		public void run() {
			getWindow().getDecorView().setSystemUiVisibility(LEAN_BACK);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		gym = Gym.instance(this);

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
		gridView = (GridLayout) findViewById(R.id.workout_grid);

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
			writeToGrid(bindingPreference.getList());
		} catch (Exception ex) {
			writeToGrid(defaultBinding);
		}
	}

	private void writeToGrid(List<ValueBinding> binding) {
		if (binding == null || binding.isEmpty()) {
			throw new IllegalArgumentException("binding must not be empty");
		}

		int columns;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			columns = binding.size() <= 3 ? 1 : 2;
		} else {
			columns = binding.size() <= 6 ? 1 : 2;
		}
		gridView.removeAllViews();
		gridView.setColumnCount(columns);

		for (int b = 0; b < binding.size(); b++) {
			ValueBinding temp = binding.get(b);

			final BindingView bindingView = (BindingView) getLayoutInflater().inflate(R.layout.layout_binding, gridView, false);
			gridView.addView(bindingView);

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
		}

		gridView.requestLayout();
	}

	@NonNull
	private List<ValueBinding> readFromGrid() {
		List<ValueBinding> bindings = new ArrayList<>();
		for (int v = 0; v < gridView.getChildCount(); v++) {
			bindings.add(((BindingView) gridView.getChildAt(v)).getBinding());
		}
		return bindings;
	}

	@Override
	protected void onDestroy() {
		List<ValueBinding> bindings = readFromGrid();
		bindingPreference.setList(bindings);

		paceBoat = null;

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
		int count = gridView.getChildCount();
		for (int v = 0; v < count; v++) {
			((BindingView) gridView.getChildAt(v)).changed(gym, paceBoat);
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
			((BindingView) gridView.getChildAt(index)).setBinding(binding);
		}

		leanBack(true);
	}

	@Override
	public void onIncrease(int index) {
		List<ValueBinding> list = readFromGrid();

		list.add(index, ValueBinding.NONE);

		writeToGrid(list);

		leanBack(true);
	}

	@Override
	public void onDecrease(int index) {
		List<ValueBinding> list = readFromGrid();

		list.remove(index);
		if (list.isEmpty()) {
			list.add(ValueBinding.NONE);
		}

		writeToGrid(list);

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

	/**
	 * Use a previous {@link svenmeier.coxswain.gym.Workout} as a pace boat.
	 */
	private class WorkoutPaceBoat extends MatchLookup<Snapshot> implements BindingView.PaceBoat {

		private List<Snapshot> snapshots = new ArrayList<>();

		private int duration;

		protected WorkoutPaceBoat() {
			super(gym.getSnapshots(gym.pace));

			restartLoader(0, WorkoutActivity.this);
		}

		@Override
		public int getDistanceDelta(Measurement measurement) {
			if (snapshots.isEmpty()) {
				return 0;
			}

			int from = distanceAt(measurement.duration);
			int to = distanceAt(measurement.duration + 1);

			int paceDistance = from + ((to - from) * (measurement.distance % 1000) / 1000);

			return measurement.distance - paceDistance;
		}

		private int distanceAt(int duration) {
			// increment by one, because first snapshot is written after one second only
			int index = duration - 1;
			if (index < 0) {
				return 0;
			}

			if (index >= snapshots.size()) {
				index = snapshots.size() - 1;
			}
			return snapshots.get(index).distance.get();
		}

		@Override
		public int getDurationDelta(Measurement measurement) {
			while (this.duration < snapshots.size()) {
				if (snapshots.get(this.duration).distance.get() >= measurement.distance) {
					break;
				}

				this.duration++;
			}

			if (this.duration >= snapshots.size() && this.duration > 0) {
				Snapshot snapshot = snapshots.get(snapshots.size() - 1);

				int distance = snapshot.distance.get();
				if (distance > 0) {
					// estimate duration
					this.duration = snapshots.size() * measurement.distance / distance;
				}
			}

			return measurement.duration - this.duration;
		}

		@Override
		protected void onLookup(List<Snapshot> propoids) {
			this.snapshots = propoids;

			// no updates needed
			destroy(0, WorkoutActivity.this);
		}
	}

	/**
	 * Use self as pace boat.
	 */
	private class SelfPaceBoat implements BindingView.PaceBoat {

		@Override
		public int getDistanceDelta(Measurement measurement) {
			return 0;
		}

		@Override
		public int getDurationDelta(Measurement measurement) {
			return 0;
		}
	}
}