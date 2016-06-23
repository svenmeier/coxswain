package svenmeier.coxswain.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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

	private static final int GATT_SERVICE_HEART_RATE = 0x180D;

	private static final int GATT_CHARACTERISTICS_HEART_RATE = 0x2A37;

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

			adapter.startLeScan(this);
			handler.postDelayed(this, 10000);
		}

		private void unavailable() {
			Toast.makeText(context, R.string.bluetooth_sensor_not_available, Toast.LENGTH_LONG).show();
		}

		public void close() {
			cancel();

			if (gatt != null) {
				gatt.close();
				gatt = null;
			}

			if (adapter != null) {
				adapter.stopLeScan(this);

				if (disableBlootoothOnClose) {
					adapter.disable();
				}

				adapter = null;
			}
		}

		@Override
		public void onLeScan(BluetoothDevice device, int i, byte[] bytes) {
			device.connectGatt(context, false, new BluetoothGattCallback() {
				@Override
				public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
					if (newState == BluetoothProfile.STATE_CONNECTED) {
						gatt.discoverServices();
					}
				}

				@Override
				public void onServicesDiscovered(BluetoothGatt candidate, int status) {
					for (BluetoothGattService service : candidate.getServices()) {
						if (getGattId(service.getUuid()) == GATT_SERVICE_HEART_RATE) {
							for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
								if (getGattId(characteristic.getUuid()) == GATT_CHARACTERISTICS_HEART_RATE) {
									final int properties = characteristic.getProperties();
									if ((properties | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
										gatt = candidate;
										gatt.setCharacteristicNotification(characteristic, true);

										adapter.stopLeScan(Connection.this);
										return;
									}
								}
							}
						}
					}

					candidate.close();
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
			});
		}

		@Override
		public void run() {
			if (adapter != null) {
				unavailable();

				close();
			}
		}
	}

	private static int getGattId(UUID uuid) {
		return (int) ((uuid.getMostSignificantBits() & 0x0000FFFF00000000L) >> 32);
	}
}
