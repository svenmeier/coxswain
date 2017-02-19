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

import java.util.LinkedHashMap;
import java.util.UUID;

import svenmeier.coxswain.Coxswain;

/**
 * A scanner of {@link BluetoothGatt} devices.
 *
 * @see #onFound(BluetoothGatt)
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattScanner extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {

	// direct connection (not auto) is faster
	private static final boolean DIRECT = false;

	private static final UUID CLIENT_CHARACTERISTIC_DESCIPRTOR = uuid(0x2902);

	private Context context;

	private BluetoothAdapter adapter;

	private LinkedHashMap<String, BluetoothDevice> devices = new LinkedHashMap<>();

	private boolean connecting;

	private BluetoothGatt discovering;

	public GattScanner(Context context) {
		this.context = context;
	}

	public synchronized boolean startScanning() {
		boolean started = true;

		if (adapter == null) {
			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

			adapter = manager.getAdapter();

			started = adapter.startLeScan(this);

			Log.d(Coxswain.TAG, "bluetooth scan started " + started);
		}

		return started;
	}

	public boolean isScanning() {
		return this.adapter != null;
	}

	/**
	 * Close all contained devices.
	 */
	public synchronized void stopScanning() {
		if (adapter == null) {
			return;
		}

		devices.clear();

		if (discovering != null) {
			discovering.close();
			discovering = null;
		}
		connecting = false;

		adapter.stopLeScan(this);
		adapter = null;
		Log.d(Coxswain.TAG, "bluetooth scan stopped");
	}

	/**
	 * Connect if not already connecting.
	 */
	private void connect() {
		if (connecting == false) {
			if (devices.isEmpty() == false) {
				BluetoothDevice device = devices.values().iterator().next();

				Log.d(Coxswain.TAG, "bluetooth services connecting " + device.getAddress());
				device.connectGatt(context, DIRECT, this);

				connecting = true;
			}
		}
	}

	@Override
	public synchronized void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		if (isScanning() == false) {
			return;
		}

		String address = device.getAddress();
		if (devices.containsKey(address) == false) {
			Log.d(Coxswain.TAG, "bluetooth gatt found " + address);

			devices.put(address, device);

			connect();
		}
	}

	@Override
	public synchronized void onConnectionStateChange(BluetoothGatt candidate, int status, int newState) {
		String address = candidate.getDevice().getAddress();

		if (newState == BluetoothProfile.STATE_CONNECTED) {
			if (connecting == true && discovering == null) {
				Log.d(Coxswain.TAG, "bluetooth gatt connected " + candidate.getDevice().getAddress());

				discovering = candidate;
				discovering.discoverServices();
			}
		} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			// link loss?
			Log.d(Coxswain.TAG, "bluetooth gatt disconnected " + address);

			onLost(candidate);

			if (isScanning()) {
				retry(candidate);
			}
		}
	}

	private void retry(BluetoothGatt candidate) {
		String address = candidate.getDevice().getAddress();

		BluetoothDevice removed = devices.remove(address);
		if (removed != null) {
			devices.put(address, removed);
		}

		if (discovering != null && discovering.getDevice().getAddress().equals(address)) {
			connecting = false;
			discovering = null;
		}

		candidate.close();

		connect();
	}

	@Override
	public synchronized void onServicesDiscovered(BluetoothGatt candidate, int status) {
		String address = candidate.getDevice().getAddress();

		if (discovering == null || discovering.getDevice().getAddress().equals(address) == false) {
			return;
		}

		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d(Coxswain.TAG, "bluetooth services discovered " + address);

			devices.remove(address);
			discovering = null;
			connecting = false;

			onFound(candidate);

			if (isScanning()) {
				connect();
			}
		} else {
			retry(candidate);
		}
	}

	/**
	 *
	 * A candidate was found.
	 * <p>
	 * Default implementation just closes the candidate.
	 *
	 * @param candidate found gatt
	 */
	protected void onFound(BluetoothGatt candidate) {
		candidate.close();
	}

	/**
	 * A candidate was lost.
	 *
	 * @param candidate lost gatt
	 */
	protected void onLost(BluetoothGatt candidate) {
	}

	protected boolean enableNotification(BluetoothGatt candidate, BluetoothGattCharacteristic characteristic) {
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

	public static final UUID uuid(int id) {
		return UUID.fromString(String.format("%08X", id) + "-0000-1000-8000-00805f9b34fb");
	}
}
