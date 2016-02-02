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
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import svenmeier.coxswain.R;

/**
 */
@Deprecated
public class ChartView extends View {

    private static final int VALUES = 60;

    private static final int VALUES_PER_TICK = 30;

    private Paint paint = new Paint();

    private Path tick = new Path();

    private Path curve = new Path();

    private DashPathEffect dash;

    private int fill;

    private int stroke;

    private int ticks;

    private int previousLength;

    private Data data = new Data() {
        @Override
        public int length() {
            return 0;
        }

        @Override
        public float max() {
            return 0f;
        }

        @Override
        public float value(int index) {
            throw new IllegalArgumentException();
        }
    };


    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ChartView,
                0, 0);

        try {
            fill = a.getColor(R.styleable.ChartView_fill, Color.WHITE);
            stroke = a.getColor(R.styleable.ChartView_stroke, Color.BLACK);
            ticks = a.getColor(R.styleable.ChartView_ticks, Color.GRAY);
        } finally {
            a.recycle();
        }

        float d = Utils.dpToPx(getContext(), 4);
        dash = new DashPathEffect(new float[]{d, d}, 0);
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

        int snapshotCount = VALUES;
        drawTicks(canvas, left, right, top, bottom, snapshotCount);

        drawCurve(canvas, left, right, top, bottom, snapshotCount);
    }

    private void drawTicks(Canvas canvas, int left, int right, int top, int bottom, int snapshotCount) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(ticks);
        paint.setStrokeWidth(Utils.dpToPx(getContext(), 1));
        paint.setPathEffect(dash);

        canvas.drawLine(left, bottom, right, bottom, paint);
        canvas.drawLine(right, bottom, right, top, paint);

        float offset = data.length() % VALUES_PER_TICK;
        int i = 0;
        while (true) {
            float x = right - (offset + (i * VALUES_PER_TICK)) * (right - left) / (snapshotCount - 1);
            if (x < left) {
                break;
            }

            tick.reset();
            tick.moveTo(x, bottom);
            tick.lineTo(x, top);

            canvas.drawPath(tick, paint);

            i++;
        }

        paint.setPathEffect(null);
    }

    private void drawCurve(Canvas canvas, int left, int right, int top, int bottom, int snapshotCount) {
        traceCurve(curve, left, right, top, bottom, snapshotCount);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawPath(curve, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(stroke);
        paint.setStrokeWidth(Utils.dpToPx(getContext(), 1));

        canvas.drawPath(curve, paint);
    }

    private void traceCurve(Path curve, int left, int right, int top, int bottom, int count) {
        int length = data.length();
        if (length == previousLength) {
            return;
        }
        previousLength = length;

        float max = data.max();

        curve.reset();
        curve.moveTo(right, bottom);

        float x = right;
        for (int c = 0; c < Math.min(length, count); c++) {
            float value = data.value(length - 1 - c);

            x = right - (right - left) * c / (count - 1);
            float y = bottom - Math.min(value / max, 1f) * (bottom - top);

            curve.lineTo(x, y);
        }

        curve.lineTo(x, bottom);
        curve.close();
    }

    public static interface Data {

        int length();

        float max();

        float value(int index);
    }
}