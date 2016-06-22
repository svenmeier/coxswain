package svenmeier.coxswain.garmin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

	private Activity activity;

	private Handler handler = new Handler();

	private final Gym gym;

	public TcxExport(Activity activity) {
		this.activity = activity;

		this.handler = new Handler();

		this.gym = Gym.instance(activity);
	}

	@Override
	public void start(Workout workout) {
		new Runner(workout);
	}

	private class Runner extends PermissionBlock implements Runnable {

		private final Workout workout;

		public Runner(Workout workout) {
			super(activity);

			this.workout = workout;

			super.enter(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}

		@Override
		protected void entered() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			toast(activity.getString(R.string.garmin_export_starting));

			Match<Snapshot> snapshots = gym.getSnapshots(workout);

			File file;
			try {
				file = write(snapshots);
			} catch (IOException e) {
				Log.e(Coxswain.TAG, "export failed", e);
				toast(activity.getString(R.string.garmin_export_failed));
				return;
			}

			// input media so file can be found via MTB
			activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

			toast(String.format(activity.getString(R.string.garmin_export_finished), file.getAbsolutePath()));
		}

		public String getFileName() {
			StringBuilder name = new StringBuilder();

			name.append(workout.name.get());
			name.append('-');
			name.append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(workout.start.get()));
			name.append(SUFFIX);

			return name.toString();
		}

		private File write(Match<Snapshot> snapshots) throws IOException {
			File dir = Environment.getExternalStoragePublicDirectory(Coxswain.TAG);
			dir.mkdirs();
			dir.setReadable(true, false);

			File file = new File(dir, getFileName());

			Writer writer = new BufferedWriter(new FileWriter(file));
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
				Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
			}
		});
	}
}
