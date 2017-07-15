package svenmeier.coxswain.heart.bluetooth.typeconverter;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.function.BiFunction;
import java.util.function.Function;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;


@RequiresApi(api = Build.VERSION_CODES.N)
public enum CharacteristicToNotificationSupport implements BiFunction<BluetoothGattCharacteristic, Throwable, Boolean> {
    INSTANCE;

    @Override
    public Boolean apply(BluetoothGattCharacteristic characteristic, Throwable throwable) {
        return (characteristic.getProperties() & PROPERTY_NOTIFY) != 0;
    }

    @Override
    public <V> BiFunction<BluetoothGattCharacteristic, Throwable, V> andThen(Function<? super Boolean, ? extends V> after) {
        throw new UnsupportedOperationException("Unavailable in Java 1.7 backport");
    }
}
