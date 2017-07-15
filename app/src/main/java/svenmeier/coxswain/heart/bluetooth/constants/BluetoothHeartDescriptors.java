package svenmeier.coxswain.heart.bluetooth.constants;

import android.os.ParcelUuid;

import java.util.UUID;

public enum BluetoothHeartDescriptors {
    CLIENT_CHARACTERISTIC_CONFIGURATION("00002902-0000-1000-8000-00805f9b34fb");

    private final ParcelUuid uuid;

    BluetoothHeartDescriptors(final String uuid) {
        this.uuid = ParcelUuid.fromString(uuid);
    }

    public ParcelUuid getParcelUuid() {
        return uuid;
    }

    public UUID getUuid() {
        return uuid.getUuid();
    }
}
