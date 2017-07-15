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

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartCharacteristics;
import svenmeier.coxswain.heart.bluetooth.reading.GattHeartRateMeasurement;
import svenmeier.coxswain.heart.bluetooth.typeconverter.CharacteristicToHeart;
import svenmeier.coxswain.heart.generic.HeartRateListener;
import svenmeier.coxswain.util.Destroyable;

@RequiresApi(api = Build.VERSION_CODES.N)
public class PollingBluetoothHeartDevice extends AbstractBluetoothHeartDevice {
    private static final int POLLING_INTERVAL_MS = 1000;

    public PollingBluetoothHeartDevice(Context context, BluetoothDevice delegate) {
        super(context, delegate);
    }

    public Future<Boolean> canUseBluetoothNotifications() {
        return queryNotificationSupport(BluetoothHeartCharacteristics.HEART_RATE_MEASUREMENT);
    }

    @Override
    public Destroyable watch(final HeartRateListener heartRateConsumer) {
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final AtomicBoolean keepGoing = new AtomicBoolean(true);
        executor.execute(makeWatcher(heartRateConsumer, keepGoing));

        return new Destructor(executor, keepGoing);
    }

    private Runnable makeWatcher(final HeartRateListener heartRateConsumer, final AtomicBoolean keepGoing) {
        return new Runnable() {
            @Override
            public void run() {
                while (keepGoing.get()) {
                    try {
                        Thread.sleep(POLLING_INTERVAL_MS);
                        heartRateConsumer.onHeartRate(getNextHeartRateReading().get().getHeartBpm());
                    } catch (Exception e) {
                        Log.e(Coxswain.TAG, "Error watching heart-rate", e);
                    }
                }
            }
        };
    }

    public Future<GattHeartRateMeasurement> getNextHeartRateReading() {
        return query(BluetoothHeartCharacteristics.HEART_RATE_MEASUREMENT)
                .handle(CharacteristicToHeart.INSTANCE);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    class Destructor implements Destroyable {
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
