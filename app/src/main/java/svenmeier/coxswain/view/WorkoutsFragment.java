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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import propoid.db.LookupException;
import propoid.db.Order;
import propoid.ui.Index;
import propoid.ui.list.MatchAdapter;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.ProgramActivity;
import svenmeier.coxswain.R;
import svenmeier.coxswain.SnapshotsActivity;
import svenmeier.coxswain.WorkoutActivity;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Workout;


public class WorkoutsFragment extends Fragment {

    private Gym gym;

    private ListView workoutsView;

    private WorkoutsAdapter adapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_workouts, container, false);

        workoutsView = (ListView) root.findViewById(R.id.workouts);
        adapter = new WorkoutsAdapter();
        adapter.install(workoutsView);

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

    private class WorkoutsAdapter extends MatchAdapter<Workout> {

        public WorkoutsAdapter() {
            super(R.layout.layout_workouts_item, Gym.instance(getActivity()).getWorkouts());

            setOrder(Order.descending(getMatch().getPrototype().start));
        }

        @Override
        protected void bind(int position, View view, final Workout workout) {
            Index index = Index.get(view);

            TextView startView = index.get(R.id.workout_start);
            startView.setText(DateUtils.formatDateTime(getActivity(), workout.start.get(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

            final ImageButton menuButton = index.get(R.id.workout_menu);
            menuButton.setFocusable(false);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), menuButton);
                    popup.getMenuInflater().inflate(R.menu.menu_workout_item, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.action_delete:
                                    gym.deleteWorkout(workout);

                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });

                    popup.show();
                }
            });

            final ImageButton repeatButton = index.get(R.id.workout_repeat);
            repeatButton.setFocusable(false);
            repeatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Program program = workout.program.get();

                        WorkoutActivity.start(getActivity(), program);
                    } catch (LookupException programAlreadyDeleted) {
                    }
                }
            });

            TextView nameView = index.get(R.id.workout_name);
            nameView.setText(workout.name.get());

            TextView countsView = index.get(R.id.workout_counts);
            String counts = TextUtils.join(", ", new String[]{
                    String.format(getString(R.string.duration_minutes), workout.duration.get() / 60),
                    String.format(getString(R.string.distance_meters), workout.distance.get()),
                    String.format(getString(R.string.strokes_count), workout.strokes.get())
            });
            countsView.setText(counts);

            TextView caloriesView = index.get(R.id.workout_calories);
            caloriesView.setText(String.format(getString(R.string.energy_calories), workout.energy.get()));
        }

        @Override
        protected void onItem(Workout workout, int position) {
            startActivity(SnapshotsActivity.createIntent(getActivity(), workout));
        }
    }
}
