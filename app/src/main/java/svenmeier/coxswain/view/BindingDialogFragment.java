package svenmeier.coxswain.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.Arrays;
import java.util.List;

import svenmeier.coxswain.R;

public class BindingDialogFragment extends DialogFragment {

	public static final String VIEW_ID = "id";

	public static final String BINDING = "binding";

	private static final List<ValueBinding> bindings = Arrays.asList(
			ValueBinding.DISTANCE,
			ValueBinding.DURATION,
			ValueBinding.STROKES,
			ValueBinding.ENERGY,
			ValueBinding.SPEED,
			ValueBinding.PULSE,
			ValueBinding.STROKE_RATE,
			ValueBinding.STROKE_RATIO,
			ValueBinding.TIME);

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final int viewId = getArguments().getInt(VIEW_ID);
		final ValueBinding binding = (ValueBinding) getArguments().getSerializable(BINDING);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final String[] items = new String[bindings.size()];
		for (int b = 0; b < bindings.size(); b++) {
			items[b] = getString(bindings.get(b).label);
		}

		builder.setTitle(R.string.action_bind)
				.setSingleChoiceItems(items, bindings.indexOf(binding), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						ValueBinding binding = bindings.get(which);

						((Callback)getActivity()).onBinding(viewId, binding);

						dismiss();
					}
				});

		return builder.create();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		final int viewId = getArguments().getInt(VIEW_ID);

		if (isAdded()) {
			((Callback)getActivity()).onBinding(viewId, null);
		}

		super.onDismiss(dialog);
	}

	public static BindingDialogFragment create(int viewId, ValueBinding binding) {
		BindingDialogFragment fragment = new BindingDialogFragment();

		Bundle argumets = new Bundle();
		argumets.putInt(VIEW_ID, viewId);
		argumets.putSerializable(BINDING, binding);
		fragment.setArguments(argumets);

		return fragment;
	}

	public interface Callback {
		public void onBinding(int viewId, ValueBinding binding);
	}
}
