package svenmeier.coxswain.garmin;

import android.Manifest;
import android.app.Activity;
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
import java.text.SimpleDateFormat;

import propoid.db.Match;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Export;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.util.PermissionBlock;

/**
 */
public class TcxExport implements Export {

	public static final String SUFFIX = ".tcx";

	private final boolean share;

	private Context context;

	private Handler handler = new Handler();

	private final Gym gym;

	public TcxExport(Context context, boolean share) {
		this.context = context;

		this.share = share;

		this.handler = new Handler();

		this.gym = Gym.instance(context);
	}

	@Override
	public void start(Workout workout) {
		new Writer(workout);
	}

	private class Writer extends PermissionBlock implements Runnable {

		private final Workout workout;

		public Writer(Workout workout) {
			super(context);

			this.workout = workout;

			acquirePermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}

		@Override
		protected void onPermissionsApproved() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			toast(context.getString(R.string.garmin_export_starting));

			Match<Snapshot> snapshots = gym.getSnapshots(workout);

			File file;
			try {
				file = write(snapshots);
			} catch (IOException e) {
				Log.e(Coxswain.TAG, "export failed", e);
				toast(context.getString(R.string.garmin_export_failed));
				return;
			}

			// input media so file can be found via MTB
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

			if (share) {
				share(file);
			} else {
				toast(String.format(context.getString(R.string.garmin_export_finished), file.getAbsolutePath()));
			}
		}

		private void share(File file) {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			Uri uri = Uri.fromFile(file);
			shareIntent.setType("text/xml");
			shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, file.getName());

			context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.garmin_export)));
		}

		public String getFileName() {
			StringBuilder name = new StringBuilder();

			name.append(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(workout.start.get()));
			name.append('_');
			name.append(workout.name("UNKNOWN"));
			name.append(SUFFIX);

			return name.toString();
		}

		private File write(Match<Snapshot> snapshots) throws IOException {
			File dir = Environment.getExternalStoragePublicDirectory(Coxswain.TAG);
			dir.mkdirs();
			dir.setReadable(true, false);

			File file = new File(dir, getFileName());

			java.io.Writer writer = new BufferedWriter(new FileWriter(file));
			try {
				new Workout2TCX(writer).document(workout, snapshots.list());
			} finally {
				writer.close();
			}

			return file;
		}
	}

	private void toast(final String text) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, text, Toast.LENGTH_LONG).show();
			}
		});
	}
}
