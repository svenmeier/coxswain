package svenmeier.coxswain.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 */
public class LevelView extends View {

    private int level = 0;

    public LevelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LevelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setLevel(int level) {
        if (this.level != level) {
            this.level = level;

            invalidate();
        }
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return super.getSuggestedMinimumHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int left = getPaddingLeft();
        int right = getWidth() - getPaddingRight();
        int top = getPaddingTop();
        int bottom = getHeight() - getPaddingBottom();

        Drawable background = getBackground();
        if (background != null) {
            background.setLevel(level);
        }

        super.onDraw(canvas);
    }
}