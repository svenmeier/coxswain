package svenmeier.coxswain.view.preference;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

/**
 * An specialization that substitutes the current text into the summary (as ListPreference does it
 * too).
 */
public class EditTextPreference extends android.preference.EditTextPreference {

	public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public EditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditTextPreference(Context context) {
		super(context);
	}

	@Override
	public CharSequence getSummary() {
		CharSequence summary = super.getSummary();
		if (summary == null) {
			return summary;
		}

		return String.format(summary.toString(), getText());
	}

	@Override
	protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
		super.onAddEditTextToDialogView(dialogView, editText);

		editText.setSelection(editText.getText().length());
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		EditText editText = getEditText();

		if (editText.getHint() != null && this.getEditText().getText().toString().isEmpty()) {
			// use hint as default
			editText.setText(editText.getHint());
		}

		super.onDialogClosed(positiveResult);
	}
}
