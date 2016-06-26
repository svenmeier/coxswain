package svenmeier.coxswain;

import android.content.Context;
import android.util.Log;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Snapshot;

/**
 */
public class HeartSensor {

	protected final Context context;

	protected final Snapshot memory;

	protected HeartSensor(Context context, Snapshot memory) {
		this.context = context;
		this.memory = memory;
	}

	public void destroy() {
	}

	public void pulse() {
	}

	public static HeartSensor create(Context context, Snapshot snapshot) {
		Preference<String> sensors = Preference.getString(context, R.string.preference_hardware_heart_sensor);

		String name = sensors.get();
		try {
			return (HeartSensor) Class.forName(name).getConstructor(Context.class, Snapshot.class).newInstance(context, snapshot);
		} catch (Exception ex) {
			Log.e(Coxswain.TAG, "cannot create sensor " + name);
			return new HeartSensor(context, snapshot);
		}
	}
}
