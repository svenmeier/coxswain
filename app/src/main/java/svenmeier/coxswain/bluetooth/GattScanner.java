package svenmeier.coxswain.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import svenmeier.coxswain.Coxswain;

/**
 * A scanner of {@link BluetoothGatt} devices.
 *
 * @see #onDiscovered(BluetoothGatt)
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattScanner extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {

	private static final UUID CLIENT_CHARACTERISTIC_DESCIPRTOR = uuid(0x2902);

	private Context context;

	/**
	 * An optional previously bound device to connect to first.
	 */
	private String bound;

	/**
	 * Used while started.
	 */
	private BluetoothAdapter adapter;

	/**
	 * Are devices scanned currently.
	 */
	private boolean scanning;

	private Set<String> scanned = new HashSet<>();

	private BluetoothGatt discovering;

	public GattScanner(Context context, String bound) {
		this.context = context;

		this.bound = bound;
	}

	public synchronized boolean start() {

		if (adapter != null) {
			// already started
			return true;
		}

		Log.d(Coxswain.TAG, "bluetooth starting");

		BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		adapter = manager.getAdapter();

		if (bound != null) {
			try {
				BluetoothDevice device = adapter.getRemoteDevice(bound);
				if (device != null) {
					connect(device);

					// assume successful connect
					return true;
				}
			} catch (Exception preferredFailed) {
			}
		}

		scan();

		// successful if scanning
		return scanning;
	}

	/**
	 * Stop.
	 */
	public synchronized void stop() {
		if (adapter == null) {
			// already stopped
			return;
		}

		if (discovering != null) {
			// still discovering
			discovering.close();
			discovering = null;
		}

		// forget all scanned
		scanned.clear();

		unscan();

		Log.d(Coxswain.TAG, "bluetooth stopped");

		adapter = null;
	}

	private void scan() {
		if (scanning) {
			// already scanning
			return;
		}

		scanning = adapter.startLeScan(this);
		Log.d(Coxswain.TAG, "bluetooth scan " + scanning);
	}

	private void unscan() {
		if (scanning == false) {
			// not scanning
			return;
		}

		Log.d(Coxswain.TAG, "bluetooth unscan");
		adapter.stopLeScan(this);
		scanning = false;
	}

	@Override
	public synchronized void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		if (adapter == null || scanning == false) {
			return;
		}

		String address = device.getAddress();
		if (scanned.contains(address) == false) {
			Log.d(Coxswain.TAG, "bluetooth scanned " + address);
			scanned.add(address);

			// stop scanning immediately, some devices won't connect otherwise
			unscan();

			connect(device);
		}
	}

	private void connect(BluetoothDevice device) {
		Log.d(Coxswain.TAG, "bluetooth connecting " + device.getAddress());
		device.connectGatt(context, false, this);
	}

	@Override
	public synchronized void onConnectionStateChange(BluetoothGatt candidate, int status, int newState) {
		String address = candidate.getDevice().getAddress();

		if (newState == BluetoothProfile.STATE_CONNECTED) {
			Log.d(Coxswain.TAG, "bluetooth discovering " + address);

			if (adapter != null) {
				// still started
				discovering = candidate;
				discovering.discoverServices();
			}
		} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			if (discovering != null && discovering.getDevice().getAddress().equals(address)) {
				Log.d(Coxswain.TAG, "bluetooth discovering lost " + address);

				discovering.close();
				discovering = null;

				// remove from scanned for another chance
				scanned.remove(address);
			} else {
				Log.d(Coxswain.TAG, "bluetooth lost " + address);
			}

			onLost(candidate);

			if (adapter != null) {
				// still started or started again
				scan();
			}
		}
	}

	@Override
	public synchronized void onServicesDiscovered(BluetoothGatt candidate, int status) {
		if (adapter == null) {
			return;
		}

		String address = candidate.getDevice().getAddress();

		if (discovering == null || discovering.getDevice().getAddress().equals(address) == false) {
			return;
		}

		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d(Coxswain.TAG, "bluetooth discovered " + address);

			// subclass is responsible for connection now
			BluetoothGatt discovered = discovering;
			discovering = null;
			onDiscovered(discovered);
		}

		if (adapter != null) {
			scan();
		}
	}

	/**
	 * A Gatt device was discovered.
	 * <p>
	 * Note: The default implementation just closes the device - if overridden, the subclass is
	 * responsible to close the device, whether immediately or later on.
	 *
	 * @param discovered discovered gatt
	 */
	protected void onDiscovered(BluetoothGatt discovered) {
		discovered.close();
	}

	/**
	 * A candidate was lost.
	 *
	 * @param lost lost gatt
	 */
	protected void onLost(BluetoothGatt lost) {
	}

	protected boolean enableNotification(BluetoothGatt candidate, BluetoothGattCharacteristic characteristic) {
		boolean status;

		status = candidate.setCharacteristicNotification(characteristic, true);
		Log.d(Coxswain.TAG, "bluetooth characteristic notification set " + status);

		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_DESCIPRTOR);
		if (descriptor == null) {
			return false;
		} else {
			Log.d(Coxswain.TAG, "bluetooth characteristic descriptor acquired");

			status = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			Log.d(Coxswain.TAG, "bluetooth descriptor notification set " + status);

			status = candidate.writeDescriptor(descriptor);
			Log.d(Coxswain.TAG, "bluetooth descriptor write " + status);

			// return true despite of the actual status
			return true;
		}
	}

	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicWrite(gatt, characteristic, status);

		Log.d(Coxswain.TAG, "bluetooth descriptor written " + status);
	}

	public static final UUID uuid(int id) {
		return UUID.fromString(String.format("%08X", id) + "-0000-1000-8000-00805f9b34fb");
	}
}
