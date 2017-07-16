package svenmeier.coxswain.heart.generic.toast;

import android.content.Context;
import android.widget.Toast;

import svenmeier.coxswain.heart.generic.BatteryStatusListener;

public class ToastBatteryStatusListener implements BatteryStatusListener {
    final Context context;

    public ToastBatteryStatusListener(Context context) {
        this.context = context;
    }

    @Override
    public void onBatteryStatus(int percentageLeft) {
        if (percentageLeft < 20) {
            Toast.makeText(context, "Battery of the HRM: " + percentageLeft + "%", Toast.LENGTH_LONG).show();
        }
    }
}
