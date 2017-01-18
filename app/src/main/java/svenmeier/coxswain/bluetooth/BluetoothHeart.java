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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Heart;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.util.PermissionBlock;

/**
 * {@link Heart} reading from a connected Bluetooth device.
 */
public class BluetoothHeart extends Heart {

	private static final int SCAN_TIMEOUT_MILLIS = 60000;

	private static final long PULSE_TIMEOUT_MILLIS = 5000;

	private static final UUID SERVICE_HEART_RATE = uuid(0x180D);

	private static final UUID CHARACTERISTIC_HEART_RATE = uuid(0x2A37);

	private static final UUID CLIENT_CHARACTERISTIC_DESCIPRTOR = uuid(0x2902);

	private Handler handler = new Handler();

	private Connection connection;

	private long lastPulse;

	private int heartRate = -1;

	public BluetoothHeart(Context context, Measurement measurement) {
		super(context, measurement);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			toast(context.getString(R.string.bluetooth_heart_no_bluetooth));
			return;
		}

		connection = new Permissions(context);
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

		measurement.pulse = heartRate;
	}

	private void toast(String text) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}

	private interface Connection {

		void open();

		void close();
	}

	private class Permissions extends PermissionBlock implements Connection {

		public Permissions(Context context) {
			super(context);
		}

		@Override
		public void open() {
			acquirePermissions(Manifest.permission.ACCESS_FINE_LOCATION);
		}

		@Override
		public void close() {
			abortPermissions();
		}

		@Override
		protected void onPermissionsApproved() {
			connection = new LocationServices();
			connection.open();
		}
	}

	private class LocationServices extends BroadcastReceiver implements Connection {

		private boolean registered;

		public void open() {
			if (isRequired()) {
				if (isEnabled() == false) {
					toast(context.getString(R.string.bluetooth_heart_no_location));

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

		/**
		 * Location services must be enabled for Apps built for M and running on M or later.
		 */
		private boolean isRequired() {
			boolean builtForM = context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.M;
			boolean runningOnM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

			return builtForM && runningOnM;
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
				toast(context.getString(R.string.bluetooth_heart_no_bluetooth));
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
	private class GattConnection extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback, Runnable, Connection {

		private BluetoothAdapter adapter;

		private List<BluetoothGatt> pending = new ArrayList<>();
		private BluetoothGatt selected;

		@Override
		public void open() {
			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
			adapter = manager.getAdapter();

			if (adapter.startLeScan(this) == false) {
				toast(context.getString(R.string.bluetooth_heart_no_bluetooth_le));

				close();
				return;
			}

			toast(context.getString(R.string.bluetooth_heart_searching));
			handler.postDelayed(this, SCAN_TIMEOUT_MILLIS);
		}

		@MainThread
		@SuppressWarnings("deprecation")
		private void stopScan() {
			if (adapter != null) {
				adapter.stopLeScan(this);
				adapter = null;
			}

			for (BluetoothGatt gatt : pending) {
				gatt.close();
			}
			pending.clear();
		}

		public void close() {
			stopScan();

			if (selected != null) {
				selected.close();
				selected = null;
			}
		}

		/**
		 * Timeout, see {@link #SCAN_TIMEOUT_MILLIS}
		 */
		@MainThread
		@Override
		public void run() {
			// still scanning?
			if (adapter != null) {
				if (selected == null) {
					// nothing found
					toast(context.getString(R.string.bluetooth_heart_not_found));

					stopScan();
				}
			}
		}

		@WorkerThread
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			if (selected != null) {
				// no more discovery
				return;
			}

			device.connectGatt(context, false, this);
		}

		@WorkerThread
		@Override
		public synchronized void onConnectionStateChange(BluetoothGatt candidate, int status, int newState) {
			if (adapter == null || selected != null) {
				// no more discovery
				return;
			}

			if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
				pending.add(candidate);

				candidate.discoverServices();
			}
		}

		@WorkerThread
		@Override
		public synchronized void onServicesDiscovered(BluetoothGatt candidate, int status) {
			if (adapter == null || selected != null) {
				// no more discovery
				return;
			}

			if (status == BluetoothGatt.GATT_SUCCESS) {
				BluetoothGattService service = candidate.getService(SERVICE_HEART_RATE);
				if (service != null) {
					BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_HEART_RATE);
					if (characteristic != null) {
						if (enableNotification(candidate, characteristic)) {
							selected = candidate;
							pending.remove(candidate);

							handler.post(new Runnable() {
								@Override
								public void run() {
									toast(context.getString(R.string.bluetooth_heart_reading));

									stopScan();
								}
							});
							return;
						}
					}
				}
			}
		}

		private boolean enableNotification(BluetoothGatt candidate, BluetoothGattCharacteristic characteristic) {
			if (candidate.setCharacteristicNotification(characteristic, true) == false) {
				Log.d(Coxswain.TAG, "setCharacteristicNotification returns false - continuing");
			}

			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_DESCIPRTOR);
			if (descriptor == null) {
				Log.d(Coxswain.TAG, "get descriptor CLIENT_CHARACTERISTIC_DESCIPRTOR returns null - aborting");
				return false;
			}

			if (descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == false) {
				Log.d(Coxswain.TAG, "set ENABLE_NOTIFICATION_VALUE returns false - continuing");
			}

			if (candidate.writeDescriptor(descriptor) == false) {
				Log.d(Coxswain.TAG, "write descriptor returns false - continuing");
			}

			return true;
		}

		@Override
		@WorkerThread
		public synchronized void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			int format;
			if ((characteristic.getProperties() & 0x01) == 0) {
				format = BluetoothGattCharacteristic.FORMAT_UINT8;
			} else {
				format = BluetoothGattCharacteristic.FORMAT_UINT16;
			}

			heartRate = characteristic.getIntValue(format, 1);
		}
	}

	public static final UUID uuid(int id) {
		return UUID.fromString(String.format("%08X", id) + "-0000-1000-8000-00805f9b34fb");
	}
}