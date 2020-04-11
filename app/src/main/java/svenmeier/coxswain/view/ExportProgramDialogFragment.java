package svenmeier.coxswain.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import propoid.db.Reference;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.io.Export;
import svenmeier.coxswain.io.ProgramExport;

public class ExportProgramDialogFragment extends DialogFragment {

	private Export<Program> export;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Program program = Gym.instance(getActivity()).get(Reference.<Program>from(getArguments()));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		String[] exports = new String[]{getString(R.string.program_export), getString(R.string.program_export_share)};

		builder.setTitle(R.string.action_export)
				.setItems(exports, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								export = new ProgramExport(getActivity(), false);
								break;
							case 1:
								export = new ProgramExport(getActivity(), true);
								break;
							default:
								throw new IndexOutOfBoundsException();
						}

						export.start(program, false);
					}
				});


		return builder.create();
	}

	public static ExportProgramDialogFragment create(Program program) {
		ExportProgramDialogFragment fragment = new ExportProgramDialogFragment();

		fragment.setArguments(new Reference<>(program).to(new Bundle()));

		return fragment;
	}
}
