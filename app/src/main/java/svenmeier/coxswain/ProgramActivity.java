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
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;

import propoid.db.Reference;
import propoid.ui.Index;
import propoid.ui.list.GenericAdapter;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.view.AbstractValueFragment;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.LimitDialogFragment;
import svenmeier.coxswain.view.TargetDialogFragment;
import svenmeier.coxswain.view.ValueBinding;
import svenmeier.coxswain.view.BindingView;


public class ProgramActivity extends AbstractActivity implements AbstractValueFragment.Callback {

    private Gym gym;

    private ListView segmentsView;
    private SegmentsAdapter segmentsAdapter;

    private Program program;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.layout_program);

        segmentsView = (ListView) findViewById(R.id.program_segments);

        Reference<Program> reference = Reference.from(getIntent());

        program = gym.getProgram(reference);
        if (program == null) {
            finish();
        } else {
            segmentsAdapter = new SegmentsAdapter();
            segmentsAdapter.install(segmentsView);

            setTitle(program.name.get());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class SegmentsAdapter extends GenericAdapter<Segment> {

        public SegmentsAdapter() {
            super(R.layout.layout_segments_item, program.getSegments());
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        protected void bind(final int position, View view, final Segment segment) {
            Index index = Index.get(view);

            BindingView targetView = index.get(R.id.segments_item_target);
            if (segment.duration.get() > 0) {
                targetView.setBinding(ValueBinding.DURATION_SHORT);
                targetView.changed(segment.duration.get());
            } else if (segment.distance.get() > 0) {
                targetView.setBinding(ValueBinding.DISTANCE);
                targetView.changed(segment.distance.get());
            } else if (segment.strokes.get() > 0) {
                targetView.setBinding(ValueBinding.STROKES);
                targetView.changed(segment.strokes.get());
            } else if (segment.energy.get() > 0) {
                targetView.setBinding(ValueBinding.ENERGY);
                targetView.changed(segment.energy.get());
            }
            targetView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean was = segmentsView.isItemChecked(position);
                    segmentsView.setItemChecked(position, true);

                    new TargetDialogFragment().show(getFragmentManager(), "changed");
                }
            });

            BindingView limitView = index.get(R.id.segments_item_limit);
            if (segment.speed.get() > 0) {
                limitView.setBinding(ValueBinding.SPEED);
                limitView.changed(segment.speed.get());
            } else if (segment.pulse.get() > 0) {
                limitView.setBinding(ValueBinding.PULSE);
                limitView.changed(segment.pulse.get());
            } else if (segment.strokeRate.get() > 0) {
                limitView.setBinding(ValueBinding.STROKE_RATE);
                limitView.changed(segment.strokeRate.get());
            } else {
                limitView.setBinding(ValueBinding.NONE);
                limitView.changed(0);
            }
            limitView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    segmentsView.setItemChecked(position, true);

                    new LimitDialogFragment().show(getFragmentManager(), "changed");
                }
            });

            LevelView difficultyView = (LevelView) index.get(R.id.segments_difficulty);
            difficultyView.setLevel(segment.difficulty.get().ordinal());
            difficultyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    segment.difficulty.set(segment.difficulty.get().increase());

                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                    notifyChanged();
                }
            });

            final ImageButton menuButton = (ImageButton) index.get(R.id.segment_menu);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(ProgramActivity.this, menuButton);
                    popup.getMenuInflater().inflate(R.menu.menu_segments_item, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.action_delete:
                                    program.removeSegment(segment);

                                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                                    notifyChanged();
                                    return true;
                                case R.id.action_insert_before:
                                    program.createSegmentBefore(segment);

                                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                                    notifyChanged();
                                    return true;
                                case R.id.action_insert_after:
                                    program.createSegmentAfter(segment);

                                    Gym.instance(ProgramActivity.this).mergeProgram(program);

                                    notifyChanged();
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
    }

    @Override
    public Segment getSegment() {
        return (Segment) segmentsView.getAdapter().getItem(segmentsView.getCheckedItemPosition());
    }

    public void setSegment(Segment segment) {
        Gym.instance(this).mergeProgram(program);

        segmentsAdapter.notifyChanged();
    }

    public static Intent createIntent(Context context, Program program) {
        Intent intent = new Intent(context, ProgramActivity.class);

        intent.setData(new Reference<Program>(program).toUri());

        return intent;
    }
}