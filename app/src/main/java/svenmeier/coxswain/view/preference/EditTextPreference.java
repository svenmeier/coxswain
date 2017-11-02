package svenmeier.coxswain.view.preference;

import android.content.Context;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

/**
 * An specialization that substitutes the current text into the summary (as ListPreference does it
 * too).
 */
public class EditTextPreference extends android.support.v7.preference.EditTextPreference {

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
	protected boolean persistString(String value) {
		boolean persisted = super.persistString(value);

		notifyChanged();

		return persisted;
	}

	@Override
	public CharSequence getSummary() {
		CharSequence summary = super.getSummary();
		if (summary == null) {
			return summary;
		}

		return String.format(summary.toString(), getText());
	}
}
