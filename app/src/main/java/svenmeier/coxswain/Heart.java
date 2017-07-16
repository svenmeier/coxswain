package svenmeier.coxswain;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.heart.generic.HeartRateListener;
import svenmeier.coxswain.heart.generic.ConnectionStatus;
import svenmeier.coxswain.heart.generic.ConnectionStatusListener;
import svenmeier.coxswain.rower.Rower;

/**
 */
public class Heart implements HeartRateListener {

	private static final long TIMEOUT_MILLIS = 5000;
	public static final int UNKNOWN_READING = -1;

    protected final Context context;

	private final Measurement measurement;

	private long heartRateTime;

	private int heartRate = -1;
	private ConnectionStatus connectionStatus;
	private List<ConnectionStatusListener> connectionStatusListeners;

	protected Heart(Context context, Measurement measurement) {
		this.context = context;
		this.measurement = measurement;
		this.connectionStatus = ConnectionStatus.INITIAL;
		this.connectionStatusListeners = new LinkedList<>();
	}

	public void destroy() {
	}

	public final void pulse() {
		if (heartRate == UNKNOWN_READING) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - heartRateTime > TIMEOUT_MILLIS) {
			heartRate = 0;
		}

		measurement.pulse = heartRate;
	}

	public void onHeartRate(int heartRate) {
		updateConnectionStatus(ConnectionStatus.CONNECTED);
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

	public int getHeartRate() {
		return heartRate;
	}

	public final ConnectionStatus getConnectionStatus() {
		return this.connectionStatus;
	}

	protected void updateConnectionStatus(final ConnectionStatus connectionStatus) {
		updateConnectionStatus(connectionStatus, null, null);
	}

	protected void updateConnectionStatus(final ConnectionStatus connectionStatus, final @Nullable String deviceName, final @Nullable String message) {
		if (this.connectionStatus != connectionStatus) {
			this.connectionStatus = connectionStatus;
			Log.i(Coxswain.TAG, "Update connection status of " + this.getClass().getSimpleName() + " to " + connectionStatus);
			for (ConnectionStatusListener listener : connectionStatusListeners) {
				listener.onConnectionStatusChange(this.getClass(), connectionStatus, deviceName, message);
			}
		}
	}

	public void registerConnectionStatusListener(final ConnectionStatusListener listener) {
		if (! connectionStatusListeners.contains(listener)) {
			connectionStatusListeners.add(listener);
		}
	}

	public void unregisterConnectionStatusListener(final ConnectionStatusListener listener) {
		connectionStatusListeners.remove(listener);
	}
}
