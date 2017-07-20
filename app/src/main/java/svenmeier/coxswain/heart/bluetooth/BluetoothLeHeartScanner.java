package svenmeier.coxswain.heart.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartServices;
import svenmeier.coxswain.heart.bluetooth.device.BluetoothHeartDevice;
import svenmeier.coxswain.util.Destroyable;

import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_FIRST_MATCH;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_MATCH_LOST;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

/**
 *  Searches for GATT enabled HRMs
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class BluetoothLeHeartScanner {
    final Context context;

    private final ScanSettings DISCOVERY_SCAN = new ScanSettings.Builder()
            .setScanMode(SCAN_MODE_LOW_LATENCY)
            .build();

    private final List<ScanFilter> DEVICES_FILTER = ImmutableList.of(
        new ScanFilter.Builder()
            .setServiceUuid(BluetoothHeartServices.HEART_RATE.getParcelUuid())
            .build());

    public BluetoothLeHeartScanner(Context context) {
        this.context = context;
    }

    /**
     *  Scans for bluetooth devices invoking the handler on discovery.
     *  In order to stop the scan call destroy on the returned object. Else the scan will
     *  continue indefinitely consuming high energy.
     */
    public Destroyable scan(final BluetoothHeartDiscoveryListener handler) {
        final OnDevice listener = new OnDevice(handler);
         final Optional<BluetoothAdapter> adapter = getBluetoothAdapter();
         if (adapter.isPresent()) {
             Log.i(Coxswain.TAG, "Starting scan from " + Thread.currentThread().getName());
             final BluetoothLeScanner scanner = adapter.get().getBluetoothLeScanner();
             scanner.startScan(DEVICES_FILTER, DISCOVERY_SCAN, listener);
             return new ScanDestroyer(scanner, listener);
         } else {
             return Destroyable.NULL;
         }
    }

    /**
     * Finds a known device.
     * In order to stop the scan, cancel the returned Future. Else the scan will
     * continue indefinitely consuming high energy.
     *
     * @param deviceId  The bluetooth-address of the device
     */
    public Future<BluetoothHeartDevice> find(final String deviceId) {
        final CompletableFuture<BluetoothHeartDevice> future = new CompletableFuture<>();
        final Optional<BluetoothAdapter> adapter = getBluetoothAdapter();
        final BluetoothHeartDeviceFactory deviceFactory = new BluetoothHeartDeviceFactory(context, null);

        if (adapter.isPresent()) {
            final OnDevice listener = new OnDevice(
                    new BluetoothHeartDiscoveryListener() {
                        @Override
                        public void onDiscovered(BluetoothDevice device, int SignalStrength, boolean supportsConnectionLess) {
                            future.complete(
                                (supportsConnectionLess) ?
                                    deviceFactory.makeConnectionless(device) :
                                    deviceFactory.make(device));
                        }

                        @Override
                        public void onLost(String deviceId, String name) {

                        }
                    });

            final BluetoothLeScanner scanner = adapter.get().getBluetoothLeScanner();

            scanner.startScan(
                    ImmutableList.of(new ScanFilter.Builder()
                            .setDeviceAddress(deviceId)
                            .build()),
                    new ScanSettings.Builder()
                            .setNumOfMatches(1)
                            .setScanMode(SCAN_MODE_LOW_LATENCY)
                            .build(),
                    listener);

            return mixInCancel(future, scanner, listener);
        } else {
            future.completeExceptionally(new IllegalStateException("Unable to get BluetoothAdapter"));
        }

        return future;
    }

    /**
     *  Retrieves an adapter instance or asks to enable bluetooth
     */
    private Optional<BluetoothAdapter> getBluetoothAdapter() {
        final BluetoothManager mgr = context.getSystemService(BluetoothManager.class);
        final BluetoothAdapter adapter = mgr.getAdapter();

        // TODO: We can send early replies using mgr.getConnectedDevices(GATT)

        if (adapter == null || ! adapter.isEnabled()) {
            Log.w(Coxswain.TAG, "Bluetooth disabled or unavailable");
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
            return Optional.empty();
        //} else if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        //    Log.w(Coxswain.TAG, "Bluetooth doesn't have LE features");
        //    // TODO: Report LE not available
        //    return Optional.empty();
        } else {
            return Optional.of(adapter);
        }
    }

    /**
     *  Allows a search for a single device (using find()) to be cancelled.
     */
    private <T> CompletableFuture<T> mixInCancel(final CompletableFuture<T> future, final BluetoothLeScanner scanner, final ScanCallback listener) {
        final CompletableFuture<T> ret = new CompletableFuture<T>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    super.cancel(mayInterruptIfRunning);
                    scanner.stopScan(listener);
                    return true;
                }
            };
        future.whenCompleteAsync(new BiConsumer<T, Throwable>() {
            @Override
            public void accept(T dev, Throwable exception) {
                if (dev != null) {
                    ret.complete(dev);
                } else {
                    ret.completeExceptionally(exception);
                }
            }
        });

        return ret;
    }

    private static class OnDevice extends ScanCallback implements Handler.Callback {
        private final BluetoothHeartDiscoveryListener handler;
        private final Handler toBluetoothService = new Handler(this);

        public OnDevice(final BluetoothHeartDiscoveryListener handler) {
            this.handler = handler;
        }

        /**
         *  Called on main thread
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (Thread.currentThread().getName().equals("main")) {
                new DiscoveryMessage(callbackType, result, toBluetoothService).send();
            } else {
                onScanResultImpl(callbackType, result);
            }
        }

        public void onScanResultImpl(int callbackType, ScanResult result) {
            Log.i(Coxswain.TAG, "Handling scan-result on " + Thread.currentThread().getName());
            switch (callbackType) {
                case CALLBACK_TYPE_ALL_MATCHES:
                case CALLBACK_TYPE_FIRST_MATCH:
                    final byte[] heartRate = result.getScanRecord().getServiceData(BluetoothHeartServices.HEART_RATE.getParcelUuid());
                    handler.onDiscovered(result.getDevice(), result.getRssi(), heartRate != null);
                    break;
                case CALLBACK_TYPE_MATCH_LOST:
                    handler.onLost(result.getDevice().getAddress(), result.getDevice().getName());
                    break;
                default:
                    Log.e(Coxswain.TAG, "Ignoring unexpected callback-type " + callbackType);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                final byte[] heartRate = result.getScanRecord().getServiceData(BluetoothHeartServices.HEART_RATE.getParcelUuid());
                handler.onDiscovered(result.getDevice(), result.getRssi(), heartRate != null);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }

        @Override
        public boolean handleMessage(Message msg) {
            final DiscoveryMessage mess = new DiscoveryMessage(msg);
            onScanResultImpl(mess.getCallbackType(), mess.getScanResult());
            return true;
        }

        private class DiscoveryMessage {
            private static final String RESULTS = "results";
            private final Message message;

            public DiscoveryMessage(final int callbackType, final ScanResult result, final Handler receiver) {
                message = receiver.obtainMessage();
                message.arg1 = 0;
                message.arg2 = callbackType;
                final Bundle data = new Bundle(1);
                data.putParcelable(RESULTS, result);
                message.setData(data);
            }

            public DiscoveryMessage(final Message message) {
                this.message = message;
            }

            public void send() {
                message.sendToTarget();
            }

            public int getCallbackType() {
                return message.arg2;
            }

            public ScanResult getScanResult() {
                return message.getData().getParcelable(RESULTS);
            }
        }
    }

    public static class ScanDestroyer implements Destroyable {
        private final BluetoothLeScanner scanner;
        private final OnDevice handler;

        public ScanDestroyer(final BluetoothLeScanner scanner, final OnDevice handler) {
            this.scanner = scanner;
            this.handler = handler;
        }

        public void destroy() {
            scanner.stopScan(handler);
        }
    }
}
