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
package svenmeier.coxswain;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.widget.EditText;

import com.woxthebox.draglistview.DragListView;
import com.woxthebox.draglistview.swipe.ListSwipeHelper;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import java.util.List;

import propoid.db.Reference;
import propoid.db.aspect.Row;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.view.AbstractValueFragment;


public class ProgramActivity extends AbstractActivity implements AbstractValueFragment.Callback {

    private Gym gym;

    private EditText nameView;

    private ItemAdapter segmentsAdapter;

    private Program program;
    private MySwipeRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setContentView(R.layout.layout_program_edit); // change

        nameView = findViewById(R.id.toolbar_edit_edit);

        final DragListView mDragListView = this.findViewById(R.id.drag_list_view);

        mDragListView.setDragListListener(new DragListView.DragListListener() {
            @Override
            public void onItemDragStarted(int position) {

            }

            @Override
            public void onItemDragging(int itemPosition, float x, float y) {

            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {

            }
        });

        mDragListView.setLayoutManager(new LinearLayoutManager(this));


        Reference<Program> reference = Reference.from(getIntent());

        program = gym.getProgram(reference);
        if (program == null) {
            finish();
        } else {
            nameView.setText(program.name.get());

        }


        List<Segment> segments = program.segments.get();
        ItemAdapter listAdapter = new ItemAdapter(segments, R.layout.layout_segments_item_edit, R.id.move, true,
                getSupportFragmentManager(), program, gym);
        segmentsAdapter = listAdapter;
        mDragListView.setAdapter(listAdapter, false);
        mDragListView.setCanDragHorizontally(false);
        mRefreshLayout = this.findViewById(R.id.swipe_refresh_layout);

        mDragListView.setSwipeListener(new ListSwipeHelper.OnSwipeListenerAdapter() {
            @Override
            public void onItemSwipeStarted(ListSwipeItem item) {
            }

            @Override
            public void onItemSwipeEnded(ListSwipeItem item, ListSwipeItem.SwipeDirection swipedDirection) {

                if (swipedDirection == ListSwipeItem.SwipeDirection.LEFT ||
                        swipedDirection == ListSwipeItem.SwipeDirection.RIGHT) {
                    Segment adapterItem = (Segment) item.getTag();
                    int pos = mDragListView.getAdapter().getPositionForItem(adapterItem);
                    ItemAdapter adapter = (ItemAdapter) mDragListView.getAdapter();

                    adapter.removeItem(pos);
                    program.removeSegment(adapterItem);

                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                    segmentsAdapter.notifyDataSetChanged();
                    if (adapter.getItemCount() == 0) {
                        program.addSegment(new Segment(Difficulty.EASY));
                        Gym.instance(ProgramActivity.this).mergeProgram(program);

                        segmentsAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (program != null) {
            program.name.set(nameView.getText().toString());
            Gym.instance(ProgramActivity.this).mergeProgram(program);
        }
    }


    @Override
    public void onChanged(Segment segment) {
        List<Segment> segments = program.getSegments();

        int index = 0;
        for (Segment candidate : segments) {
            if (Row.getID(candidate) == Row.getID(segment)) {
                segments.set(index, segment);
                break;
            }
            index++;
        }

        segmentsAdapter.notifyDataSetChanged();
    }

    public static Intent createIntent(Context context, Program program) {
        Intent intent = new Intent(context, ProgramActivity.class);

        intent.setData(new Reference<>(program).toUri());

        return intent;
    }
}