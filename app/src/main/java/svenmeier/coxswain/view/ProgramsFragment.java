package svenmeier.coxswain.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import propoid.ui.Index;
import propoid.ui.list.GenericAdapter;
import propoid.ui.list.MatchAdapter;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.ProgramActivity;
import svenmeier.coxswain.R;
import svenmeier.coxswain.WorkoutActivity;
import svenmeier.coxswain.gym.Program;

import static java.util.concurrent.TimeUnit.SECONDS;


public class ProgramsFragment extends Fragment implements NameDialogFragment.NameHolder {

    private Gym gym;

    private ListView programsView;

    private ProgramsAdapter programsAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_programs, container, false);

        programsView = (ListView) root.findViewById(R.id.programs);
        programsAdapter = new ProgramsAdapter();
        programsAdapter.install(programsView);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        programsAdapter.load(getActivity());
    }

    @Override
    public String getName() {
        return ((Program) programsView.getAdapter().getItem(programsView.getCheckedItemPosition())).name.get();
    }

    @Override
    public void setName(String name) {
        Program program = (Program) programsView.getAdapter().getItem(programsView.getCheckedItemPosition());
        program.name.set(name);
        Gym.instance(getActivity()).mergeProgram(program);

        programsAdapter.load(getActivity());
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
            int duration = program.asDuration();
            durationTextView.setText(String.format("%d:%02d", SECONDS.toHours(duration), SECONDS.toMinutes(duration) % 60));

            SegmentsView progressView = index.get(R.id.program_segments);
            progressView.setData(new SegmentsData(program));

            final ImageButton stopButton = index.get(R.id.program_stop);
            stopButton.setFocusable(false);
            final ImageButton menuButton = index.get(R.id.program_menu);
            menuButton.setFocusable(false);

            if (gym.isSelected(program)) {
                stopButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        gym.select(null);

                        load(getActivity());
                    }
                });
                stopButton.setVisibility(View.VISIBLE);
                menuButton.setVisibility(View.GONE);
            } else {
                menuButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(getActivity(), menuButton);
                        popup.getMenuInflater().inflate(R.menu.menu_programs_item, popup.getMenu());

                        programsView.setItemChecked(position, true);

                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.action_new:
                                        Gym.instance(getActivity()).mergeProgram(new Program("Program"));

                                        load(getActivity());

                                        return true;
                                    case R.id.action_rename:
                                        new NameDialogFragment().show(getChildFragmentManager(), "name");
                                        return true;
                                    case R.id.action_edit:
                                        startActivity(ProgramActivity.createIntent(getActivity(), program));
                                        return true;
                                    case R.id.action_delete:
                                        Gym.instance(getActivity()).deleteProgram(program);

                                        load(getActivity());

                                        return true;
                                    default:
                                        return false;
                                }
                            }
                        });

                        popup.show();
                    }
                });
                menuButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
            }
            stopButton.setFocusable(false);
        }

        @Override
        protected void onItem(Program program, int position) {
            if (gym.isSelected(program) == false) {
                gym.select(program);
            }

            startActivity(new Intent(getActivity(), WorkoutActivity.class));
        }
    }
}
