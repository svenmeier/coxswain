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

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import propoid.ui.Index;
import propoid.ui.list.MatchAdapter;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.ProgramActivity;
import svenmeier.coxswain.R;
import svenmeier.coxswain.WorkoutActivity;
import svenmeier.coxswain.gym.Program;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ProgramsFragment extends Fragment {

    private Gym gym;

    private ListView programsView;

    private ProgramsAdapter adapter;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_programs, container, false);

        programsView = (ListView) root.findViewById(R.id.programs);
        adapter = new ProgramsAdapter();
        adapter.install(programsView);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        adapter.initLoader(0, this);
    }

    @Override
    public void onStop() {
        super.onStop();

        adapter.destroy(0, this);
    }

    private class ProgramsAdapter extends MatchAdapter<Program> {

        public ProgramsAdapter() {
            super(R.layout.layout_programs_item, gym.getPrograms());
        }

        @Override
        protected void bind(final int position, View view, final Program program) {

            Index index = Index.get(view);

            TextView nameTextView = index.get(R.id.program_name);
            nameTextView.setText(program.name.get());

            TextView durationTextView = index.get(R.id.program_duration);
            durationTextView.setText(asHoursMinutes(program.asDuration()));

            SegmentsView segmentsView = index.get(R.id.program_segments);
            segmentsView.setData(new SegmentsData(program));

            final ImageButton menuButton = index.get(R.id.program_menu);
            menuButton.setFocusable(false);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gym.deselect();

                    PopupMenu popup = new PopupMenu(getActivity(), menuButton);
                    popup.getMenuInflater().inflate(R.menu.menu_programs_item, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.action_new:
                                    Gym.instance(getActivity()).mergeProgram(new Program("Program"));

                                    return true;
                                case R.id.action_rename:
                                    NameDialogFragment.create(program).show(getFragmentManager(), "name");
                                    return true;
                                case R.id.action_edit:
                                    startActivity(ProgramActivity.createIntent(getActivity(), program));
                                    return true;
                                case R.id.action_export:
                                    ExportProgramDialogFragment.create(program).show(getFragmentManager(), "export");

                                    return true;
                                case R.id.action_delete:
                                    DeleteDialogFragment.create(program).show(getFragmentManager(), "delete");

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
        protected void onItem(Program program, int position) {
            gym.repeat(program);

            WorkoutActivity.start(getActivity());
        }
    }

    private static String asHoursMinutes(int seconds) {
        return String.format("%d:%02d", SECONDS.toHours(seconds), SECONDS.toMinutes(seconds) % 60);
    }
}