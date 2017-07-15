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

public enum BluetoothHeartCharacteristics {
    HEART_RATE_MEASUREMENT(BluetoothHeartServices.HEART_RATE, "2A37-0000-1000-8000-00805f9b34fb");

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
