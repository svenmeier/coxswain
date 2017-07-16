package svenmeier.coxswain.heart.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Heart;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.heart.generic.ConnectionStatus;
import svenmeier.coxswain.heart.generic.HeartRateCommunication;
import svenmeier.coxswain.heart.generic.toast.ToastBatteryStatusListener;
import svenmeier.coxswain.heart.generic.toast.ToastConnectionStatusListener;
import svenmeier.coxswain.heart.generic.communication.HandlerCommunication;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 *  Provides the new implementation in the format of the previous Heart-readings
 */
@SuppressWarnings("unused") // Initialized through reflection
public class BluetoothHeartAdapter extends Heart {
    private final HeartServiceConnection heartService;
    private final HeartRateCommunication communication;

    public BluetoothHeartAdapter(Context uiContext, Measurement measurement) {
        super(uiContext, measurement);

        heartService = new HeartServiceConnection();

        registerConnectionStatusListener(new ToastConnectionStatusListener(uiContext));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Log.i(Coxswain.TAG, "Contacting bluetooth heart service...");
            //communication = new BroadcastCommunication();   // TODO: DI this
            communication = new HandlerCommunication();
            final HeartRateCommunication.Reader listener = communication.makeReader(
                    new ToastBatteryStatusListener(uiContext),
                    new ToastConnectionStatusListener(uiContext),
                    this);
            listener.bind(uiContext);
            heartService.bind(uiContext, communication);
        } else {
            Log.w(Coxswain.TAG, "New bluetooth unavailable: Needs Nougat!");
            updateConnectionStatus(ConnectionStatus.UNAVAILABLE_ON_SYSTEM, null, "Requires Android Nougat");
            communication = null;
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void destroy() {
        if (heartService != null) {
            heartService.unbind();
        }
        if (communication != null) {
            communication.destroy();
        }
        super.destroy();
    }

    private static class HeartServiceConnection implements ServiceConnection {
        private BluetoothHeartService service = null;
        private HeartRateCommunication communication;

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            this.service = ((BluetoothHeartService.LocalBinder) binder).getService();
            this.service.initialize(communication.makeWriter());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.service = null;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public void bind(final Context context, final HeartRateCommunication communication) {
            this.communication = communication;
            final Intent intent = new Intent(context, BluetoothHeartService.class);
            if (! context.bindService(intent, this, BIND_AUTO_CREATE)) {
                Log.e(Coxswain.TAG, "Unable to bind to " + BluetoothHeartService.class.getSimpleName());
            }
        }

        public void unbind() {
            if (service != null) {
                service.unbindService(this);
            }
            service = null;
        }

        @Nullable
        public BluetoothHeartService getService() {
            return service;
        }
    }
}
