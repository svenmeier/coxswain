package svenmeier.coxswain;

import android.preference.PreferenceManager;

/**
 */
public class Application extends android.app.Application {

	@Override
	public void onCreate() {
		super.onCreate();

		new Initializer();
	}

	private class Initializer implements Runnable {

		public Initializer() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			PreferenceManager.setDefaultValues(Application.this, R.xml.preferences, true);

			Gym.instance(Application.this).defaultPrograms();
		}
	}
}
