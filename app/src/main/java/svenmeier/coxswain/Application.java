package svenmeier.coxswain;

import android.preference.PreferenceManager;

/**
 */
public class Application extends android.app.Application {

	@Override
	public void onCreate() {
		super.onCreate();

		PreferenceManager.setDefaultValues(Application.this, R.xml.preferences, true);

		new Initializer();
	}

	private class Initializer implements Runnable {

		public Initializer() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			Gym.instance(Application.this).defaultPrograms();
		}
	}
}
