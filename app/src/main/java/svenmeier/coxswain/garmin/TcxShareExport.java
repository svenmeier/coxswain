package svenmeier.coxswain.garmin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.io.Export;
import svenmeier.coxswain.util.PermissionBlock;

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
		setFile(context, file, shareIntent);
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());

		context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.garmin_export)));
	}
}
