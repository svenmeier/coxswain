package svenmeier.coxswain.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 */
public class DashLayout extends ViewGroup {

	public DashLayout(Context context) {
		super(context);
	}

	public DashLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DashLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

		int height = (bottom - top) - getPaddingTop() + getPaddingBottom();
		int width = (right - left) - getPaddingLeft() - getPaddingRight();

		int columns;
		if (width < height || getChildCount() <= 1) {
			columns = 1;
		}else {
			columns = 2;
		}

		int index = 0;

		for (int c = 0; c < columns; c++) {
			int childLeft = getPaddingLeft() + (c * width / columns);
			int childRight = getPaddingLeft() + ((c + 1) * width / columns);

			final int count = getChildCount() * (c + 1) / columns - index;
			for (int r = 0; r < count; r++) {
				View child = getChildAt(index);

				int childTop = getPaddingTop() + r * height / count;
				int childBottom = getPaddingTop() + (r + 1) * height / count;

				child.measure(MeasureSpec.makeMeasureSpec(childRight - childLeft, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childBottom - childTop, MeasureSpec.EXACTLY));
				child.layout(childLeft, childTop, childRight, childBottom);

				index++;
			}
		}
	}
}
