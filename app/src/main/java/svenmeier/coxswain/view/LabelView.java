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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import svenmeier.coxswain.R;

/**
 */
public class LabelView extends View {

    private String text = "";

    private Paint paint = new Paint();

    private float width;

    private int foreground = 0xff000000;

    private float size;

    private int align;

    public LabelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public LabelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.LabelView,
                0, 0);

        try {
            text = a.getString(R.styleable.LabelView_text);
            if (text == null) {
                text = "";
            }
            align = a.getInt(R.styleable.LabelView_align, 0);
        } finally {
            a.recycle();
        }

        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        size = 0f;
    }

    public void setText(String text) {
        int length = this.text.length();

        this.text = text;

        if (length != this.text.length()) {
            size = 0f;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int left = getPaddingLeft();
        int right = getWidth() - getPaddingRight();
        int top = getPaddingTop();
        int bottom = getHeight() - getPaddingBottom();

        if (size == 0f) {
            Rect textBounds = new Rect();
            paint.setTextSize(100f);
            paint.getTextBounds(text, 0, text.length(), textBounds);
            size = 100f * (bottom - top)/ textBounds.height();
            paint.setTextSize(size);
            width = paint.measureText(text);
            if (width > right - left) {
                paint.setTextScaleX((right - left) / width);
                width = paint.measureText(text);
            }
        }

        float x;
        if (align == -1) {
            x = left;
        } else if (align == 1) {
            x = right - width;
        } else {
            x = (right - left) / 2 - width/2;
        }
        int y = bottom - 1;

        paint.setColor(foreground);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(text, x, y, paint);
    }
}
