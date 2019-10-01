package svenmeier.coxswain;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Constructor;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.Rower;

/**
 */
public class Heart {

	private static final long TIMEOUT_MILLIS = 5000;

	private final Handler handler = new Handler();
	
	protected final Context context;

	private final Measurement measurement;

	protected final Callback callback;

	public Heart(Context context, Measurement measurement, Callback callback) {
		this.context = context;
		this.measurement = measurement;
		this.callback = callback;
	}

	public void destroy() {
	}

	protected void onHeartRate(int heartRate) {
		measurement.pulse = heartRate;

		handler.post(new Runnable() {
			@Override
			public void run() {
				callback.onMeasurement();
			}
		});
	}

	public static Heart create(Context context, Rower rower, Callback callback) {
		Preference<String> sensors = Preference.getString(context, R.string.preference_hardware_heart_sensor);

		String name = sensors.get();
		try {
			Constructor<?> constructor = Class.forName(name).getConstructor(Context.class, Measurement.class, Callback.class);
			return (Heart) constructor.newInstance(context, rower, callback);
		} catch (Exception ex) {
			Log.e(Coxswain.TAG, "cannot create sensor " + name);
			return new Heart(context, rower, callback);
		}
	}

	public interface Callback {
		void onMeasurement();
	}
}
