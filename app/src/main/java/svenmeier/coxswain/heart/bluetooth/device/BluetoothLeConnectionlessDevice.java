package svenmeier.coxswain.heart.bluetooth.device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.Consumer;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartServices;
import svenmeier.coxswain.util.Destroyable;

import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_FIRST_MATCH;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_MATCH_LOST;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER;

/**
 *  Some devices provide data in their scan-advertisements. We'll handle these here.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class BluetoothLeConnectionlessDevice implements BluetoothHeartDevice {
    final Context context;
    private final BluetoothDevice device;

    private final ScanSettings WATCHING_SCAN = new ScanSettings.Builder()
            .setScanMode(SCAN_MODE_LOW_POWER)
            .build();

    private final List<ScanFilter> DEVICES_FILTER = ImmutableList.of(
            new ScanFilter.Builder()
                    .setServiceUuid(BluetoothHeartServices.HEART_RATE.getParcelUuid())
                    .build());

    public BluetoothLeConnectionlessDevice(final Context context, final BluetoothDevice device) {
        this.context = context;
        this.device = device;
    }

    @Override
    public Destroyable watch(final Consumer<Integer> heartRateConsumer) {
        final OnRead listener = new OnRead(heartRateConsumer);

        final BluetoothLeScanner scanner =
                 context.getSystemService(BluetoothManager.class)
                .getAdapter()
                .getBluetoothLeScanner();

        scanner.startScan(
                    ImmutableList.of(new ScanFilter.Builder()
                            .setDeviceAddress(device.getAddress())
                            .build()),
                    WATCHING_SCAN,
                    listener);

        return new ScanDestroyer(scanner, listener);
    }

    @Override
    public void destroy() {

    }

    private static class OnRead extends ScanCallback {
        private final Consumer<Integer> heartRateConsumer;

        public OnRead(Consumer<Integer> heartRateConsumer) {
            this.heartRateConsumer = heartRateConsumer;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            switch (callbackType) {
                case CALLBACK_TYPE_ALL_MATCHES:
                case CALLBACK_TYPE_FIRST_MATCH:
                    final byte[] heartRate = result.getScanRecord().getServiceData(BluetoothHeartServices.HEART_RATE.getParcelUuid());
                    if (heartRate != null && heartRate.length == 1) {
                        heartRateConsumer.accept(Integer.valueOf(heartRate[0]));
                    }
                    break;
                case CALLBACK_TYPE_MATCH_LOST:
                    Log.e(Coxswain.TAG, "Lost connectionless hart rate");
                    break;
                default:
                    Log.e(Coxswain.TAG, "Ignoring unexpected callback-type " + callbackType);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }
    }

    public static class ScanDestroyer implements Destroyable {
        private final BluetoothLeScanner scanner;
        private final OnRead handler;

        public ScanDestroyer(final BluetoothLeScanner scanner, final OnRead handler) {
            this.scanner = scanner;
            this.handler = handler;
        }

        public void destroy() {
            scanner.stopScan(handler);
        }
    }
}
