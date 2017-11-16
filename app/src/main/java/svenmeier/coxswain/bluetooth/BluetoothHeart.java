package svenmeier.coxswain.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;

import propoid.util.content.Preference;
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

	private Handler handler = new Handler();

	private Preference<String> preferredDevice;

	private Connection connection;

	public BluetoothHeart(Context context, Measurement measurement) {
		super(context, measurement);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			toast(context.getString(R.string.bluetooth_heart_no_bluetooth));
			return;
		}

		preferredDevice = Preference.getString(context, R.string.preference_bluetooth_preferred);

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
	private class GattConnection extends GattScanner implements Runnable, Connection {

		private final UUID SERVICE_HEART_RATE = GattScanner.uuid(0x180D);
		private final UUID CHARACTERISTIC_HEART_RATE = GattScanner.uuid(0x2A37);

		private BluetoothGatt selected;

		GattConnection() {
			super(context, preferredDevice.get());
		}

		@Override
		public void open() {
			if (start() == false) {
				toast(context.getString(R.string.bluetooth_heart_no_bluetooth_le));

				close();
				return;
			}

			toast(context.getString(R.string.bluetooth_heart_searching));
			handler.removeCallbacks(this);
			handler.postDelayed(this, SCAN_TIMEOUT_MILLIS);
		}

		public void close() {
			stop();
		}

		/**
		 * Timeout, see {@link #SCAN_TIMEOUT_MILLIS}
		 */
		@Override
		public void run() {
			if (connection == GattConnection.this && selected == null) {
				// nothing found, clear preferred
				preferredDevice.set(null);

				toast(context.getString(R.string.bluetooth_heart_not_found));
				close();
			}
		}

		@Override
		protected void onDiscovered(BluetoothGatt candidate) {
			BluetoothGattService service = candidate.getService(SERVICE_HEART_RATE);
			if (service != null) {
				Log.d(Coxswain.TAG, "bluetooth heart rate service acquired");

				BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_HEART_RATE);
				if (characteristic != null) {
					Log.d(Coxswain.TAG, "bluetooth heart rate characteristic acquired");

					if (enableNotification(candidate, characteristic)) {
						selected = candidate;

						stop();

						handler.post(new Runnable() {
							@Override
							public void run() {
								toast(context.getString(R.string.bluetooth_heart_reading));
							}
						});

						preferredDevice.set(candidate.getDevice().getAddress());

						return;
					}
				}
			}
		}

		@Override
		protected void onLost(BluetoothGatt candidate) {
			if (selected != null && selected.getDevice().getAddress().equals(candidate.getDevice().getAddress())) {
				selected = null;

				handler.post(new Runnable() {
					@Override
					public void run() {
						toast(context.getString(R.string.bluetooth_heart_link_loss));

						if (connection == GattConnection.this) {
							open();
						}
					}
				});
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

			final String data = Arrays.toString(characteristic.getValue());
			Log.d(Coxswain.TAG, "bluetooth characteristic changed " + data);
			handler.post(new Runnable() {
				@Override
				public void run() {
					toast("changed: " + data);
				}
			});


			onHeartRate(characteristic.getIntValue(format, 1));
		}
	}
}