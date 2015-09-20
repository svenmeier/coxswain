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
    }

    public void setState(int state) {
        this.state[0] = state;

        refreshDrawableState();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (state[0] == 0) {
            return super.onCreateDrawableState(extraSpace);
        } else {
            final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

            mergeDrawableStates(drawableState, state);

            return drawableState;
        }
    }
}