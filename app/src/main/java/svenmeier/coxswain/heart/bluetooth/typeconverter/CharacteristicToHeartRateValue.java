package svenmeier.coxswain.heart.bluetooth.typeconverter;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.function.BiFunction;
import java.util.function.Function;

import svenmeier.coxswain.heart.bluetooth.reading.GattHeartRateValue;

/**
 *  This is _not_ for regular readings
 *
 *  @see CharacteristicToHeartRateMeasurement
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public enum CharacteristicToHeartRateValue implements BiFunction<BluetoothGattCharacteristic, Throwable, GattHeartRateValue> {
    INSTANCE;

    @Override
    public GattHeartRateValue apply(final BluetoothGattCharacteristic bytes, final Throwable throwable) {
        return new GattHeartRateValue(bytes);
    }

    @Override
    public <V> BiFunction<BluetoothGattCharacteristic, Throwable, V> andThen(Function<? super GattHeartRateValue, ? extends V> after) {
        throw new UnsupportedOperationException("Unavailable in Java 1.7 backport");
    }
}
