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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 */
public class GridScroll extends RecyclerView.OnScrollListener {

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {
        if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            View child = recyclerView.getChildAt(0);

            int top = child.getTop();
            int height = child.getHeight();

            LinearLayoutManager layoutManager = ((LinearLayoutManager)recyclerView.getLayoutManager());
            int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();

            if (top < -height / 2) {
                recyclerView.smoothScrollToPosition(firstVisiblePosition + 1);
            } else {
                recyclerView.smoothScrollToPosition(firstVisiblePosition);
            }
        }
    }
}
