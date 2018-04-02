package svenmeier.coxswain;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;


public class MySwipeRefreshLayout extends SwipeRefreshLayout {
    private View mScrollingView;

    public MySwipeRefreshLayout(Context context) {
        super(context);
        setRefreshing(false);
        setEnabled(false);

    }

    public MySwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setRefreshing(false);
        setEnabled(false);
    }

    @Override
    public boolean canChildScrollUp() {
        return mScrollingView != null && mScrollingView.canScrollVertically(-1);
    }

    public void setScrollingView(View scrollingView) {
        mScrollingView = scrollingView;
    }
}
