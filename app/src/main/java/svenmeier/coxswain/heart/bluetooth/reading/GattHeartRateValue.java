package svenmeier.coxswain.heart.bluetooth.reading;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 *  This is the value used for certain characteristics. It's _NOT_ the current reading.
 *
 *  It's used in conjunction with the characteristics (among others):
 *      * AEROBIC_HEART_RATE_LOWER_LIMIT
 *      * AEROBIC_HEART_RATE_UPPER_LIMIT
 *      * ANAEROBIC_HEART_RATE_LOWER_LIMIT
 *      * ANAEROBIC_HEART_RATE_UPPER_LIMIT
 *      * FAT_BURN_HEART_RATE_LOWER_LIMIT
 *      * FAT_BURN_HEART_RATE_UPPER_LIMIT
 *      * RESTING_HEART_RATE
 *
 *  @see GattHeartRateMeasurement
 */
public class GattHeartRateValue {
    final Integer valueBpm;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public GattHeartRateValue(BluetoothGattCharacteristic chr) {
        this.valueBpm = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
    }

    public Integer getValueBpm() {
        return valueBpm;
    }
}
