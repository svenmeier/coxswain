package svenmeier.coxswain.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.util.Log;

import androidx.annotation.CallSuper;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

import svenmeier.coxswain.Coxswain;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BlueWriter extends BluetoothGattCallback {

	public static final UUID SERVICE_HEART_RATE = uuid(0x180D);
	public static final UUID CHARACTERISTIC_HEART_RATE_MEASUREMENT = uuid(0x2A37);

	public static final UUID SERVICE_DEVICE_INFORMATION = uuid(0x180A);
	public static final UUID CHARACTERISTIC_SOFTWARE_REVISION = uuid(0x2A28);

	public static final UUID SERVICE_FITNESS_MACHINE = uuid(0x1826);
	public static final UUID CHARACTERISTIC_ROWER_DATA = uuid(0x2AD1);
	public static final UUID CHARACTERISTIC_CONTROL_POINT = uuid(0x2AD9);

	public static final UUID SERVICE_BATTERY = uuid(0x180F);
	public static final UUID CHARACTERISTIC_BATTERY_LEVEL = uuid(0x2A19);

	public static final UUID CLIENT_CHARACTERISTIC_DESCRIPTOR = uuid(0x2902);

	private Queue<Request> requests = new ArrayDeque<>();

	private Request current = null;

	private interface Request {

		void request();
	}

	private void request(Request request) {
		requests.add(request);

		requestNext();
	}

	private void requestNext() {
		if (current != null) {
			return;
		}

		current = requests.poll();
		if (current != null) {
			current.request();
		}
	}

	public BluetoothGattCharacteristic get(BluetoothGatt gatt, UUID service, UUID characteristic) {
		BluetoothGattService s = gatt.getService(service);
		if (s == null) {
			return null;
		}

		BluetoothGattCharacteristic c = s.getCharacteristic(characteristic);
		return c;
	}

	@CallSuper
	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		if (current != null) {
			current = null;
			requestNext();
		}
	}

	@CallSuper
	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		if (current != null) {
			current = null;
			requestNext();
		}
	}

	@CallSuper
	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		if (current != null) {
			current = null;
			requestNext();
		}
	}

	public void enableNotification(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

		request(new Request() {
			@Override
			public void request() {
				gatt.setCharacteristicNotification(characteristic, true);

				BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_DESCRIPTOR);
				if (descriptor != null) {
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					if (gatt.writeDescriptor(descriptor) == false) {
						Log.e(Coxswain.TAG, "bluetooth enabled notification failed");
					}
				}
			}
		});
	}

	public void enableIndication(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

		request(new Request() {
			@Override
			public void request() {
				gatt.setCharacteristicNotification(characteristic, true);

				BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_DESCRIPTOR);
				if (descriptor != null) {
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
					if (gatt.writeDescriptor(descriptor) == false) {
						Log.e(Coxswain.TAG, "bluetooth enabled indication failed");
					}
				}
			}
		});
	}

	public void write(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte value) {

		request(new Request() {
			@Override
			public void request() {
				characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
				if (gatt.writeCharacteristic(characteristic) == false) {
					Log.e(Coxswain.TAG, "bluetooth write failed");
				}
			}
		});
	}

	public void read(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
		request(new Request() {
			@Override
			public void request() {
				if (gatt.readCharacteristic(characteristic) == false) {
					Log.e(Coxswain.TAG, "bluetooth read failed");
				}
			}
		});
	}

	public static final UUID uuid(int id) {
		return UUID.fromString(String.format("%08X-0000-1000-8000-00805f9b34fb", id));
	}
}