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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
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
import svenmeier.coxswain.view.LabelView;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.LimitDialogFragment;
import svenmeier.coxswain.view.TargetDialogFragment;
import svenmeier.coxswain.view.ValueView;


public class ProgramActivity extends Activity implements AbstractValueFragment.Callback {

    private Gym gym;

    private ListView segmentsView;
    private SegmentsAdapter segmentsAdapter;

    private Program program;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

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

            ValueView targetView = index.get(R.id.segments_target);
            final LabelView targetLabelView = index.get(R.id.segments_target_label);
            if (segment.duration.get() > 0) {
                targetView.setPattern(getString(R.string.duration_short_pattern));
                targetView.setValue(segment.duration.get());
                targetLabelView.setText(getString(R.string.target_duration));
            } else if (segment.distance.get() > 0) {
                targetView.setPattern(getString(R.string.distance_pattern));
                targetView.setValue(segment.distance.get());
                targetLabelView.setText(getString(R.string.target_distance));
            } else if (segment.strokes.get() > 0) {
                targetView.setPattern(getString(R.string.strokes_pattern));
                targetView.setValue(segment.strokes.get());
                targetLabelView.setText(getString(R.string.target_strokes));
            }
            targetView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean was = segmentsView.isItemChecked(position);
                    segmentsView.setItemChecked(position, true);

                    new TargetDialogFragment().show(getFragmentManager(), "value");
                }
            });

            ValueView limitView = (ValueView) index.get(R.id.segments_limit);
            LabelView limitLabelView = (LabelView) index.get(R.id.segments_limit_label);
            if (segment.speed.get() > 0) {
                limitView.setPattern(getString(R.string.speed_pattern));
                limitView.setValue(segment.speed.get());
                limitLabelView.setText(getString(R.string.limit_speed));
            } else if (segment.pulse.get() > 0) {
                limitView.setPattern(getString(R.string.pulse_pattern));
                limitView.setValue(segment.pulse.get());
                limitLabelView.setText(getString(R.string.limit_pulse));
            } else if (segment.strokeRate.get() > 0) {
                limitView.setPattern(getString(R.string.strokeRate_pattern));
                limitView.setValue(segment.strokeRate.get());
                limitLabelView.setText(getString(R.string.limit_strokeRate));
            } else {
                limitView.setText("");
                limitLabelView.setText(getString(R.string.limit_none));
            }
            limitView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    segmentsView.setItemChecked(position, true);

                    new LimitDialogFragment().show(getFragmentManager(), "value");
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