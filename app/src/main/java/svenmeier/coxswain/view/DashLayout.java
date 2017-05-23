package svenmeier.coxswain.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import svenmeier.coxswain.R;

/**
 */
public class DashLayout extends ViewGroup {

	private int columns = 1;

	public DashLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs);
	}

	public DashLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.DashLayout,
				0, 0);

		try {
			columns = a.getInteger(R.styleable.DashLayout_columns, 1);
		} finally {
			a.recycle();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

		int width = (right - left) - getPaddingLeft() - getPaddingRight();
		int height = (bottom - top) - getPaddingTop() - getPaddingBottom();

		int rows = (getChildCount() + (columns - 1)) / columns;

		int index = 0;
		for (int c = 0; c < columns; c++) {
			int childLeft = getPaddingLeft() + (c * width / columns);
			int childRight = getPaddingLeft() + ((c + 1) * width / columns);

			for (int r = 0; r < rows; r++) {
				if (index < getChildCount()) {
					View child = getChildAt(index);

					int childTop = getPaddingTop() + r * height / rows;
					int childBottom = getPaddingTop() + (r + 1) * height / rows;

					child.measure(MeasureSpec.makeMeasureSpec(childRight - childLeft, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childBottom - childTop, MeasureSpec.EXACTLY));
					child.layout(childLeft, childTop, childRight, childBottom);
				}

				index++;
			}
		}
	}
}