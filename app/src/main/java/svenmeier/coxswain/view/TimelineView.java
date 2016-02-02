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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import java.util.Calendar;

/**
 */
public class TimelineView extends View {

    private static final long DAY = 24*60*60*1000;

    private Paint paint = new Paint();

    private long time;

    private long window = 14 * DAY;

    private Interaction interaction;

    public TimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public TimelineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    private void init() {
        interaction = new Interaction();

        time = getUnit(System.currentTimeMillis()).min();
    }

    public Unit getUnit(long time) {
        long windowDays = window / DAY;
        if (windowDays > 60) {
            return new MonthUnit(time);
        } else if (windowDays > 14) {
            return new WeekUnit(time);
        } else {
            return new DayUnit(time);
        }
    }

    private long toTime(float display) {

        int height = getHeight();

        return (long)(display * window / height);
    }

    private float toDisplay(long time) {

        int height = getHeight();

        return time * height / window;
    }

    private void updateTime(long time) {
        this.time = Math.min(time, getUnit(System.currentTimeMillis()).min());

        postInvalidate();
    }

    private void setWindow(long window) {
        this.window = window;

        this.window = Math.min(this.window, 356 * DAY);
        this.window = Math.max(this.window, DAY);

        postInvalidate();;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        interaction.checkFling();

        super.onDraw(canvas);

        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xff000000);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(Utils.dpToPx(getContext(), 20));

        Unit unit = getUnit(time);
        for (int i = 0; true; i++) {
            unit.next();

            float y = toDisplay(time - unit.to());
            float height = toDisplay(time - unit.from()) - y;

            String text = DateUtils.formatDateTime(getContext(), unit.from(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
            draw(canvas, text, y, height);

            if (y > getHeight()) {
                break;
            }
        }
    }

    private void draw(Canvas canvas, String text, float y, float height) {
        canvas.drawLine(0, y, getWidth(), y, paint);
        canvas.drawText(text, 0f, y + height - Utils.dpToPx(getContext(), 2), paint);
    }


    private class Interaction extends GestureDetector.SimpleOnGestureListener implements OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

        private GestureDetector detector;

        private ScaleGestureDetector scaleDetector;

        private Scroller scroller;

        private int lastFlingY;

        private long unscaledWindow;

        public Interaction() {
            detector = new GestureDetector(getContext(), this);
            detector.setIsLongpressEnabled(false);

            scaleDetector = new ScaleGestureDetector(getContext(), this);

            scroller = new Scroller(getContext());

            setOnTouchListener(this);
        }

        /**
         * OnGestureListener
         */
        @Override
        public boolean onDown(MotionEvent e) {
            scroller.forceFinished(true);

            return true;
        }

        /**
         * OnTouchListener
         */
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            detector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);

            return true;
        }

        /**
         * OnGestureListener
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            updateTime(time - toTime(distanceY));

            return true;
        }

        /**
         * OnGestureListener
         */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            scroller.fling(0, 0, (int)velocityX, (int)velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            lastFlingY = 0;

            postInvalidate();

            return false;
        }

        public void checkFling() {
            if (scroller.computeScrollOffset()) {
                int flingY = scroller.getCurrY();

                updateTime(time + toTime(flingY - lastFlingY));

                lastFlingY = flingY;
            }
        }

        /**
         * OnScaleGestureListener
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            unscaledWindow = window;

            return true;
        }

        /**
         * OnScaleGestureListener
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();

            setWindow((long) (unscaledWindow / scale));

            return false;
        }

        /**
         * OnScaleGestureListener
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    }

    private interface Unit {

        Unit next();

        long min();

        long from();

        long to();
    }

    private class DayUnit implements Unit {

        private Calendar calendar;

        private long from;

        private long to;

        public DayUnit(long time) {
            calendar = Calendar.getInstance();

            calendar.setTimeInMillis(time);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            calendar.add(Calendar.DATE, 1);
        }

        @Override
        public long min() {
            return calendar.getTimeInMillis();
        }

        @Override
        public Unit next() {
            to = calendar.getTimeInMillis();
            calendar.add(Calendar.DATE, -1);
            from = calendar.getTimeInMillis();

            return this;
        }

        @Override
        public long from() {
            return from;
        }

        @Override
        public long to() {
            return to;
        }
    }

    private class WeekUnit implements Unit {

        private Calendar calendar;

        private long from;

        private long to;

        public WeekUnit(long time) {
            calendar = Calendar.getInstance();

            calendar.setTimeInMillis(time);
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            calendar.add(Calendar.DATE, 7);
        }

        @Override
        public long min() {
            return calendar.getTimeInMillis();
        }

        @Override
        public Unit next() {
            to = calendar.getTimeInMillis();
            calendar.add(Calendar.DATE, -7);
            from = calendar.getTimeInMillis();

            return this;
        }

        @Override
        public long from() {
            return from;
        }

        @Override
        public long to() {
            return to;
        }
    }

    private class MonthUnit implements Unit {

        private Calendar calendar;

        private long from;

        private long to;

        public MonthUnit(long time) {
            calendar = Calendar.getInstance();

            calendar.setTimeInMillis(time);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            calendar.add(Calendar.MONTH, 1);
        }

        @Override
        public long min() {
            return calendar.getTimeInMillis();
        }

        @Override
        public Unit next() {
            to = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, -1);
            from = calendar.getTimeInMillis();

            return this;
        }

        @Override
        public long from() {
            return from;
        }

        @Override
        public long to() {
            return to;
        }
    }
}