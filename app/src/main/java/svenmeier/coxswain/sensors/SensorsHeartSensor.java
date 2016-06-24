package svenmeier.coxswain.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import svenmeier.coxswain.HeartSensor;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;

/**
 */
public class SensorsHeartSensor extends HeartSensor {

	private static final int TYPE_HEART_RATE_LEGACY = 65562;

	private final Context context;

	private final Snapshot memory;

	private Connection connection;

	private int heartRate = 0;

	public SensorsHeartSensor(Context context, Snapshot memory) {
		this.context = context;

		this.memory = memory;

		connection = new Connection();
	}

	@Override
	public HeartSensor destroy() {
		if (connection != null) {
			connection.close();
			connection = null;
		}

		return this;
	}

	@Override
	public void pulse() {
		memory.pulse.set(heartRate);
	}

	private class Connection implements SensorEventListener {

		private Sensor sensor;

		public Connection() {
			SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

			sensor = manager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
			if (sensor == null) {
				sensor = manager.getDefaultSensor(TYPE_HEART_RATE_LEGACY);
			}

			if (sensor == null) {
				Toast.makeText(context, R.string.sensors_sensor_no_heart, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, R.string.sensors_sensor_reading, Toast.LENGTH_LONG).show();
				manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}

		public void close() {
			if (sensor != null) {
				SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

				manager.unregisterListener(this);
			}
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.values != null && event.values.length > 0) {
				heartRate = Math.round(event.values[0]);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int i) {
		}
	}
}
