package svenmeier.coxswain.view.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.AttributeSet;

import svenmeier.coxswain.R;

/**
 * An specialization that substitutes the current ringtone into the summary (as ListPreference does it
 * too).
 * Additionally the support library doesn't support it yet :/.
 */
public class RingtonePreference extends ResultPreference {

	private String defaultValue;

	public RingtonePreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {

		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.Preference);

		this.defaultValue = array.getString(R.styleable.Preference_android_defaultValue);
	}

	public RingtonePreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs);
	}

	public RingtonePreference(Context context) {
		super(context);
	}

	@Override
	public Intent getRequest() {
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Uri.parse(defaultValue));

		String existingValue = getPersistedString(null);
		if (existingValue != null) {
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
		}

		return intent;
	}

	@Override
	public void onResult(Intent intent) {
		Uri ringtone = null;
		if (intent != null) {
			ringtone = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
		}

		String value = null;
		if (ringtone != null) {
			value = ringtone.toString();
		}

		persistString(value);
		notifyChanged();
	}

	@Override
	public CharSequence getSummary() {
		CharSequence summary = super.getSummary();
		if (summary == null) {
			return summary;
		}

		String string = getPersistedString(null);

		if (string == null || string.isEmpty()) {
			string = getContext().getString(R.string.preference_audio_none);
		} else {
			Ringtone ringtone = RingtoneManager.getRingtone(getContext(), Uri.parse(string));
			if (ringtone != null) {
				string = ringtone.getTitle(getContext());
			}
		}

		return String.format(summary.toString(), string);
	}
}