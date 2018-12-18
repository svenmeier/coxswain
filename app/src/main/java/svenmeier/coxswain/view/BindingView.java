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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

import propoid.util.content.Preference;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Segment;

/**
 */
public class BindingView extends LinearLayout {

    private int state = R.attr.binding_normal;

    private ValueBinding binding;

    private TextView valueView;

    private TextView labelView;

    private Runnable timer;

    private int splitDistance;

    private boolean arabic;

    private int oldValue = Integer.MAX_VALUE;

    public BindingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public BindingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        splitDistance = Preference.getInt(getContext(), R.string.preference_split_distance).fallback(500).get();

        arabic = Preference.getBoolean(getContext(), R.string.preference_numbers_arabic).get();
    }

    public void setBinding(ValueBinding binding) {
        if (this.binding == binding) {
            return;
        }
        this.binding = binding;

        if (labelView == null) {
            labelView = (TextView)findViewById(R.id.label);
        }
        labelView.setText(getContext().getString(binding.label));

        if (valueView == null) {
            valueView = (TextView)findViewById(R.id.value);
        }

        changed(0);

        checkTimer();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        checkTimer();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.timer = null;

        super.onDetachedFromWindow();
    }

    private void checkTimer() {
        if (binding == ValueBinding.TIME) {
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
        } else {
            timer = null;
        }
    }

    public ValueBinding getBinding() {
        return binding;
    }

    public void changed(int value) {
        changeState(R.attr.binding_normal);

        changeValue(value, false);
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

        Measurement measurement = gym.measurement;

        switch (binding) {
            case DURATION:
                target(measurement.duration, targetDuration, achieved);
                break;
            case DISTANCE:
                target(measurement.distance, targetDistance, achieved);
                break;
            case STROKES:
                target(measurement.strokes, targetStrokes, achieved);
                break;
            case ENERGY:
                target(measurement.energy, targetEnergy, achieved);
                break;
            case SPEED:
                limit(measurement.speed, limitSpeed);
                break;
            case PULSE:
                limit(measurement.pulse, limitPulse);
                break;
            case STROKE_RATE:
                limit(measurement.strokeRate, limitStrokeRate);
                break;
            case STROKE_RATIO:
                limit(measurement.strokeRatio, 0);
                break;
            case SPLIT:
                split(100f / measurement.speed);
                break;
            case AVERAGE_SPLIT:
                split(measurement.duration * 1f / measurement.distance);
                break;
            case DELTA_DISTANCE:
                delta(paceBoat.getDistanceDelta(measurement), false);
                break;
            case DELTA_DURATION:
                delta(paceBoat.getDurationDelta(measurement), true);
                break;
        }
    }

    private void split(float inverseSpeed) {
        changeState(R.attr.binding_normal);

        float duration = splitDistance * inverseSpeed;

        changeValue((duration == Float.NaN || duration == Float.POSITIVE_INFINITY) ? 0 : (int)(duration), false);
    }

    private void delta(int delta, boolean positiveIsLow) {
        if (delta == 0) {
            changeState(R.attr.binding_normal);
        } else if ((delta < 0) ^ positiveIsLow) {
            changeState(R.attr.binding_limit_low);
        } else {
            changeState(R.attr.binding_limit_high);
        }
        changeValue(delta, false);
    }

    private void target(int value, int target, int achieved) {
        if (target > 0) {
            changeState(R.attr.binding_target);

            changeValue(Math.max(0, target - achieved), false);
        } else {
            changeState(R.attr.binding_normal);

            changeValue(value, false);
        }
    }

    private void limit(int value, int limit) {
        if (limit > 0) {
            int difference = value - limit;
            if (difference < 0) {
                changeState(R.attr.binding_limit_low);
            } else {
                changeState(R.attr.binding_limit_high);
            }

            changeValue(difference, true);
        } else {
            changeState(R.attr.binding_normal);

            changeValue(value, false);
        }
    }

    private void changeValue(int value, boolean signed) {
        if (value == oldValue) {
            // prevent unnecessary changes, these seem to freeze Samsung Galaxy S8
            return;
        }

        this.oldValue = value;

        valueView.setText(binding.format(getContext(), value, arabic, signed));
    }

    private void changeState(int state) {
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
         * @return delta to pace duration
         */
        int getDurationDelta(Measurement measurement);

        /**
         * @return delta to pace distance
         */
        int getDistanceDelta(Measurement measurement);
    }
}