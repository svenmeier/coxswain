package svenmeier.coxswain;

import android.content.Context;
import android.util.Log;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.Rower;

/**
 */
public class Heart {

	protected final Context context;

	protected final Rower rower;

	protected Heart(Context context, Rower rower) {
		this.context = context;
		this.rower = rower;
	}

	public void destroy() {
	}

	public void pulse() {
	}

	public static Heart create(Context context, Rower rower) {
		Preference<String> sensors = Preference.getString(context, R.string.preference_hardware_heart_sensor);

		String name = sensors.get();
		try {
			return (Heart) Class.forName(name).getConstructor(Context.class, Snapshot.class).newInstance(context, rower);
		} catch (Exception ex) {
			Log.e(Coxswain.TAG, "cannot create sensor " + name);
			return new Heart(context, rower);
		}
	}
}
