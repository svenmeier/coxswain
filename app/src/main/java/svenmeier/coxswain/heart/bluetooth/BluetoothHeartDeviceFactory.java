package svenmeier.coxswain.heart.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.concurrent.ExecutionException;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.bluetooth.device.AbstractBluetoothHeartAdditionalReadingsDevice;
import svenmeier.coxswain.heart.bluetooth.device.BluetoothHeartDevice;
import svenmeier.coxswain.heart.bluetooth.device.BluetoothLeConnectionlessDevice;
import svenmeier.coxswain.heart.bluetooth.device.NotificationBluetoothHeartDevice;
import svenmeier.coxswain.heart.bluetooth.device.PollingBluetoothHeartDevice;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BluetoothHeartDeviceFactory {
    final Context context;

    public BluetoothHeartDeviceFactory(Context context) {
        this.context = context;
    }

    public AbstractBluetoothHeartAdditionalReadingsDevice make(final BluetoothDevice device) {
        final PollingBluetoothHeartDevice dev = new PollingBluetoothHeartDevice(context, device);

        try {
            if (dev.canUseBluetoothNotifications().get()) {
                Log.i(Coxswain.TAG, "Using notification-based heart-sensor: " + device.getName());
                return new NotificationBluetoothHeartDevice(dev.getConversation());
            } else {
                Log.i(Coxswain.TAG, "Using polling heart-sensor: " + device.getName());
                return dev;
            }
        } catch (final Exception e) {
            Log.e(Coxswain.TAG, "Error detecting bluetooth notification support", e);
            return dev;
        }
    }

    public BluetoothHeartDevice makeConnectionLess(final BluetoothDevice device) {
        return new BluetoothLeConnectionlessDevice(context, device);
    }
}
