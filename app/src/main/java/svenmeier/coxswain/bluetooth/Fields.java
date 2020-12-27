package svenmeier.coxswain.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Fields {

	public static final int UINT8 = BluetoothGattCharacteristic.FORMAT_UINT8;

	public static final int UINT16 = BluetoothGattCharacteristic.FORMAT_UINT16;

	public static final int UINT32 = BluetoothGattCharacteristic.FORMAT_UINT32;

	public static final int SINT16 = BluetoothGattCharacteristic.FORMAT_SINT16;

	private final BluetoothGattCharacteristic characteristic;

	private int flag;

	private int offset = 0;

	public Fields(BluetoothGattCharacteristic characteristic, int flagSize) {
		this.characteristic = characteristic;

		flag = get(flagSize);
	}

	public boolean flag(int bit) {
		return (flag & (1 << bit)) != 0;
	}

	public void skip(int format) {
		offset += size(format);
	}

	public int get(int format) {
		int value = characteristic.getIntValue(format, offset);

		offset += size(format);

		return value;
	}

	private int size(int format) {
		return format & 0xf;
	}

	public int[] remaining(int format) {
		int count = (characteristic.getValue().length - offset) / size(format);

		int[] values = new int[count];
		for (int c = 0; c < count; c++) {
			values[c] = get(format);
		}

		return values;
	}
}
