package svenmeier.coxswain;

import android.app.Application;
import android.os.Build;

import androidx.preference.PreferenceManager;

/**
 */
public class Coxswain extends Application {

	public static String TAG = "coxswain";

	private Gym gym;

	@Override
	public void onCreate() {
		super.onCreate();

		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		gym = Gym.instance(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			CompactService.setup(this.getApplicationContext());
		}
	}
}
