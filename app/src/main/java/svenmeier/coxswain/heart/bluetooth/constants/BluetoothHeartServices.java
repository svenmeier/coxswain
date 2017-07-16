package svenmeier.coxswain.heart.bluetooth.constants;

import android.os.ParcelUuid;

import java.util.UUID;

public enum BluetoothHeartServices {
    HEART_RATE("180d-0000-1000-8000-00805f9b34fb"),
    BATTERY("180f-0000-1000-8000-00805f9b34fb");

    private final ParcelUuid uuid;

    BluetoothHeartServices(String uuid) {
        this.uuid = ParcelUuid.fromString(uuid);
    }

    public ParcelUuid getParcelUuid() {
        return uuid;
    }

    public UUID getUuid() {
        return uuid.getUuid();
    }
}
