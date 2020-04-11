package svenmeier.coxswain.view.preference;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import androidx.preference.Preference;

import svenmeier.coxswain.R;

/**
 * A preference that needs a result from an {@link Intent}.
 */
public abstract class ResultPreference extends Preference {

	public ResultPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public ResultPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public ResultPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ResultPreference(Context context) {
		super(context);
	}

	public abstract Intent getRequest();

	public abstract void onResult(Intent intent);
}
