package svenmeier.coxswain.view.preference;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.AttributeSet;

import svenmeier.coxswain.SettingsActivity;
import svenmeier.coxswain.motivator.DefaultMotivator;

/**
 * An specialization that substitutes the current ringtone into the summary (as ListPreference does it
 * too).
 */
public class RingtonePreference extends android.preference.RingtonePreference {

	public RingtonePreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public RingtonePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RingtonePreference(Context context) {
		super(context);
	}

	@Override
	protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
		super.onPrepareRingtonePickerIntent(ringtonePickerIntent);

		Uri defaultRingtone = Uri.parse(DefaultMotivator.DEFAULT_RINGTONE);
		ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultRingtone);
	}

	@Override
	protected void onSaveRingtone(Uri ringtoneUri) {
		super.onSaveRingtone(ringtoneUri);

		// update summary
		notifyChanged();
	}

	@Override
	public CharSequence getSummary() {
		CharSequence summary = super.getSummary();
		if (summary == null) {
			return summary;
		}

		String string = getPersistedString("");

		if (string != null && string.isEmpty() == false) {
			Ringtone ringtone = RingtoneManager.getRingtone(getContext(), Uri.parse(string));
			if (ringtone != null) {
				string = ringtone.getTitle(getContext());
			}
		}

		return String.format(summary.toString(), string);
	}
}
