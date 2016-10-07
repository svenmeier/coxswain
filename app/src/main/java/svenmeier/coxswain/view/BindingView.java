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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.Calendar;

import propoid.util.content.Preference;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;

/**
 */
public class BindingView extends FrameLayout {

    private int state = R.attr.value_normal;

    private ValueBinding binding;

    private ValueView valueView;

    private LabelView labelView;

    private Runnable timer;

    private int splitDistance;

    public BindingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public BindingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        inflate(getContext(), R.layout.layout_value, this);

        valueView = (ValueView)findViewById(R.id.value);
        labelView = (LabelView)findViewById(R.id.label);

        setBinding(ValueBinding.DISTANCE);

        splitDistance = Preference.getInt(getContext(), R.string.preference_split_distance).fallback(500).get();
    }

    public void setBinding(ValueBinding binding) {
        if (this.binding == binding) {
            return;
        }
        this.binding = binding;

        labelView.setText(getContext().getString(binding.label));
        valueView.setPattern(getContext().getString(binding.pattern));

        changed(0);

        initBinding();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        initBinding();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.timer = null;

        super.onDetachedFromWindow();
    }

    private void initBinding() {
        switch (binding) {
            case TIME:
                timer = new Runnable() {
                    @Override
                    public void run() {
                        if (timer == this && binding == ValueBinding.TIME) {
                            Calendar calendar = Calendar.getInstance();
                            int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
                            limit(minutes, 0);

                            postDelayed(timer, 1000);
                        }
                    }
                };
                timer.run();
                break;
        }
    }

    public ValueBinding getBinding() {
        return binding;
    }

    public void changed(int value) {
        setState(R.attr.value_normal);
        valueView.setValue(value);
    }

    public void changed(Gym gym, PaceBoat paceBoat) {
        int achieved = 0;

        int targetDuration = 0;
        int targetDistance = 0;
        int targetStrokes = 0;
        int targetEnergy = 0;
        int limitSpeed = 0;
        int limitStrokeRate = 0;
        int limitPulse = 0;

        if (gym.progress != null) {
            achieved = gym.progress.achieved();

            Segment segment = gym.progress.segment;

            targetDuration = segment.duration.get();
            targetDistance = segment.distance.get();
            targetStrokes = segment.strokes.get();
            targetEnergy = segment.energy.get();
            limitSpeed = segment.speed.get();
            limitStrokeRate = segment.strokeRate.get();
            limitPulse = segment.pulse.get();
        }

        Snapshot snapshot = gym.snapshot;
        if (snapshot == null) {
            snapshot = new Snapshot();
        }

        long elapsed = 0;
        if (gym.current != null) {
            elapsed = System.currentTimeMillis() - gym.current.start.get();
        }

        switch (binding) {
            case DURATION:
                target((int)(elapsed / 1000), targetDuration, achieved);
                break;
            case DISTANCE:
                target(snapshot.distance.get(), targetDistance, achieved);
                break;
            case STROKES:
                target(snapshot.strokes.get(), targetStrokes, achieved);
                break;
            case ENERGY:
                target(snapshot.energy.get(), targetEnergy, achieved);
                break;
            case SPEED:
                limit(snapshot.speed.get(), limitSpeed);
                break;
            case PULSE:
                limit(snapshot.pulse.get(), limitPulse);
                break;
            case STROKE_RATE:
                limit(snapshot.strokeRate.get(), limitStrokeRate);
                break;
            case STROKE_RATIO:
                limit(snapshot.strokeRatio.get(), 0);
                break;
            case SPLIT:
                split(100f / snapshot.speed.get());
                break;
            case AVERAGE_SPLIT:
                split(elapsed / 1000f / snapshot.distance.get());
                break;
            case DELTA_DISTANCE:
                delta(paceBoat.getDistanceDelta(elapsed, snapshot.distance.get()), false);
                break;
            case DELTA_DURATION:
                delta(paceBoat.getDurationDelta(elapsed, snapshot.distance.get()), true);
                break;
        }
    }

    private void split(float inverseSpeed) {
        setState(R.attr.value_normal);

        float duration = splitDistance * inverseSpeed;

        valueView.setValue((duration == Float.NaN || duration == Float.POSITIVE_INFINITY) ? 0 : (int)(duration));
    }

    private void delta(int delta, boolean positiveIsLow) {
        if (delta == 0) {
            setState(R.attr.value_normal);
        } else if ((delta < 0) ^ positiveIsLow) {
            setState(R.attr.value_low);
        } else {
            setState(R.attr.value_high);
        }
        valueView.setValue(delta);
    }

    private void target(int value, int target, int achieved) {
        if (target > 0) {
            setState(R.attr.value_target);

            valueView.setValue(Math.max(0, target - achieved));
        } else {
            setState(R.attr.value_normal);

            valueView.setValue(value);
        }
    }

    private void limit(int value, int limit) {
        if (limit > 0) {
            int difference = value - limit;
            if (difference < 0) {
                setState(R.attr.value_low);
            } else {
                setState(R.attr.value_high);
            }

            valueView.setPattern(valueView.getPattern().replace('-', '+'));
            valueView.setValue(difference);
        } else {
            setState(R.attr.value_normal);

            valueView.setPattern(valueView.getPattern().replace('+', '-'));
            valueView.setValue(value);
        }
    }

    private void setState(int state) {
        if (this.state == state) {
            return;
        }
        this.state = state;

        refreshDrawableState();
        invalidate();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

        mergeDrawableStates(drawableState, new int[]{state});

        return drawableState;
    }

    public interface PaceBoat {

		/**
         * @param elapsed elapsed milliseconds since start of workout
         * @param distance current distance of workout
         * @return delta to pace duration
         */
        int getDurationDelta(long elapsed, int distance);

        /**
         * @param elapsed elapsed milliseconds since start of workout
         * @param distance current distance of workout
         * @return delta to pace distance
         */
        int getDistanceDelta(long elapsed, int distance);
    }
}