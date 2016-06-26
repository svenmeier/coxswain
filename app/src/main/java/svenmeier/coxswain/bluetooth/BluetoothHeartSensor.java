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
 * {@link HeartSensor} using a sensor connected via Bluetooth.
 */
public class BluetoothHeartSensor extends HeartSensor {

	private static final int SCAN_TIMEOUT_MILLIS = 10000;

	private static final long PULSE_TIMEOUT_MILLIS = 5000;

	private static final UUID SERVICE_HEART_RATE = uuid(0x180D);

	private static final UUID CHARACTERISTIC_HEART_RATE = uuid(0x2A37);

	private static final UUID CLIENT_CHARACTERISTIC_DESCIPRTOR = uuid(0x2902);

	private Handler handler = new Handler();

	private Connection connection;

	private long lastPulse;

	private int heartRate = -1;

	public BluetoothHeartSensor(Context context, Snapshot memory) {
		super(context, memory);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			toast(context.getString(R.string.bluetooth_sensor_no_bluetooth));
			return;
		}

		connection = new LocationServices();
		connection.open();
	}

	@Override
	public void destroy() {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	@Override
	public void pulse() {
		if (heartRate == -1) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastPulse > PULSE_TIMEOUT_MILLIS) {
			heartRate = 0;
		}
		lastPulse = now;

		memory.pulse.set(heartRate);
	}

	private void toast(String text) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}

	private interface Connection {

		void open();

		void close();
	}

	private class LocationServices extends BroadcastReceiver implements Connection {

		private boolean registered;

		public void open() {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (isEnabled() == false) {
					toast(context.getString(R.string.bluetooth_sensor_no_location));

					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);

					IntentFilter filter = new IntentFilter();
					filter.addAction(LocationManager.MODE_CHANGED_ACTION);
					context.registerReceiver(this, filter);
					registered = true;

					return;
				}
			}

			proceed();
		}

		private boolean isEnabled() {
			LocationManager manager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setCostAllowed(true);
			criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

			String provider = manager.getBestProvider(criteria, true);

			return provider != null && LocationManager.PASSIVE_PROVIDER.equals(provider) == false;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (connection != this) {
				return;
			}

			if (isEnabled()) {
				proceed();
			}
		}

		private void proceed() {
			close();

			connection = new Bluetooth();
			connection.open();
		}

		@Override
		public void close() {
			if (registered) {
				context.unregisterReceiver(this);
				registered = false;
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class Bluetooth extends BroadcastReceiver implements Connection {

		private BluetoothAdapter adapter;

		private boolean registered;

		@Override
		public void open() {
			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
			adapter = manager.getAdapter();

			if (adapter == null) {
				toast(context.getString(R.string.bluetooth_sensor_no_bluetooth));
				return;
			}

			if (adapter.isEnabled() == false) {
				IntentFilter filter = new IntentFilter();
				filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
				filter.addAction(LocationManager.MODE_CHANGED_ACTION);
				context.registerReceiver(this, filter);
				registered = true;

				adapter.enable();
				return;
			}

			proceed();
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (connection != this) {
				return;
			}

			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
			if (state == BluetoothAdapter.STATE_ON) {
				proceed();
			}
		}

		private void proceed() {
			close();

			connection = new GattConnection();
			connection.open();
		}

		@Override
		public void close() {
			if (registered) {
				context.unregisterReceiver(this);
				registered = false;
			}

			adapter = null;
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class GattConnection extends PermissionBlock implements BluetoothAdapter.LeScanCallback, Runnable, Connection {

		private BluetoothAdapter adapter;

		private BluetoothGatt gatt;

		public GattConnection() {
			super(context);
		}

		@Override
		public void open() {
			acquirePermissions(Manifest.permission.ACCESS_FINE_LOCATION);
		}

		@Override
		protected void onPermissionsApproved() {

			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
			adapter = manager.getAdapter();

			if (adapter.startLeScan(this) == false) {
				toast(context.getString(R.string.bluetooth_sensor_no_bluetooth_le));

				close();
				return;
			}

			handler.postDelayed(this, SCAN_TIMEOUT_MILLIS);
		}

		public void close() {
			abortPermissions();

			if (adapter != null) {
				adapter.stopLeScan(this);
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
				// no more discovery
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
					adapter.stopLeScan(this);
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
	}

	private static final UUID uuid(int id) {
		return UUID.fromString(String.format("%08X", id) + "-0000-1000-8000-00805f9b34fb");
	}
}