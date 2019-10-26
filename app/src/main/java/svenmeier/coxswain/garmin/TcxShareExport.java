package svenmeier.coxswain.garmin;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;

import propoid.db.Match;
import propoid.util.content.Preference;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.io.Export;
import svenmeier.coxswain.util.PermissionBlock;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 */
public class TcxShareExport extends TcxExport {

	public TcxShareExport(Context context) {
		super(context);
	}

	@Override
	protected void onWritten(File file) {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/xml");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
		setFile(context, file, shareIntent);

		if (automatic) {
			String sharePackage = Preference.getString(context, R.string.preference_export_tcx_share_package).get();
			if (sharePackage != null) {
				shareIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
				shareIntent.setPackage(sharePackage);

				context.startActivity(shareIntent);
				return;
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
			IntentSender sender = PendingIntent.getBroadcast(context, 0, ShareReceiver.newIntent(context), PendingIntent.FLAG_UPDATE_CURRENT).getIntentSender();

			Intent chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.garmin_export), sender);
			chooserIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(chooserIntent);
		} else {
			context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.garmin_export)));
		}
	}
}
