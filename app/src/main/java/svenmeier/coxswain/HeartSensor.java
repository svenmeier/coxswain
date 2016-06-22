package svenmeier.coxswain;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Snapshot;

/**
 */
public abstract class HeartSensor {

	public abstract HeartSensor destroy();

	public abstract void pulse();

	public static class None extends HeartSensor {

		public None (Context context, Snapshot memory) {
		}

		@Override
		public HeartSensor destroy() {
			return this;
		}

		@Override
		public void pulse() {
		}
	}

	public static HeartSensor create(Context context, Snapshot snapshot) {
		Preference<String> sensors = Preference.getString(context, R.string.preference_hardware_heart_sensor);

		String name = sensors.get();
		try {
			return (HeartSensor) Class.forName(name).getConstructor(Context.class, Snapshot.class).newInstance(context, snapshot);
		} catch (Exception ex) {
			Log.e(Coxswain.TAG, "cannot create sensor " + name);
			return new None(context, snapshot);
		}
	}
}
