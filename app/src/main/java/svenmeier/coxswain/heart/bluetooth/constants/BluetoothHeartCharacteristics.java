package svenmeier.coxswain.heart.bluetooth.constants;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 *  Lists readings to be obtained from the BT device.
 */
public enum BluetoothHeartCharacteristics {
    HEART_RATE_MEASUREMENT(BluetoothHeartServices.HEART_RATE, "2A37-0000-1000-8000-00805f9b34fb"),
    BODY_SENSOR_LOCATION(BluetoothHeartServices.HEART_RATE, "2A38-0000-1000-8000-00805f9b34fb"),
    BATTERY_STATUS(BluetoothHeartServices.BATTERY, "2A19-0000-1000-8000-00805f9b34fb"),

    // Service is most likely wrong
    AEROBIC_HEART_RATE_LOWER_LIMIT(BluetoothHeartServices.HEART_RATE, "2A7E-0000-1000-8000-00805f9b34fb"),
    AEROBIC_HEART_RATE_UPPER_LIMIT(BluetoothHeartServices.HEART_RATE, "2A84-0000-1000-8000-00805f9b34fb"),
    ANAEROBIC_HEART_RATE_LOWER_LIMIT(BluetoothHeartServices.HEART_RATE, "2A81-0000-1000-8000-00805f9b34fb"),
    ANAEROBIC_HEART_RATE_UPPER_LIMIT(BluetoothHeartServices.HEART_RATE, "2A82-0000-1000-8000-00805f9b34fb"),
    FAT_BURN_HEART_RATE_LOWER_LIMIT(BluetoothHeartServices.HEART_RATE, "2A88-0000-1000-8000-00805f9b34fb"),
    FAT_BURN_HEART_RATE_UPPER_LIMIT(BluetoothHeartServices.HEART_RATE, "2A89-0000-1000-8000-00805f9b34fb"),
    RESTING_HEART_RATE(BluetoothHeartServices.HEART_RATE, "A92-0000-1000-8000-00805f9b34fb")
    ;

    private final BluetoothHeartServices service;
    private final ParcelUuid uuid;

    BluetoothHeartCharacteristics(final BluetoothHeartServices service, final String uuid) {
        this.service = service;
        this.uuid = ParcelUuid.fromString(uuid);
    }

    public BluetoothHeartServices getService() {
        return service;
    }

    public UUID getUuid() {
        return uuid.getUuid();
    }

    public ParcelUuid getParcelUuid() {
        return uuid;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public BluetoothGattCharacteristic lookup(final BluetoothGatt gatt) {
        Objects.requireNonNull(gatt, "GATT in null");
        return Objects.requireNonNull(
                gatt.getService(service.getUuid()),
                    "Service " + service + " unavailable on device")
                .getCharacteristic(getUuid());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Optional<BluetoothHeartCharacteristics> byUuid(UUID uuid) {
        for (BluetoothHeartCharacteristics candidate : values()) {
            if (candidate.getUuid().equals(uuid)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
