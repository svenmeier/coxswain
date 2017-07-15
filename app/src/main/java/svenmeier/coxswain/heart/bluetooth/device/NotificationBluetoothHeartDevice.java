package svenmeier.coxswain.heart.bluetooth.device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.ArraySet;
import android.util.Log;

import java.util.Set;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartCharacteristics;
import svenmeier.coxswain.heart.bluetooth.reading.GattHeartRateMeasurement;
import svenmeier.coxswain.heart.generic.HeartRateListener;
import svenmeier.coxswain.util.Destroyable;

/**
 *  A heart-rate monitor, where we subscribe to updates of the measurement.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class NotificationBluetoothHeartDevice extends AbstractBluetoothHeartDevice implements BluetoothHeartDevice, BluetoothNotificationListener {
    private Set<HeartRateListener> listeners = new ArraySet<>(1);

    public NotificationBluetoothHeartDevice(Context context, BluetoothDevice device) {
        super(context, device);
    }

    public NotificationBluetoothHeartDevice(Conversation conversation) {
        super(conversation);
    }

    @Override
    public Destroyable watch(final HeartRateListener heartRateConsumer) {
        listeners.add(heartRateConsumer);
        enableNotifications(BluetoothHeartCharacteristics.HEART_RATE_MEASUREMENT, this);
        return this;
    }

    @Override
    public void onNotification(final BluetoothGattCharacteristic chr) {
        final GattHeartRateMeasurement reading = new GattHeartRateMeasurement(chr);
        Log.d(Coxswain.TAG, "Reading: " + reading);
        if (reading != null) {
            for (HeartRateListener listener: listeners) {
                // TODO: We could also supply other characteristics
                listener.onHeartRate(reading.getHeartBpm());
            }
        }
    }

    public void destroy() {
        disableNotifications(BluetoothHeartCharacteristics.HEART_RATE_MEASUREMENT);
        super.destroy();
    }
}
