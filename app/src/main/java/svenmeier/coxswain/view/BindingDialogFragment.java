package svenmeier.coxswain.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

public class BindingDialogFragment extends DialogFragment {

	public static final String VIEW_ID = "id";

	private static final ValueBinding[] bindings = new ValueBinding[]{
			ValueBinding.DISTANCE,
			ValueBinding.DURATION,
			ValueBinding.STROKES,
			ValueBinding.ENERGY,
			ValueBinding.SPEED,
			ValueBinding.PULSE,
			ValueBinding.STROKE_RATE,
			ValueBinding.STROKE_RATIO};

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final int viewId = getArguments().getInt(VIEW_ID);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final String[] items = new String[bindings.length];
		for (int b = 0; b < bindings.length; b++) {
			items[b] = getString(bindings[b].label);
		}

		builder.setTitle(R.string.action_bind)
				.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						ValueBinding binding = bindings[which];

						((Callback)getActivity()).onBinding(viewId, binding);
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

	public static BindingDialogFragment create(int viewId) {
		BindingDialogFragment fragment = new BindingDialogFragment();

		Bundle argumets = new Bundle();
		argumets.putInt(VIEW_ID, viewId);
		fragment.setArguments(argumets);

		return fragment;
	}

	public interface Callback {
		public void onBinding(int viewId, ValueBinding binding);
	}
}
