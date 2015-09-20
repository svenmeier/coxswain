package svenmeier.coxswain.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import propoid.ui.list.GenericAdapter;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;

import static java.util.concurrent.TimeUnit.SECONDS;


public class WorkoutsFragment extends Fragment {

    private Gym gym;

    private ListView workoutsView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_workouts, container, false);

        workoutsView = (ListView) root.findViewById(R.id.workouts);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        workoutsView.setAdapter(new WorkoutsAdapter());
    }

    private class WorkoutsAdapter extends GenericAdapter<Workout> {

        private DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);

        public WorkoutsAdapter() {
            super(R.layout.layout_workouts_item, Gym.instance(getActivity()).getWorkouts());
        }

        @Override
        protected void bind(int position, View view, Workout workout) {
            TextView nameView = (TextView) view.findViewById(R.id.workout_name);
            nameView.setText(workout.name.get());

            TextView startView = (TextView) view.findViewById(R.id.workout_start);
            startView.setText(format.format(new Date(workout.start.get())));

            TextView countsView = (TextView) view.findViewById(R.id.workout_counts);
            String counts = TextUtils.join(", ", new String[]{
                    String.format(getString(R.string.duration_minutes), workout.duration.get() / 60),
                    String.format(getString(R.string.distance_meters), workout.distance.get()),
                    String.format(getString(R.string.strokes_count), workout.strokes.get())
                    });
            countsView.setText(counts);
        }
    }
}
