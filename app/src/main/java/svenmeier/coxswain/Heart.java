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
		measurement.setPulse(heartRate);

		handler.post(new Runnable() {
			@Override
			public void run() {
				callback.onMeasurement(measurement);
			}
		});
	}

	public static Heart create(Context context, Measurement measurement, Callback callback) {
		Preference<String> sensors = Preference.getString(context, R.string.preference_hardware_heart_sensor);

		String name = sensors.get();
		try {
			Constructor<?> constructor = Class.forName(name).getConstructor(Context.class, Measurement.class, Callback.class);
			return (Heart) constructor.newInstance(context, measurement, callback);
		} catch (Exception ex) {
			Log.e(Coxswain.TAG, "cannot create sensor " + name);
			return new Heart(context, measurement, callback);
		}
	}

	public interface Callback {
		void onMeasurement(Measurement measurement);
	}
}
