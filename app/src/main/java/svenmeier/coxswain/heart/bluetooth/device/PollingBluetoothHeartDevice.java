package svenmeier.coxswain.heart.bluetooth.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Heart;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartCharacteristics;
import svenmeier.coxswain.heart.bluetooth.constants.GattHeartRateMeasurement;
import svenmeier.coxswain.util.Destroyable;

@RequiresApi(api = Build.VERSION_CODES.N)
public class PollingBluetoothHeartDevice extends AbstractBluetoothHeartDevice {
    final AtomicInteger lastReading = new AtomicInteger(Heart.UNKNOWN_READING);

    public PollingBluetoothHeartDevice(Context context, BluetoothDevice delegate) {
        super(context, delegate);
    }

    public Future<Boolean> canUseBluetoothNotifications() {
        return queryNotificationSupport(BluetoothHeartCharacteristics.HEART_RATE_MEASUREMENT)
                .handle(this::bytesToBoolean);
    }

    @Override
    public Destroyable watch(final Consumer<Integer> heartRateConsumer) {
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final AtomicBoolean keepGoing = new AtomicBoolean(true);
        executor.execute(makeWatcher(heartRateConsumer, keepGoing));

        return new Destructor(executor, keepGoing);
    }

    private Runnable makeWatcher(final Consumer<Integer> heartRateConsumer, final AtomicBoolean keepGoing) {
        return () -> {
            while (keepGoing.get()) {
                try {
                    Thread.sleep(1000);
                    heartRateConsumer.accept(getNextHeartRateReading().get().getHeartBpm());
                } catch (Exception e) {
                    Log.e(Coxswain.TAG, "Error watching heart-rate", e);
                }
            }
        };
    }

    public Future<GattHeartRateMeasurement> getNextHeartRateReading() {
        return query(BluetoothHeartCharacteristics.HEART_RATE_MEASUREMENT)
                .handle((val, exc) -> new GattHeartRateMeasurement(val));
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    private class Destructor implements Destroyable {
        private final AtomicBoolean keepGoing;
        private final ExecutorService executor;

        public Destructor(ExecutorService executor, AtomicBoolean keepGoing) {
            this.executor = executor;
            this.keepGoing = keepGoing;
        }

        @Override
        public void destroy() {
            keepGoing.set(false);
            executor.shutdown();
            PollingBluetoothHeartDevice.this.destroy();
        }
    }
}
