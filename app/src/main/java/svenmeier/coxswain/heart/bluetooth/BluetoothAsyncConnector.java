package svenmeier.coxswain.heart.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.ConnectionStatus;
import svenmeier.coxswain.heart.bluetooth.device.AbstractBluetoothHeartAdditionalReadingsDevice;
import svenmeier.coxswain.heart.bluetooth.device.BluetoothHeartDevice;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.util.Destroyable;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BluetoothAsyncConnector extends AsyncTask<BluetoothHeartAdapter, ConnectionStatus, BluetoothHeartDevice> implements BluetoothHeartDiscoveryListener {
    private Destroyable currentScan;
    private BluetoothHeartAdapter heartAdapter;
    private CompletableFuture<BluetoothHeartDevice> ret = new CompletableFuture<>();

    @Override
    protected BluetoothHeartDevice doInBackground(BluetoothHeartAdapter... adapters) {
        try {
            heartAdapter = adapters[0];
            scan(heartAdapter.getContext());
            return ret.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void scan(final Context context) {
        Log.i(Coxswain.TAG, "Start bluetooth-scan...");
        final BluetoothLeHeartScanner scanner = new BluetoothLeHeartScanner(context);
        publishProgress(ConnectionStatus.SCANNING);
        currentScan = scanner.scan(this);
        Log.i(Coxswain.TAG, "SCAN returned");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDiscovered(BluetoothDevice device, int SignalStrength, boolean supportsConnectionLess) {
        Log.i(Coxswain.TAG, "SCAN DISCO");
        publishProgress(ConnectionStatus.CONNECTING);
        Log.i(Coxswain.TAG, "Using first discovered device: " + device.getAddress() + " -> " + device.getName());
        currentScan.destroy();
        Log.i(Coxswain.TAG, "SCAN destroyed");
        // TODO: Does not cover connection-less as we have to scan again using scanner.find()
        connectNow(device);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void connectNow(final BluetoothDevice device) {
        Log.i(Coxswain.TAG, "MAKING DEV");
        final AbstractBluetoothHeartAdditionalReadingsDevice dev = new BluetoothHeartDeviceFactory(heartAdapter.getContext()).make(device);
        dev.readBattery(heartAdapter);
        //dev.readAnarobicHeartRateLowerLimit();
        //dev.readAnarobicHeartRateUpperLimit();

        Log.i(Coxswain.TAG, "Start watching");
        dev.watch(heartAdapter);
        ret.complete(dev);
    }

    @Override
    public void onLost(String deviceId) {
        publishProgress(ConnectionStatus.CONNECTING);
    }
}
