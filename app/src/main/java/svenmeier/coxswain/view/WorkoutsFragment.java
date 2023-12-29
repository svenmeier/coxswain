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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import propoid.core.Property;
import propoid.db.Order;
import propoid.ui.list.GenericRecyclerAdapter;
import propoid.ui.list.MatchRecyclerAdapter;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.SnapshotsActivity;
import svenmeier.coxswain.WorkoutActivity;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.rower.Distance;
import svenmeier.coxswain.rower.Duration;
import svenmeier.coxswain.rower.Energy;
import svenmeier.coxswain.rower.Stroke;

import static java.util.concurrent.TimeUnit.SECONDS;


public class WorkoutsFragment extends Fragment implements Gym.Listener {

    private Gym gym;

    private RecyclerView workoutsView;

    private WorkoutsAdapter adapter;

    private int sort = 0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        gym = Gym.instance(context);

        gym.addListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        gym.removeListener(this);

        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_workouts, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            sort = (sort + 1) % 4;

            String text = adapter.sort(sort, false);
            adapter.restartLoader(0, this);

            if (text != null) {
                Snackbar.make(getView(), text, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.action_sort_ascending, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                adapter.sort(sort, true);
                                adapter.restartLoader(0, WorkoutsFragment.this);
                            }
                        })
                        .show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_workouts, container, false);

        workoutsView = (RecyclerView) root.findViewById(R.id.workouts);
        workoutsView.setLayoutManager(new LinearLayoutManager(getContext()));
        workoutsView.setHasFixedSize(true);
        workoutsView.setAdapter(adapter = new WorkoutsAdapter());

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter.initLoader(0, this);
    }

    @Override
    public void changed(Object scope) {
        if (scope == null) {
            if (adapter != null) {
                adapter.destroy(0, this);
            }

            // must reset adapter to take into account current program
            workoutsView.setAdapter(adapter = new WorkoutsAdapter());
            adapter.initLoader(0, this);
        }
    }

    private class WorkoutsAdapter extends MatchRecyclerAdapter<Workout> {

        public WorkoutsAdapter() {
            super(R.layout.layout_workouts_item, Gym.instance(getActivity()).getWorkouts());

            sort(sort, false);
        }

        public String sort(int index, boolean ascending) {
            String text = null;

            Property by;
            switch (index) {
                case 0:
                    text = getString(R.string.sort_start);
                    by = getMatch().getPrototype().start;
                    break;
                case 1:
                    text = getString(R.string.sort_duration);
                    by = getMatch().getPrototype().duration;
                    break;
                case 2:
                    text = getString(R.string.sort_distance);
                    by = getMatch().getPrototype().distance;
                    break;
                default:
                    text = getString(R.string.sort_energy);
                    by = getMatch().getPrototype().energy;
            }

            if (ascending) {
                setOrder(Order.ascending(by));
            } else {
                setOrder(Order.descending(by));
            }

            return text;
        }

        @Override
        protected GenericHolder createHolder(View v) {
            return new WorkoutHolder(v);
        }
    }

    private class WorkoutHolder extends GenericRecyclerAdapter.GenericHolder<Workout> implements View.OnClickListener {


        private final TextView startView;
        private final TextView nameView;
        private final TextView countsView;
        private final ImageButton menuButton;

        public WorkoutHolder(View view) {
            super(view);

            view.setOnClickListener(this);

            startView = (TextView) view.findViewById(R.id.workout_start);
            nameView = (TextView) view.findViewById(R.id.workout_name);
            countsView = (TextView) view.findViewById(R.id.workout_counts);

            menuButton = (ImageButton) view.findViewById(R.id.workout_menu);
            menuButton.setFocusable(false);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), menuButton);
                    popup.getMenuInflater().inflate(R.menu.menu_workout_item, popup.getMenu());

                    popup.getMenu().findItem(R.id.action_evaluate).setChecked(item.evaluate.get());

                    popup.getMenu().findItem(R.id.action_repeat).setEnabled(item.canRepeat());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.action_delete:
                                    DeleteDialogFragment.create(item).show(getActivity().getSupportFragmentManager(), "delete");

                                    return true;
                                case R.id.action_evaluate:
                                    item.evaluate.set(!item.evaluate.get());

                                    gym.mergeWorkout(item);

                                    return true;
                                case R.id.action_export:
                                    ExportWorkoutDialogFragment.create(item).show(getActivity().getSupportFragmentManager(), "export");

                                    return true;
                                case R.id.action_repeat:
                                    gym.repeat(item);

                                    WorkoutActivity.start(getActivity());
                                    return true;
                                case R.id.action_challenge:
                                    gym.challenge(item);

                                    WorkoutActivity.start(getActivity());
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
            startView.setText(DateUtils.formatDateTime(getActivity(), item.start.get(), DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

            nameView.setText(item.programName("-"));

			String counts = TextUtils.join(", ", new String[]{
                    Duration.seconds(getActivity(), item.duration.get()).formatted(),
                    Distance.m(getActivity(), item.distance.get()).formatted(),
                    Stroke.count(getActivity(), item.strokes.get()).formatted(),
                    Energy.kcal(getActivity(), item.energy.get()).formatted()
            });
            countsView.setText(counts);
        }

        @Override
        public void onClick(View v) {
            startActivity(SnapshotsActivity.createIntent(getActivity(), item));
        }
    }
}
