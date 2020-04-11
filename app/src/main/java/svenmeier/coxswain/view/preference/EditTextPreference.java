package svenmeier.coxswain.view.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;

/**
 * An specialization that substitutes the current text into the summary (as ListPreference does it
 * too).
 */
public class EditTextPreference extends androidx.preference.EditTextPreference {

	public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		init(context, attrs);
	}

	public EditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs);
	}

	public EditTextPreference(Context context) {
		super(context);
	}

	private void init(Context context, AttributeSet attrs) {
		try {
			for (int i=0;i<attrs.getAttributeCount();i++) {
				String name = attrs.getAttributeName(i);
				if ("defaultValue".equals(name)) {
					String value = attrs.getAttributeValue(i);
					if (value != null && value.matches("[0-9]+")) {
						setOnPreferenceChangeListener(new NumberOnly());
						break;
					}
				}
			}
		} catch (Exception exception) {
		}
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
	protected boolean persistString(String value) {
		boolean persisted = super.persistString(value);

		notifyChanged();

		return persisted;
	}

	private static class NumberOnly implements Preference.OnPreferenceChangeListener {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				Integer.parseInt(newValue.toString());
			} catch (NumberFormatException ex) {
				return false;
			}

			return true;
		}
	}
}
