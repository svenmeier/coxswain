package svenmeier.coxswain.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * A queue of {@link BluetoothGatt} devices.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattDeviceQueue {

	private LinkedHashMap<String, BluetoothGatt> map = new LinkedHashMap<>();

	/**
	 * Close all contained devices.
	 */
	public void close() {
		for (BluetoothGatt gatt : map.values()) {
			gatt.close();
		}

		map.clear();
	}

	public void add(BluetoothGatt gatt) {
		String address = gatt.getDevice().getAddress();
		if (map.containsKey(address) == false) {
			map.put(address, gatt);
		}
	}

	public void remove(BluetoothGatt gatt) {
		map.remove(gatt.getDevice().getAddress());
	}

	public BluetoothGatt poll() {
		BluetoothGatt next = null;

		Iterator<BluetoothGatt> iterator = map.values().iterator();
		if (iterator.hasNext()) {
			next = iterator.next();

			iterator.remove();
		}

		return next;
	}
}
