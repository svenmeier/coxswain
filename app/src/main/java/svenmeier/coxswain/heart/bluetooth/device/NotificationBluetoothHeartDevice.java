package svenmeier.coxswain.heart.bluetooth.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.List;
import java.util.function.Consumer;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartCharacteristics;
import svenmeier.coxswain.heart.bluetooth.constants.GattHeartRateMeasurement;
import svenmeier.coxswain.util.Destroyable;

@RequiresApi(api = Build.VERSION_CODES.N)
public class NotificationBluetoothHeartDevice extends AbstractBluetoothHeartDevice implements BluetoothHeartDevice {
    public NotificationBluetoothHeartDevice(Context context, BluetoothDevice device) {
        super(context, device);
    }

    public NotificationBluetoothHeartDevice(Conversation conversation) {
        super(conversation);
    }

    @Override
    public Destroyable watch(Consumer<Integer> heartRateConsumer) {
        enableNotifications(BluetoothHeartCharacteristics.HEART_RATE_MEASUREMENT,
                bytes -> onRawHeartData(bytes, reading -> heartRateConsumer.accept(reading.getHeartBpm())));
        return this;
    }

    private void onRawHeartData(final List<Byte> bytes, final Consumer<GattHeartRateMeasurement> heartRateConsumer) {
        final GattHeartRateMeasurement reading = new GattHeartRateMeasurement(bytes);
        Log.d(Coxswain.TAG, "Reading: " + reading);
        if (reading != null) {
            heartRateConsumer.accept(reading);
        }
    }

    public void destroy() {
        disableNotifications(BluetoothHeartCharacteristics.HEART_RATE_MEASUREMENT);
        super.destroy();
    }
}
