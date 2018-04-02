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
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.woxthebox.draglistview.DragListView;
import com.woxthebox.draglistview.swipe.ListSwipeHelper;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import java.util.List;

import propoid.db.Reference;
import propoid.db.aspect.Row;
import propoid.ui.list.GenericRecyclerAdapter;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.view.AbstractValueFragment;
import svenmeier.coxswain.view.BindingView;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.ValueBinding;


public class ProgramActivity extends AbstractActivity implements AbstractValueFragment.Callback {

    private Gym gym;

    private EditText nameView;

    private RecyclerView segmentsView;
    private ItemAdapter segmentsAdapter;

    private Program program;
    private MySwipeRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setContentView(R.layout.layout_program_edit); // change

        nameView = (EditText) findViewById(R.id.toolbar_edit_edit);

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

            //segmentsView.setAdapter(segmentsAdapter = new SegmentsAdapter());
        }


        List<Segment> segments = program.segments.get();
        ItemAdapter listAdapter = new ItemAdapter(segments, R.layout.layout_segments_item_edit, R.id.move, true,
                getSupportFragmentManager(), program, gym);
        segmentsAdapter = listAdapter;
        mDragListView.setAdapter(listAdapter, false);
        mDragListView.setCanDragHorizontally(false);
        mRefreshLayout = (MySwipeRefreshLayout) this.findViewById(R.id.swipe_refresh_layout);

        mDragListView.setSwipeListener(new ListSwipeHelper.OnSwipeListenerAdapter() {
            @Override
            public void onItemSwipeStarted(ListSwipeItem item) {
                mRefreshLayout.setEnabled(false);
            }

            @Override
            public void onItemSwipeEnded(ListSwipeItem item, ListSwipeItem.SwipeDirection swipedDirection) {
                mRefreshLayout.setEnabled(true);

                // Swipe to delete on left 666
                if (swipedDirection == ListSwipeItem.SwipeDirection.LEFT) {
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
                        // 666 adapter.addItem(0, new Pair<>(1L, "xxx"));
                    }
                }
            }
        });


//        segmentsView = (RecyclerView) findViewById(R.id.program_segments);
//        segmentsView.setLayoutManager(new LinearLayoutManager(this));
//        segmentsView.setHasFixedSize(true);
//
//        Reference<Program> reference = Reference.from(getIntent());
//
//        program = gym.getProgram(reference);
//        if (program == null) {
//            finish();
//        } else {
//            nameView.setText(program.name.get());
//
//            segmentsView.setAdapter(segmentsAdapter = new SegmentsAdapter());
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (program != null) {
            program.name.set(nameView.getText().toString());
            Gym.instance(ProgramActivity.this).mergeProgram(program);
        }
    }

    private class SegmentsAdapter extends GenericRecyclerAdapter<Segment> {

        public SegmentsAdapter() {
            super(R.layout.layout_segments_item, program.getSegments());
        }

        @Override
        protected GenericHolder createHolder(View v) {
            return new SegmentHolder(v);
        }
    }

    private class SegmentHolder extends GenericRecyclerAdapter.GenericHolder<Segment> {

        private final BindingView targetView;
        private final BindingView limitView;
        private final LevelView difficultyView;
        private final ImageButton menuButton;

        public SegmentHolder(View v) {
            super(v);

            targetView = (BindingView) v.findViewById(R.id.segments_item_target);
            limitView = (BindingView) v.findViewById(R.id.segments_item_limit);
            difficultyView = (LevelView) v.findViewById(R.id.segments_difficulty);

            menuButton = (ImageButton) v.findViewById(R.id.segment_menu);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(ProgramActivity.this, menuButton);
                    popup.getMenuInflater().inflate(R.menu.menu_segments_item, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.action_delete:
                                    program.removeSegment(item);

                                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                                    segmentsAdapter.notifyDataSetChanged();
                                    return true;
                                case R.id.action_insert_before:
                                    program.createSegmentBefore(item);

                                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                                    segmentsAdapter.notifyDataSetChanged();
                                    return true;
                                case R.id.action_insert_after:
                                    program.createSegmentAfter(item);

                                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                                    segmentsAdapter.notifyDataSetChanged();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });

                    popup.show();
                }
            });
        }

        @Override
        protected void onBind() {
            if (item.duration.get() > 0) {
                targetView.setBinding(ValueBinding.DURATION_SHORT);
                targetView.changed(item.duration.get());
            } else if (item.distance.get() > 0) {
                targetView.setBinding(ValueBinding.DISTANCE);
                targetView.changed(item.distance.get());
            } else if (item.strokes.get() > 0) {
                targetView.setBinding(ValueBinding.STROKES);
                targetView.changed(item.strokes.get());
            } else if (item.energy.get() > 0) {
                targetView.setBinding(ValueBinding.ENERGY);
                targetView.changed(item.energy.get());
            }
            targetView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AbstractValueFragment.createTarget(item).show(getSupportFragmentManager(), "changed");
                }
            });

            if (item.speed.get() > 0) {
                limitView.setBinding(ValueBinding.SPEED);
                limitView.changed(item.speed.get());
            } else if (item.pulse.get() > 0) {
                limitView.setBinding(ValueBinding.PULSE);
                limitView.changed(item.pulse.get());
            } else if (item.strokeRate.get() > 0) {
                limitView.setBinding(ValueBinding.STROKE_RATE);
                limitView.changed(item.strokeRate.get());
            } else {
                limitView.setBinding(ValueBinding.NONE);
                limitView.changed(0);
            }
            limitView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AbstractValueFragment.createLimit(item).show(getSupportFragmentManager(), "changed");
                }
            });

            difficultyView.setLevel(item.difficulty.get().ordinal());
            difficultyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.difficulty.set(item.difficulty.get().increase());

                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                    segmentsAdapter.notifyDataSetChanged();
                }
            });
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

        intent.setData(new Reference<Program>(program).toUri());

        return intent;
    }
}