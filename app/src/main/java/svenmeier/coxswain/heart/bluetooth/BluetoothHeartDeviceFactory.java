package svenmeier.coxswain.heart.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.concurrent.ExecutionException;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.bluetooth.device.AbstractBluetoothHeartAdditionalReadingsDevice;
import svenmeier.coxswain.heart.bluetooth.device.BluetoothHeartDevice;
import svenmeier.coxswain.heart.bluetooth.device.BluetoothLeConnectionlessDevice;
import svenmeier.coxswain.heart.bluetooth.device.NotificationBluetoothHeartDevice;
import svenmeier.coxswain.heart.bluetooth.device.PollingBluetoothHeartDevice;

/**
 *  Creates the appropriate BluetoothHeartDevice based on the devices properties.
 *  This will take some seconds, as the device needs to be connected to get the info.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class BluetoothHeartDeviceFactory {
    final Context context;
    final BluetoothHeartConnectionListener connectionListener;

    public BluetoothHeartDeviceFactory(@NonNull Context context, @Nullable BluetoothHeartConnectionListener connectionListener) {
        this.context = context;
        this.connectionListener = connectionListener;
    }

    /**
     *  Creates a Polling or Notification based device
     */
    public AbstractBluetoothHeartAdditionalReadingsDevice make(final BluetoothDevice device) {
        final PollingBluetoothHeartDevice dev = new PollingBluetoothHeartDevice(context, device, connectionListener);

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

    public BluetoothHeartDevice makeConnectionless(final BluetoothDevice device) {
        return new BluetoothLeConnectionlessDevice(context, device);
    }
}
