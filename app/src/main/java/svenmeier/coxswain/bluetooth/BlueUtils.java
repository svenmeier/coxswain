package svenmeier.coxswain.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;

import com.google.android.gms.fitness.request.BleScanCallback;

import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BlueUtils {

	public static final UUID SERVICE_HEART_RATE = uuid(0x180D);

	public static final UUID CHARACTERISTIC_HEART_RATE_MEASUREMENT = uuid(0x2A37);

	public static final UUID SERVICE_FITNESS_MACHINE = uuid(0x1826);

	public static final UUID CHARACTERISTIC_ROWER_DATA = uuid(0x2AD1);

	/**
	 * https://github.com/kinetic-fit/sensors-swift/blob/master/Sources/SwiftySensors/FitnessMachineService.swift
	 * https://github.com/kinetic-fit/sensors-swift/blob/master/Sources/SwiftySensors/FitnessMachineSerializer.swift
	 */
	public static final UUID CHARACTERISTIC_CONTROL_POINT = uuid(0x2AD9);

	public static final UUID CLIENT_CHARACTERISTIC_DESCIPRTOR = uuid(0x2902);

	public static final UUID uuid(int id) {
		return UUID.fromString(String.format("%08X-0000-1000-8000-00805f9b34fb", id));
	}

	public static boolean enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

		gatt.setCharacteristicNotification(characteristic, true);

		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BlueUtils.CLIENT_CHARACTERISTIC_DESCIPRTOR);
		if (descriptor == null) {
			return false;
		} else {
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			gatt.writeDescriptor(descriptor);
			return true;
		}
	}

	public static boolean enableIndication(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

		gatt.setCharacteristicNotification(characteristic, true);

		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BlueUtils.CLIENT_CHARACTERISTIC_DESCIPRTOR);
		if (descriptor == null) {
			return false;
		} else {
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
			gatt.writeDescriptor(descriptor);
			return true;
		}
	}

	public static boolean write(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int value) {
		characteristic.setValue(new byte[]{(byte)value});
		return gatt.writeCharacteristic(characteristic);
	}

	public static boolean write(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte... value) {
		characteristic.setValue(value);
		return gatt.writeCharacteristic(characteristic);
	}
}
