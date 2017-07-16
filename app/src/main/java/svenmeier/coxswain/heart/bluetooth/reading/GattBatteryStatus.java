package svenmeier.coxswain.heart.bluetooth.reading;


import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 *  Interpret values according to GATT BAS spec 1.0
 */
public class GattBatteryStatus {
    private static final int LOW_THRESHOLD = 20;
    final Integer batteryPercent;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public GattBatteryStatus(BluetoothGattCharacteristic chr) {
        this.batteryPercent = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
    }

    public Integer getBatteryPercent() {
        return batteryPercent;
    }

    public boolean isLow() {
        return (batteryPercent != null) && (batteryPercent < LOW_THRESHOLD);
    }

    @Override
    public String toString() {
        return "Battery level " + batteryPercent + "%";
    }
}
