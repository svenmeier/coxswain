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
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import java.util.UUID;

import svenmeier.coxswain.HeartSensor;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.util.PermissionBlock;

/**
 */
public class BluetoothHeartSensor extends HeartSensor {

	private static final UUID SERVICE_HEART_RATE = uuid(0x180D);

	private static final UUID CHARACTERISTIC_HEART_RATE = uuid(0x2A37);

	private static final UUID CLIENT_CHARACTERISTIC_DESCIPRTOR = uuid(0x2902);

	private final Context context;

	private Handler handler = new Handler();

	private final Snapshot memory;

	private Connection connection;

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
		memory.pulse.set(heartRate);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class Connection extends PermissionBlock implements BluetoothAdapter.LeScanCallback, Runnable {

		private BluetoothAdapter adapter;

		private BluetoothGatt gatt;

		private boolean disableBlootoothOnClose;

		public Connection() {
			super(context);

			acquire(Manifest.permission.ACCESS_FINE_LOCATION);
		}

		@Override
		protected void onApproved() {
			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

			adapter = manager.getAdapter();

			if (adapter == null) {
				unavailable();
				return;
			}

			if (adapter.isEnabled() == false) {
				if (adapter.enable()) {
					this.disableBlootoothOnClose = true;
				} else {
					adapter = null;
					unavailable();
					return;
				}
			}

			if (adapter.startLeScan(this) == false) {
				adapter = null;
				unavailable();
				return;
			}

//			handler.postDelayed(this, 10000);
		}

		private void unavailable() {
			Toast.makeText(context, R.string.bluetooth_sensor_not_available, Toast.LENGTH_LONG).show();
		}

		private void connected() {
			Toast.makeText(context, R.string.bluetooth_sensor_connected, Toast.LENGTH_LONG).show();
		}

		public void close() {
			release();

			if (adapter != null) {
				// no more discovery
				adapter.stopLeScan(this);
				adapter = null;
			}

			if (gatt != null) {
				gatt.close();
				gatt = null;
			}

			if (disableBlootoothOnClose) {
				adapter.disable();
				disableBlootoothOnClose = false;
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
					unavailable();

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
									connected();
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
