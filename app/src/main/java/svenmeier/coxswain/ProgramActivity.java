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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.Collections;
import java.util.List;

import propoid.db.Reference;
import propoid.db.aspect.Row;
import propoid.ui.list.GenericRecyclerAdapter;
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
    private SegmentsAdapter segmentsAdapter;

    private Program program;
    private ItemTouchHelper touchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setContentView(R.layout.layout_program);

        nameView = findViewById(R.id.toolbar_edit);

        segmentsView = findViewById(R.id.program_segments);
        segmentsView.setLayoutManager(new LinearLayoutManager(this));
        segmentsView.setHasFixedSize(true);

        Reference<Program> reference = Reference.from(getIntent());

        program = gym.getProgram(reference);
        if (program == null) {
            finish();
        } else {
            nameView.setText(program.name.get());

            segmentsView.setAdapter(segmentsAdapter = new SegmentsAdapter());

            touchHelper = new ItemTouchHelper(new SegmentsMover());
            touchHelper.attachToRecyclerView(segmentsView);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (program != null) {
            program.name.set(nameView.getText().toString());
            Gym.instance(ProgramActivity.this).mergeProgram(program);
        }
    }

    private class SegmentsMover extends ItemTouchHelper.SimpleCallback {

        public SegmentsMover() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
            int fromPosition = from.getAdapterPosition();
            int toPosition = to.getAdapterPosition();

            Collections.swap(program.getSegments(), fromPosition, toPosition);

            segmentsAdapter.notifyItemMoved(fromPosition, toPosition);

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            program.removeSegment(program.getSegment(position));

            Gym.instance(ProgramActivity.this).mergeProgram(program);

            segmentsAdapter.notifyItemRemoved(position);
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
                    Segment duplicate = program.duplicateSegment(item);

                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                    segmentsAdapter.notifyItemInserted(program.getSegments().indexOf(duplicate));
                }
            });
            menuButton.setOnTouchListener(new View.OnTouchListener() {

                private float y;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP) {
                        y = event.getY();
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (Math.abs(event.getY() - y) > 5) {
                            touchHelper.startDrag(SegmentHolder.this);

                            return true;
                        }
                    }

                    return false;
                }
            });
        }

        @Override
        protected void onBind() {
            if (item.duration.get() > 0) {
                targetView.setBinding(ValueBinding.DURATION);
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
            } else if (item.power.get() > 0) {
                limitView.setBinding(ValueBinding.POWER);
                limitView.changed(item.power.get());
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

                    segmentsAdapter.notifyItemChanged(getAdapterPosition());
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