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
import android.widget.LinearLayout;

import svenmeier.coxswain.R;

/**
 */
public class ValueContainer extends LinearLayout {

    private static final int[] state = {0};

    public ValueContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ValueContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void clearState() {
        this.state[0] = 0;

        refreshDrawableState();

        invalidate();
    }

    public void setState(int state) {
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