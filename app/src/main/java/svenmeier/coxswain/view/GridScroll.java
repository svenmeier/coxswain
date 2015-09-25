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
