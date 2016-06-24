package svenmeier.coxswain.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import java.util.UUID;

import svenmeier.coxswain.HeartSensor;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.util.PermissionBlock;

/**
 */
public class BluetoothHeartSensor extends HeartSensor {

	private static final int SCAN_TIMEOUT_MILLIS = 10000;

	private static final long PULSE_TIMEOUT_MILLIS = 5000;

	private static final UUID SERVICE_HEART_RATE = uuid(0x180D);

	private static final UUID CHARACTERISTIC_HEART_RATE = uuid(0x2A37);

	private static final UUID CLIENT_CHARACTERISTIC_DESCIPRTOR = uuid(0x2902);

	private final Context context;

	private Handler handler = new Handler();

	private final Snapshot memory;

	private Connection connection;

	private long lastPulse;

	private int heartRate = 0;

	public BluetoothHeartSensor(Context context, Snapshot memory) {
		this.context = context;

		this.memory = memory;

		if (connection == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			connection = new Connection();
		}
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
		long now = System.currentTimeMillis();
		if (now - lastPulse > PULSE_TIMEOUT_MILLIS) {
			heartRate = 0;
		}
		lastPulse = now;

		memory.pulse.set(heartRate);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class Connection extends PermissionBlock implements BluetoothAdapter.LeScanCallback, Runnable {

		private BluetoothAdapter adapter;

		private BluetoothGatt gatt;

		private BluetoothEnabler enabler;

		public Connection() {
			super(context);

			acquire(Manifest.permission.ACCESS_FINE_LOCATION);
		}

		@Override
		protected void onApproved() {

			if (checkLocation() == false) {
				// starting with Android 6.0 location services have to be enabled
				// to be able to discover bluetooth devices :/

				toast(context.getString(R.string.bluetooth_sensor_no_location));
				return;
			}

			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

			adapter = manager.getAdapter();

			if (adapter == null) {
				toast(context.getString(R.string.bluetooth_sensor_no_bluetooth));
				return;
			}

			if (adapter.isEnabled() == false) {
				enabler = new BluetoothEnabler();

				return;
			}

			scan();
		}

		private void scan() {
			if (adapter.startLeScan(this) == false) {
				toast(context.getString(R.string.bluetooth_sensor_no_bluetooth));

				close();
				return;
			}

			handler.postDelayed(this, SCAN_TIMEOUT_MILLIS);
		}

		private void toast(String text) {
			Toast.makeText(context, text, Toast.LENGTH_LONG).show();
		}

		private boolean checkLocation() {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				LocationManager manager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_COARSE);
				criteria.setAltitudeRequired(false);
				criteria.setBearingRequired(false);
				criteria.setCostAllowed(true);
				criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

				String provider = manager.getBestProvider(criteria, true);
				if (provider == null || LocationManager.PASSIVE_PROVIDER.equals(provider)) {
					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);

					return false;
				}
			}

			return true;
		}

		public void close() {
			abort();

			if (adapter != null) {
				adapter.stopLeScan(this);

				if (enabler != null) {
					enabler.close();
					enabler = null;
				}

				adapter = null;
			}

			if (gatt != null) {
				gatt.close();
				gatt = null;
			}
		}

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			if (gatt != null) {
				return;
			}

			device.connectGatt(context, false, new BluetoothGattCallbackImpl());
		}

		@Override
		public void run() {
			// still scanning?
			if (adapter != null) {
				if (gatt == null) {
					toast(context.getString(R.string.bluetooth_sensor_no_heart));

					close();
				} else {
					adapter.stopLeScan(Connection.this);
					adapter = null;
				}
			}
		}

		/**
		 * The method of this class are not called on the main thread!
		 */
		private class BluetoothGattCallbackImpl extends BluetoothGattCallback {

			@Override
			public void onConnectionStateChange(BluetoothGatt candidate, int status, int newState) {
				if (adapter == null || gatt != null) {
					// no more discovery
					return;
				}

				if (newState == BluetoothProfile.STATE_CONNECTED) {
					candidate.discoverServices();
				}
			}

			@Override
			public synchronized void onServicesDiscovered(BluetoothGatt candidate, int status) {
				if (adapter == null || gatt != null) {
					// no more discovery
					return;
				}

				BluetoothGattService service = candidate.getService(SERVICE_HEART_RATE);
				if (service != null) {
					BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_HEART_RATE);
					if (characteristic != null) {
						if (enableNotification(candidate, characteristic)) {
							gatt = candidate;
							handler.post(new Runnable() {
								@Override
								public void run() {
									toast(context.getString(R.string.bluetooth_sensor_reading));
								}
							});
							return;
						}
					}
				}

				candidate.close();
			}

			private boolean enableNotification(BluetoothGatt candidate, BluetoothGattCharacteristic characteristic) {
				if (candidate.setCharacteristicNotification(characteristic, true) == false) {
					return false;
				}

				BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_DESCIPRTOR);
				if (descriptor == null) {
					return false;
				}

				if (descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == false) {
					return false;
				}

				if (candidate.writeDescriptor(descriptor) == false) {
					return false;
				}

				return true;
			}

			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
				int format;
				if ((characteristic.getProperties() & 0x01) == 0) {
					format = BluetoothGattCharacteristic.FORMAT_UINT8;
				} else {
					format = BluetoothGattCharacteristic.FORMAT_UINT16;
				}

				heartRate = characteristic.getIntValue(format, 1);
			}
		}

		private class BluetoothEnabler extends BroadcastReceiver {

			public BluetoothEnabler() {
				IntentFilter filter = new IntentFilter();
				filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
				context.registerReceiver(this, filter);

				adapter.enable();
			}

			@Override
			public void onReceive(Context context, Intent intent) {
				if (adapter == null) {
					return;
				}

				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
				if (state == BluetoothAdapter.STATE_ON) {
					scan();
				}
			}

			public void close() {
				adapter.disable();

				context.unregisterReceiver(this);
			}
		}
	}

	private static final UUID uuid(int id) {
		return UUID.fromString(String.format("%08X", id) + "-0000-1000-8000-00805f9b34fb");
	}
}
