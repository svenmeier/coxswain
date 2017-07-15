package svenmeier.coxswain.heart.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Heart;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.heart.ConnectionStatus;
import svenmeier.coxswain.heart.ToastConnectionStatusListener;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.util.Destroyable;

/**
 *  Provides the new implementation in the format of the previous Heart-readings
 */
@SuppressWarnings("unused") // Initialized through reflection
public class BluetoothHeartAdapter extends Heart implements BatteryStatusListener {
    CompletableFuture<Destroyable> heartRateListener = null;
    final BluetoothAsyncConnector connector;
    private @Nullable String deviceName;

    public BluetoothHeartAdapter(Context context, Measurement measurement) {
        super(context, measurement);

        registerConnectionStatusListener(new ToastConnectionStatusListener(context));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Log.i(Coxswain.TAG, "Start bluetooth-scan...");
            connector = new BluetoothAsyncConnector();
            connector.execute(this);
        } else {
            Log.w(Coxswain.TAG, "New bluetooth unavailable: Needs Nougat!");
            updateConnectionStatus(ConnectionStatus.UNAVAILABLE_ON_SYSTEM, null, "Requires Android Nougat");
            connector = null;
        }
    }

    @Override
    public void onHeartRate(int heartRate) {
        updateConnectionStatus(ConnectionStatus.CONNECTED, deviceName, null);
        super.onHeartRate(heartRate);
    }

    @Override
    public void onBatteryStatus(int percentageLeft) {
        if (percentageLeft < 20) {
            // TODO: Has to be on UI-thred
            //Toast.makeText(context, "Battery of " + deviceName +": " + percentageLeft + "%", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void destroy() {
        if (heartRateListener != null) {
            heartRateListener.whenComplete(new BiConsumer<Destroyable, Throwable>() {
                @Override
                public void accept(Destroyable destroyable, Throwable throwable) {
                    if (destroyable != null) {
                        destroyable.destroy();
                    }
                }
            });
        }
        if (connector != null) {
            connector.cancel(true);
        }
        super.destroy();
    }

    public Context getContext() {
        return context;
    }
}
