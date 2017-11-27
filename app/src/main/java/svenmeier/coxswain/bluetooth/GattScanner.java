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
import java.util.Iterator;
import java.util.LinkedHashMap;
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

	private String preferred;

	private BluetoothAdapter adapter;

	private boolean scanning;

	private Set<String> scanned = new HashSet<>();

	private BluetoothGatt connected;

	public GattScanner(Context context, String preferred) {
		this.context = context;

		this.preferred = preferred;
	}

	public synchronized boolean start() {

		if (adapter != null) {
			// already started
			return true;
		}

		Log.d(Coxswain.TAG, "bluetooth starting");

		BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		adapter = manager.getAdapter();

		if (preferred != null) {
			try {
				BluetoothDevice device = adapter.getRemoteDevice(preferred);
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
			// aleady stopped
			return;
		}

		if (connected != null) {
			// still connected
			connected.close();
			connected = null;
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
		if (adapter == null) {
			return;
		}

		String address = candidate.getDevice().getAddress();

		if (newState == BluetoothProfile.STATE_CONNECTED) {
			Log.d(Coxswain.TAG, "bluetooth connected " + candidate.getDevice().getAddress());

			connected = candidate;
			connected.discoverServices();
		} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			if (connected != null && connected.getDevice().getAddress().equals(address)) {
				connected.close();
				connected = null;

				// remove from scanned for another chance
				scanned.remove(address);
			}

			onLost(candidate);

			if (adapter != null) {
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

		if (connected == null || connected.getDevice().getAddress().equals(address) == false) {
			return;
		}

		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d(Coxswain.TAG, "bluetooth discovered " + address);

			// subclass is responsible for connection now
			BluetoothGatt discovered = connected;
			connected = null;
			onDiscovered(discovered);
		}

		if (adapter != null) {
			scan();
		}
	}

	/**
	 *
	 * A candidate was discovered.
	 * <p>
	 * Note: The default implementation just closes the device - if overridden the subclass is
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
	 * @param candidate lost gatt
	 */
	protected void onLost(BluetoothGatt candidate) {
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
