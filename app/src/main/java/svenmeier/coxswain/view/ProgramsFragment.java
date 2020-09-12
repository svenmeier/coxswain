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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import propoid.db.Order;
import propoid.ui.list.GenericRecyclerAdapter;
import propoid.ui.list.MatchRecyclerAdapter;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.ProgramActivity;
import svenmeier.coxswain.R;
import svenmeier.coxswain.WorkoutActivity;
import svenmeier.coxswain.gym.Program;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ProgramsFragment extends Fragment {

    private Gym gym;

    private RecyclerView programsView;

    private ProgramsAdapter adapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        gym = Gym.instance(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_programs, container, false);

        programsView = (RecyclerView) root.findViewById(R.id.programs);
        programsView.setLayoutManager(new LinearLayoutManager(getContext()));
        programsView.setHasFixedSize(true);
        programsView.setAdapter(adapter = new ProgramsAdapter());

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter.initLoader(0, this);
    }

    private class ProgramsAdapter extends MatchRecyclerAdapter<Program> {

        public ProgramsAdapter() {
            super(R.layout.layout_programs_item, gym.getPrograms());

            setOrder(Order.ascending(getMatch().getPrototype().name));
        }

        @Override
        protected GenericHolder createHolder(View v) {
            return new ProgramHolder(v);
        }
    }

    private class ProgramHolder extends GenericRecyclerAdapter.GenericHolder<Program> implements View.OnClickListener{

        private final TextView nameTextView;
        private final TextView durationTextView;
        private final SegmentsView segmentsView;
        private final ImageButton menuButton;

        public ProgramHolder(View view) {
            super(view);

            view.setOnClickListener(this);

            nameTextView = view.findViewById(R.id.program_name);
            durationTextView = view.findViewById(R.id.program_duration);
            segmentsView = view.findViewById(R.id.program_segments);

            menuButton = view.findViewById(R.id.program_menu);
            menuButton.setFocusable(false);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gym.deselect();

                    PopupMenu popup = new PopupMenu(getActivity(), menuButton);
                    popup.getMenuInflater().inflate(R.menu.menu_programs_item, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.action_select:
                                    gym.select(item);
                                    return true;
                                case R.id.action_new:
                                    Program newProgram = gym.newProgram();

                                    startActivity(ProgramActivity.createIntent(getActivity(), newProgram));
                                    return true;
                                case R.id.action_edit:
                                    startActivity(ProgramActivity.createIntent(getActivity(), item));
                                    return true;
                                case R.id.action_export:
                                    ExportProgramDialogFragment.create(item).show(getFragmentManager(), "export");

                                    return true;
                                case R.id.action_duplicate:
                                    Program duplicatedProgram = gym.duplicateProgram(item);

                                    startActivity(ProgramActivity.createIntent(getActivity(), duplicatedProgram));

                                    return true;
                                case R.id.action_delete:
                                    DeleteDialogFragment.create(item).show(getFragmentManager(), "delete");

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
            nameTextView.setText(item.name.get());

            durationTextView.setText(asHoursMinutes(item.asDuration()));

            segmentsView.setData(new SegmentsData(item));
        }

        @Override
        public void onClick(View v) {
            gym.select(item);

            WorkoutActivity.start(getActivity());
        }
    }

    private static String asHoursMinutes(int seconds) {
        return String.format("%d:%02d", SECONDS.toHours(seconds), SECONDS.toMinutes(seconds) % 60);
    }
}