package svenmeier.coxswain.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.LinkedHashMap;

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

	private Context context;

	private BluetoothAdapter adapter;

	private LinkedHashMap<String, BluetoothDevice> devices = new LinkedHashMap<>();

	private boolean connecting;

	private BluetoothGatt current;

	public GattScanner(Context context) {
		this.context = context;
	}

	public synchronized boolean startScanning() {
		if (adapter == null) {
			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

			adapter = manager.getAdapter();

			if (adapter.startLeScan(this) == false) {
				adapter = null;
				return false;
			}
		}

		return true;
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

		if (current != null) {
			current.close();
			current = null;
		}
		connecting = false;

		adapter.stopLeScan(this);
		adapter = null;
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

	@WorkerThread
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

	@WorkerThread
	@Override
	public synchronized void onConnectionStateChange(BluetoothGatt candidate, int status, int newState) {
		String address = candidate.getDevice().getAddress();

		if (newState == BluetoothProfile.STATE_CONNECTED) {
			if (current == null) {
				Log.d(Coxswain.TAG, "bluetooth gatt connected " + candidate.getDevice().getAddress());

				current = candidate;
				current.discoverServices();
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

		if (current != null && current.getDevice().getAddress().equals(address)) {
			connecting = false;
			current = null;
		}

		candidate.close();

		connect();
	}

	@WorkerThread
	@Override
	public synchronized void onServicesDiscovered(BluetoothGatt candidate, int status) {
		if (current == null || candidate.getDevice().getAddress().equals(candidate.getDevice().getAddress()) == false) {
			return;
		}

		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d(Coxswain.TAG, "bluetooth services discovered " + candidate.getDevice().getAddress());

			if (onFound(candidate)) {
				devices.remove(candidate.getDevice().getAddress());
				current = null;
				connecting = false;
			}

			if (isScanning()) {
				connect();
			}
		} else {
			retry(candidate);
		}
	}

	/**
	 * A candidate was found.
	 *
	 * @param candidate found gatt
	 * @return {@code true} if accepted
	 */
	protected boolean onFound(BluetoothGatt candidate) {
		return false;
	}

	/**
	 * A candidate was lost.
	 *
	 * @param candidate lost gatt
	 */
	protected void onLost(BluetoothGatt candidate) {
	}
}
