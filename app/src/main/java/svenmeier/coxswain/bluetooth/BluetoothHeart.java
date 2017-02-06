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
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
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

	private static final UUID SERVICE_HEART_RATE = uuid(0x180D);

	private static final UUID CHARACTERISTIC_HEART_RATE = uuid(0x2A37);

	private static final UUID CLIENT_CHARACTERISTIC_DESCIPRTOR = uuid(0x2902);

	private Handler handler = new Handler();

	private Connection connection;

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

		private Map<String, BluetoothDevice> connecting = new HashMap<>();
		private Map<String, BluetoothGatt> discovering = new HashMap<>();
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
			}

			for (BluetoothGatt gatt : discovering.values()) {
				gatt.close();
			}
			discovering.clear();

			connecting.clear();
		}

		public void close() {
			stopScan();

			if (selected != null) {
				selected.close();
				selected = null;
			}

			adapter = null;
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

			String address = device.getAddress();

			if (connecting.containsKey(address) == false) {
				Log.d(Coxswain.TAG, "bluetooth found " + address);

				connecting.put(address, device);

				device.connectGatt(context, false, this);
			}
		}

		@WorkerThread
		@Override
		public synchronized void onConnectionStateChange(BluetoothGatt candidate, int status, int newState) {
			if (adapter == null) {
				// no more discovery
				return;
			}

			if (selected != null) {
				if (newState == BluetoothProfile.STATE_DISCONNECTED
						&& selected.getDevice().getAddress().equals(candidate.getDevice().getAddress())) {
					// link loss?
					handler.post(new Runnable() {
						@Override
						public void run() {
							toast(context.getString(R.string.bluetooth_heart_link_loss));
						}
					});
				}
				// already selected
				return;
			}

			String address = candidate.getDevice().getAddress();

			if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED
					&& discovering.containsKey(address) == false) {
				Log.d(Coxswain.TAG, "bluetooth gatt connected " + address);

				discovering.put(address, candidate);

				candidate.discoverServices();
			}
		}

		@WorkerThread
		@Override
		public synchronized void onServicesDiscovered(BluetoothGatt candidate, int status) {
			if (adapter == null || selected != null) {
				// no more discovery or alreaddy selected
				return;
			}

			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.d(Coxswain.TAG, "bluetooth services discovered " + candidate.getDevice().getName());

				BluetoothGattService service = candidate.getService(SERVICE_HEART_RATE);
				if (service != null) {
					Log.d(Coxswain.TAG, "bluetooth heart rate service acquired");

					BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_HEART_RATE);
					if (characteristic != null) {
						Log.d(Coxswain.TAG, "bluetooth heart rate characteristic acquired");

						if (enableNotification(candidate, characteristic)) {
							selected = candidate;
							discovering.remove(candidate.getDevice().getAddress());

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
			boolean info;

			info = candidate.setCharacteristicNotification(characteristic, true);
			Log.d(Coxswain.TAG, "bluetooth characteristic notification set " + info);

			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_DESCIPRTOR);
			if (descriptor == null) {
				return false;
			} else {
				Log.d(Coxswain.TAG, "bluetooth characteristic descriptor acquired");

				info = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				Log.d(Coxswain.TAG, "bluetooth descriptor notification set " + info);

				info = candidate.writeDescriptor(descriptor);
				Log.d(Coxswain.TAG, "bluetooth descriptor written " + info);

				return true;
			}
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

			onHeartRate(characteristic.getIntValue(format, 1));
		}
	}

	public static final UUID uuid(int id) {
		return UUID.fromString(String.format("%08X", id) + "-0000-1000-8000-00805f9b34fb");
	}
}