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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import svenmeier.coxswain.R;

/**
 */
public class SegmentsView extends View {

    private Data data = new Data() {

        @Override
        public int length() {
            return 1;
        }

        @Override
        public float value(int index) {
            return 1.0f;
        }

        @Override
        public float total() {
            return 1.0f;
        }

        @Override
        public int level(int index) {
            return 0;
        }
    };

    private Drawable drawable;

    private int orientation;

    private Paint paint = new Paint();

    private Path arrow = new Path();

    public SegmentsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public SegmentsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.SegmentsView, 0, 0);

        try {
            drawable = a.getDrawable(R.styleable.SegmentsView_drawable);

            orientation = a.getInt(R.styleable.SegmentsView_orientation, 0);
        } finally {
            a.recycle();
        }
    }

    public void setData(Data data) {
        this.data = data;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int left = getPaddingLeft();
        int right = getWidth() - getPaddingRight();
        int top = getPaddingTop();
        int bottom = getHeight() - getPaddingBottom();

        paint.setFlags(Paint.ANTI_ALIAS_FLAG);

        if (orientation == 0) {
            drawHorizontal(canvas, left, right, top, bottom);
        } else {
            drawVertical(canvas, left, right, top, bottom);
        }
    }

    private void drawVertical(Canvas canvas, int left, int right, int top, int bottom) {
        int length = data.length();

        float ratio = 0f;
        int y = top;
        for (int i = 0; i < length; i++) {
            ratio += data.value(i);

            int next;
            if (i == length - 1) {
                next = bottom;
            } else {
                next = (int) ((bottom - top) * (ratio / data.total()));
            }

            drawable.setLevel(data.level(i));
            drawable.setBounds(left, y, right, next);
            drawable.draw(canvas);

            y = next;
        }
    }

    private void drawHorizontal(Canvas canvas, int left, int right, int top, int bottom) {
        int length = data.length();

        float ratio = 0f;
        int x = left;
        for (int i = 0; i < length; i++) {
            ratio += data.value(i);

            int next;
            if (i == length - 1) {
                next = right;
            } else {
                next = (int) ((right - left) * (ratio / data.total()));
            }

            drawable.setLevel(data.level(i));
            drawable.setBounds(x, top, next, bottom);
            drawable.draw(canvas);

            x = next;
        }
    }

    public static interface Data {
        public int length();

        public float value(int index);

        public int level(int index);

        public float total();
    }
}