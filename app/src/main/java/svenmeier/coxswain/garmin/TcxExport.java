package svenmeier.coxswain.garmin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.UiThread;
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

/**
 */
public class TcxExport extends Export<Workout> {

	public static final String SUFFIX = ".tcx";

	private Handler handler = new Handler();

	private final Gym gym;

	private Preference<Boolean> generateLocations;

	public TcxExport(Context context) {
		super(context.getApplicationContext());

		this.handler = new Handler();

		this.gym = Gym.instance(context);

		this.generateLocations = Preference.getBoolean(context, R.string.preference_export_track);
	}

	@Override
	public void start(Workout workout) {
		new Writing(workout);
	}

	@UiThread
	protected void onWritten(File file) {
		Toast.makeText(context, String.format(context.getString(R.string.garmin_export_finished), file.getAbsolutePath()), Toast.LENGTH_LONG).show();
	}

	private class Writing extends PermissionBlock implements Runnable {

		private final Workout workout;

		public Writing(Workout workout) {
			super(context);

			this.workout = workout;

			acquirePermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}

		@Override
		protected void onRejected() {
			toast(context.getString(R.string.garmin_export_failed));
		}

		@Override
		protected void onPermissionsApproved() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			toast(context.getString(R.string.garmin_export_starting));

			Match<Snapshot> snapshots = gym.getSnapshots(workout);

			final File file;
			try {
				file = write(snapshots);
			} catch (IOException e) {
				Log.e(Coxswain.TAG, "export failed", e);
				toast(context.getString(R.string.garmin_export_failed));
				return;
			}

			// input media so file can be found via MTB
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

			handler.post(new Runnable() {
				@Override
				public void run() {
					onWritten(file);
				}
			});
		}

		public String getFileName() {
			StringBuilder name = new StringBuilder();

			name.append(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(workout.start.get()));
			name.append('_');
			name.append(workout.programName("UNKNOWN").replaceAll("[_\\/]", " "));
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
				Workout2TCX workout2TCX = new Workout2TCX(writer);

				if (generateLocations.get()) {
					workout2TCX.track(new ArtificialTrack(workout.location.get()));
				}

				workout2TCX.document(workout, snapshots.list());
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
