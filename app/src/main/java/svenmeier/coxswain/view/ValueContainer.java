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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import svenmeier.coxswain.R;

/**
 */
public class ValueContainer extends FrameLayout {

    private static final int[] state = {0};

    private ValueView valueView;

    private LabelView labelView;

    public ValueContainer(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public ValueContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        inflate(getContext(), R.layout.layout_value, this);

        valueView = (ValueView)findViewById(R.id.value);
        labelView = (LabelView)findViewById(R.id.label);
    }

    public void labelPattern(int label, int pattern) {

        labelView.setText(getContext().getString(label));

        valueView.setPattern(getContext().getString(pattern));
    }

    public void value(int value) {
        clearState();

        valueView.setValue(value);
    }

    public void target(int memory, int segment, int achieved) {
        if (segment > 0) {
            setState(R.attr.field_target);

            valueView.setValue(Math.max(0, segment - achieved));
        } else {
            clearState();

            valueView.setValue(memory);
        }
    }

    public void limit(int memory, int segment) {
        if (segment > 0) {
            int difference = memory - segment;
            if (difference < 0) {
                setState(R.attr.field_low);
            } else {
                setState(R.attr.field_high);
            }

            valueView.setPattern(valueView.getPattern().replace('-', '+'));
            valueView.setValue(difference);
        } else {
            clearState();

            valueView.setPattern(valueView.getPattern().replace('+', '-'));
            valueView.setValue(memory);
        }
    }

    private void clearState() {
        this.state[0] = R.attr.field_normal;

        refreshDrawableState();
        invalidate();
    }

    private void setState(int state) {
        this.state[0] = state;

        refreshDrawableState();
        invalidate();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

        mergeDrawableStates(drawableState, state);

        return drawableState;
    }
}