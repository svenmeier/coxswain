package svenmeier.coxswain.garmin;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import propoid.util.content.Preference;
import svenmeier.coxswain.R;

import static android.content.Intent.EXTRA_CHOSEN_COMPONENT;

/**
 * See {@link TcxShareExport}.
 */
public class ShareReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName componentName = intent.getParcelableExtra(EXTRA_CHOSEN_COMPONENT);

        Preference.getString(context, R.string.preference_export_tcx_share_package).set(componentName.getPackageName());
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, ShareReceiver.class);
    }
}
