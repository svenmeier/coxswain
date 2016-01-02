package svenmeier.coxswain;

import android.preference.PreferenceManager;

/**
 */
public class Application extends android.app.Application {

	public static String TAG = "coxswain";

	private Gym gym;

	@Override
	public void onCreate() {
		super.onCreate();

		PreferenceManager.setDefaultValues(Application.this, R.xml.preferences, true);

		gym = Gym.instance(this);

		new Initializer();
	}

	private class Initializer implements Runnable {

		public Initializer() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			gym.defaultPrograms();
		}
	}
}
