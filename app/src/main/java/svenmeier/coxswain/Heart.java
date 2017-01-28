package svenmeier.coxswain;

import android.content.Context;
import android.util.Log;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.Rower;

/**
 */
public class Heart {

	private static final long TIMEOUT_MILLIS = 5000;

	protected final Context context;

	private final Measurement measurement;

	private long heartRateTime;

	private int heartRate = -1;

	protected Heart(Context context, Measurement measurement) {
		this.context = context;
		this.measurement = measurement;
	}

	public void destroy() {
	}

	public final void pulse() {
		if (heartRate == -1) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - heartRateTime > TIMEOUT_MILLIS) {
			heartRate = 0;
		}

		measurement.pulse = heartRate;
	}

	protected void onHeartRate(int heartRate) {
		heartRateTime = System.currentTimeMillis();
		this.heartRate = heartRate;
	}

	public static Heart create(Context context, Rower rower) {
		Preference<String> sensors = Preference.getString(context, R.string.preference_hardware_heart_sensor);

		String name = sensors.get();
		try {
			return (Heart) Class.forName(name).getConstructor(Context.class, Measurement.class).newInstance(context, rower);
		} catch (Exception ex) {
			Log.e(Coxswain.TAG, "cannot create sensor " + name);
			return new Heart(context, rower);
		}
	}
}
