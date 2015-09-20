package svenmeier.coxswain.view;

import android.view.View;
import android.widget.AbsListView;

/**
 * Created by sven on 06.09.15.
 */
public class GridScroll implements AbsListView.OnScrollListener {
    @Override
    public void onScrollStateChanged(AbsListView listView, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE) {
            View child = listView.getChildAt(0);

            int top = child.getTop();
            int height = child.getHeight();

            if (top < -height / 2) {
                listView.smoothScrollToPosition(listView.getFirstVisiblePosition() + 1);
            } else {
                listView.smoothScrollToPosition(listView.getFirstVisiblePosition());
            }
        }
    }

    @Override
    public void onScroll(AbsListView listView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }
}
