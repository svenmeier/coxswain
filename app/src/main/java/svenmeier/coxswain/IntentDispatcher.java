package svenmeier.coxswain;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import svenmeier.coxswain.view.ProgramsFragment;

public class IntentDispatcher extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            GymService.start(context, intent.<UsbDevice>getParcelableExtra(UsbManager.EXTRA_DEVICE));
        } else if (GymService.ACTION_ROWING_STARTED.equals(intent.getAction())) {
            context.startActivity(new Intent(context, (ProgramsFragment.class)));
        }
    }
}
