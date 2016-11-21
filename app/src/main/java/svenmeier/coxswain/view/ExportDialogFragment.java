package svenmeier.coxswain.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import propoid.db.Reference;
import svenmeier.coxswain.Export;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.calendar.CalendarExport;
import svenmeier.coxswain.garmin.TcxExport;
import svenmeier.coxswain.google.FitExport;
import svenmeier.coxswain.gym.Workout;

public class ExportDialogFragment extends DialogFragment {

	private Export export;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Workout workout = Gym.instance(getActivity()).get(Reference.<Workout>from(getArguments()));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		String[] exports = new String[]{getString(R.string.calendar_export), getString(R.string.garmin_export), getString(R.string.garmin_export_share), getString(R.string.googlefit_export)};

		builder.setTitle(R.string.action_share)
				.setItems(exports, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								export = new CalendarExport(getActivity());
								break;
							case 1:
								export = new TcxExport(getActivity(), false);
								break;
							case 2:
								export = new TcxExport(getActivity(), true);
								break;
							case 3:
								export = new FitExport(getActivity());
								break;
							default:
								throw new IndexOutOfBoundsException();
						}

						export.start(workout);
					}
				});


		return builder.create();
	}

	public static ExportDialogFragment create(Workout workout) {
		ExportDialogFragment fragment = new ExportDialogFragment();

		fragment.setArguments(new Reference<>(workout).to(new Bundle()));

		return fragment;
	}
}
