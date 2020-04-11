package svenmeier.coxswain.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import propoid.db.Reference;
import svenmeier.coxswain.garmin.TcxShareExport;
import svenmeier.coxswain.io.Export;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.io.CalendarExport;
import svenmeier.coxswain.garmin.TcxExport;
import svenmeier.coxswain.google.FitExport;
import svenmeier.coxswain.gym.Workout;

public class ExportWorkoutDialogFragment extends DialogFragment {

	private Export<Workout> export;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Workout workout = Gym.instance(getActivity()).get(Reference.<Workout>from(getArguments()));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		String[] exports = new String[]{getString(R.string.calendar_export), getString(R.string.garmin_export), getString(R.string.garmin_export_share), getString(R.string.googlefit_export)};

		builder.setTitle(R.string.action_export)
				.setItems(exports, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								export = new CalendarExport(getActivity());
								break;
							case 1:
								export = new TcxExport(getActivity());
								break;
							case 2:
								export = new TcxShareExport(getActivity());
								break;
							case 3:
								export = new FitExport(getActivity());
								break;
							default:
								throw new IndexOutOfBoundsException();
						}

						Export.start(getActivity(), export, workout);
					}
				});


		return builder.create();
	}

	public static ExportWorkoutDialogFragment create(Workout workout) {
		ExportWorkoutDialogFragment fragment = new ExportWorkoutDialogFragment();

		fragment.setArguments(new Reference<>(workout).to(new Bundle()));

		return fragment;
	}
}
