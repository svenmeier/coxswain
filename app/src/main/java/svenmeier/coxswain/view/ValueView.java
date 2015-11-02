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

import svenmeier.coxswain.R;

/**
 */
public class ValueView extends LabelView {

    private static final int CODEPOINT_0 = 48;

    private String pattern = "";

    private int value = Integer.MAX_VALUE;

    public ValueView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public ValueView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ValueView,
                0, 0);

        try {
            pattern = a.getString(R.styleable.ValueView_pattern);
            if (pattern == null) {
                pattern = "";
            }
        } finally {
            a.recycle();
        }

        setValue(0);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        if (this.pattern.equals(pattern)) {
            return;
        }

        this.pattern = pattern;
        setText("");

        value = Integer.MAX_VALUE;
    }

    public void setValue(int value) {
        if (this.value == value) {
            return;
        }

        this.value = value;

        StringBuilder text = new StringBuilder();

        int digits = Math.abs(value);
        for (int c = pattern.length() - 1; c >= 0; c--) {
            char character = pattern.charAt(c);

            if (Character.isDigit(character)) {
                int base = ((int)character) - CODEPOINT_0;
                if (base == 0) {
                    base = 10;
                }

                text.append(digits % base);

                digits /= base;
            } else if ('-' == character) {
                if (value < 0) {
                    text.append("-");
                }
            } else if ('+' == character){
                if (value < 0) {
                    text.append("-");
                } else {
                    text.append("+");
                }
            } else {
                text.append(character);
            }
        }

        text.reverse();

        setText(text.toString());
    }
}
