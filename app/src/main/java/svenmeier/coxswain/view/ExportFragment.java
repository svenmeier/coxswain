package svenmeier.coxswain.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import propoid.db.Reference;
import svenmeier.coxswain.Export;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.garmin.TcxExport;
import svenmeier.coxswain.google.FitExport;
import svenmeier.coxswain.gym.Workout;

public class ExportFragment extends DialogFragment {

	public static final int FIT_REQUEST_CODE = 1;

	private Export export;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Workout workout = Gym.instance(getActivity()).getWorkout(Reference.<Workout>from(getArguments()));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		String[] exports = new String[]{getString(R.string.export_tcx), getString(R.string.export_fit)};

		builder.setTitle(R.string.action_share)
				.setItems(exports, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								export = new TcxExport(getActivity());
								break;
							case 1:
								export = new FitExport(getActivity(), FIT_REQUEST_CODE);
								break;
							default:
								throw new IndexOutOfBoundsException();
						}

						export.start(workout);
					}
				});


		return builder.create();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (export != null && requestCode == FIT_REQUEST_CODE) {
			export.onResult(resultCode);
		}
	}


	public static ExportFragment create(Workout workout) {
		ExportFragment fragment = new ExportFragment();

		fragment.setArguments(new Reference<>(workout).to(new Bundle()));

		return fragment;
	}
}
