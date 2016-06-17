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
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import svenmeier.coxswain.R;

/**
 * A {@link }LabelView} specialization for displaying of integer values.
 * <br>
 * A pattern allows to format each digit separately:
 * <dl>
 *     <dt>0</dt>
 *     <dd>a decimal digit</dd>
 *     <dt>6</dt>
 *     <dd>a senary digit (as used as the second digit in minutes and hours)</dd>
 *     <dt>F</dt>
 *     <dd>a hexadecimal digit</dd>
 *     <dt>-</dt>
 *     <dd>an optinal minus sign (only shown if value is negative)</dd>
 *     <dt>+</dt>
 *     <dd>a forced minus or plus sign (always shown)</dd>
 * </dl>
 */
public class ValueView extends LabelView {

    private static final int CODEPOINT_0 = 48;

    private String pattern = "";

    private int value = Integer.MAX_VALUE;

    public ValueView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ValueView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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

        String text = format(value);

        setText(text);
    }

    public String format(int value) {
        StringBuilder text = new StringBuilder();

        int digits = Math.abs(value);
        for (int c = pattern.length() - 1; c >= 0; c--) {
            char character = pattern.charAt(c);

            if ('0' == character) {
                // decimal
                text.append(digits % 10);

                digits /= 10;
            } else if ('6' == character) {
                // minutes or hours
                text.append(digits % 6);

                digits /= 6;
            } else if ('F' == character) {
                // hexadecimal
                text.append(Integer.toHexString(digits % 0xF));

                digits /= 0xF;
            } else if ('-' == character) {
                // optional sign
                if (value < 0) {
                    text.append("-");
                }
            } else if ('+' == character){
                // forced sign
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

        return text.toString();
    }
}
