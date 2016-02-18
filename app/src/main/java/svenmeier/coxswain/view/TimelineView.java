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
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
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

    public static final long MINUTE = 60 * 1000;

    public static final long HOUR = 60 * MINUTE;

    public static final long DAY = 24 * HOUR;

    private Paint paint = new Paint();

    private long time;

    private long window = DAY;

    private Interaction interaction;

    private Periods periods;

    private RectF rect = new RectF();

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

        setPeriods(new DefaultPeriods());
    }

    public long getWindow() {
        return window;
    }

    @Override
    public Parcelable onSaveInstanceState() {

        SavedState state = new SavedState(super.onSaveInstanceState());

        state.time = time;
        state.window = window;

        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState state = (SavedState)parcelable;

        super.onRestoreInstanceState(state.getSuperState());

        time = state.time;
        window = state.window;
    }

    public void setPeriods(Periods periods) {
        this.periods = periods;

        time = periods.unit(periods.max(), window).ceiling();

        postInvalidate();
    }

    private long toTime(float display) {

        int height = getHeight();

        return (long)(display * window / height);
    }

    private float toDisplay(long time) {

        int height = getHeight();

        return time * height / window;
    }

    public void setTime(long time) {
        long max = periods.max();
        long min = periods.min();

        this.time = Math.min(time, periods.unit(max, window).ceiling());
        this.time = Math.max(this.time, periods.unit(min, window).floor() + window);

        postInvalidate();
    }

    public void setWindow(long window) {
        this.window = window;

        this.window = Math.min(this.window, periods.maxWindow());
        this.window = Math.max(this.window, periods.minWindow());

        setTime(time);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        interaction.checkFling();

        super.onDraw(canvas);

        paint.setFlags(Paint.ANTI_ALIAS_FLAG);

        Unit unit = periods.unit(time, window);
        for (int i = 0; true; i++) {
            unit.next();

            float y1 = toDisplay(time - unit.to());
            float y2 = toDisplay(time - unit.from());
            float x1 = 0;
            float x2 = getWidth();

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0x80808080);
            canvas.drawLine(0, y2, getWidth(), y2, paint);

            rect.set(x1, y1, x2, y2);
            periods.paint(unit.getClass(), unit.from(), unit.to(), canvas, rect);

            if (y2 > getHeight()) {
                break;
            }
        }
    }

    private class Interaction extends GestureDetector.SimpleOnGestureListener implements OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

        private GestureDetector detector;

        private ScaleGestureDetector scaleDetector;

        private Scroller scroller;

        private boolean preventClick;

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
         * OnTouchListener
         */
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            detector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (preventClick == false) {
                    performClick();
                }
            }

            return true;
        }

        /**
         * OnGestureListener
         */
        @Override
        public boolean onDown(MotionEvent e) {
            scroller.forceFinished(true);

            preventClick = false;

            return true;
        }

        /**
         * OnGestureListener
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            setTime(time - toTime(distanceY));

            preventClick = true;

            return false;
        }

        /**
         * OnGestureListener
         */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            scroller.fling(0, 0, (int)velocityX, (int)velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            lastFlingY = 0;

            postInvalidate();

            preventClick = true;

            return false;
        }

        public void checkFling() {
            if (scroller.computeScrollOffset()) {
                int flingY = scroller.getCurrY();

                setTime(time + toTime(flingY - lastFlingY));

                lastFlingY = flingY;
            }
        }

        /**
         * OnScaleGestureListener
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            unscaledWindow = window;

            preventClick = true;

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

    public interface Periods {

        long min();

        long max();

        Unit unit(long time, long window);

        void paint(Class<?> unit, long from, long to, Canvas canvas, RectF rect);

        long minWindow();

        long maxWindow();
    }

    public class DefaultPeriods implements Periods {

        @Override
        public long min() {
            return 0;
        }

        @Override
        public long max() {
            return System.currentTimeMillis();
        }

        @Override
        public long minWindow() {
            return DAY;
        }

        @Override
        public long maxWindow() {
            return 356 * DAY;
        }

        @Override
        public Unit unit(long time, long window) {
            return new DayUnit(time);
        }

        @Override
        public void paint(Class<?> unit, long from, long to, Canvas canvas, RectF rect) {
        }
    }

    public interface Unit {

        long floor();

        long ceiling();

        void next();

        long from();

        long to();
    }

    public static class MinuteUnit implements Unit {

        private Calendar calendar;

        private int mod;

        private long floor;

        private long ceiling;

        private long from;

        private long to;

        public MinuteUnit(long time) {
            this(time, 1);
        }

        public MinuteUnit(long time, int mod) {
            this.mod = mod;

            calendar = Calendar.getInstance();

            calendar.setTimeInMillis(time);
            int minute = calendar.get(Calendar.MINUTE);
            calendar.set(Calendar.MINUTE, minute - (minute % mod));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            floor = calendar.getTimeInMillis();
            calendar.add(Calendar.MINUTE, mod);
            ceiling = calendar.getTimeInMillis();
        }

        @Override
        public long floor() {
            return floor;
        }

        @Override
        public long ceiling() {
            return ceiling;
        }

        @Override
        public void next() {
            to = calendar.getTimeInMillis();
            calendar.add(Calendar.MINUTE, -mod);
            from = calendar.getTimeInMillis();
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

    public static class HourUnit implements Unit {

        private Calendar calendar;

        private long floor;

        private long ceiling;

        private long from;

        private long to;

        public HourUnit(long time) {
            calendar = Calendar.getInstance();

            calendar.setTimeInMillis(time);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            floor = calendar.getTimeInMillis();
            calendar.add(Calendar.HOUR, 1);
            ceiling = calendar.getTimeInMillis();
        }

        @Override
        public long floor() {
            return floor;
        }

        @Override
        public long ceiling() {
            return ceiling;
        }

        @Override
        public void next() {
            to = calendar.getTimeInMillis();
            calendar.add(Calendar.HOUR, -1);
            from = calendar.getTimeInMillis();
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

    public static class DayUnit implements Unit {

        private Calendar calendar;

        private long floor;

        private long ceiling;

        private long from;

        private long to;

        public DayUnit(long time) {
            calendar = Calendar.getInstance();

            calendar.setTimeInMillis(time);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            floor = calendar.getTimeInMillis();
            calendar.add(Calendar.DATE, 1);
            ceiling = calendar.getTimeInMillis();
        }

        @Override
        public long floor() {
            return floor;
        }

        @Override
        public long ceiling() {
            return ceiling;
        }

        @Override
        public void next() {
            to = calendar.getTimeInMillis();
            calendar.add(Calendar.DATE, -1);
            from = calendar.getTimeInMillis();
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

    public static class WeekUnit implements Unit {

        private Calendar calendar;

        private long floor;

        private long ceiling;

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

            floor = calendar.getTimeInMillis();
            calendar.add(Calendar.DATE, 7);
            ceiling = calendar.getTimeInMillis();
        }

        @Override
        public long floor() {
            return floor;
        }

        @Override
        public long ceiling() {
            return ceiling;
        }

        @Override
        public void next() {
            to = calendar.getTimeInMillis();
            calendar.add(Calendar.DATE, -7);
            from = calendar.getTimeInMillis();
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

    public static class MonthUnit implements Unit {

        private Calendar calendar;

        private long floor;

        private long ceiling;

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

            floor = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, 1);
            ceiling = calendar.getTimeInMillis();
        }

        @Override
        public long floor() {
            return floor;
        }

        @Override
        public long ceiling() {
            return ceiling;
        }

        @Override
        public void next() {
            to = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, -1);
            from = calendar.getTimeInMillis();
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

    public static class SavedState extends BaseSavedState {

        long time;
        long window;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);

            out.writeLong(time);
            out.writeLong(window);
        }

        @Override
        public String toString() {
            String str = "TimelineView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " time=" + time + " window=" + window + "}";

            return str;
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private SavedState(Parcel in) {
            super(in);

            time = in.readLong();
            window = in.readLong();
        }
    }
}