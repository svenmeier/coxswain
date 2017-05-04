package svenmeier.coxswain;

import android.app.Application;
import android.support.v7.preference.PreferenceManager;

import propoid.util.content.Preference;

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

		new Initializer();
	}

	private class Initializer implements Runnable {

		public Initializer() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			gym.defaults();
		}
	}
}
