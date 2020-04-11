package svenmeier.coxswain.garmin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.UiThread;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
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

    protected boolean automatic;

    public TcxExport(Context context) {
		super(context.getApplicationContext());

		this.handler = new Handler();

		this.gym = Gym.instance(context);
	}

	@Override
	public void start(Workout workout, boolean automatic) {

	    this.automatic = automatic;

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
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

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
				Workout2TCX workout2TCX = new Workout2TCX(writer, getCourse(workout));

				workout2TCX.document(workout, snapshots.list());
			} finally {
				writer.close();
			}

			return file;
		}
	}

	private ICourse getCourse(Workout workout) {
		ICourse course = new StationaryCourse(workout.location.get());

		if (Preference.getBoolean(context, R.string.preference_export_track).get()) {
			InputStream input;
			try {
				input = new FileInputStream(new File(Environment.getExternalStoragePublicDirectory(Coxswain.TAG), "course.tcx"));
			} catch (Exception ex) {
				input = context.getResources().openRawResource(R.raw.course);
			}
			
			try {
				TCX2Course tcx2Course = new TCX2Course(new InputStreamReader(input, Charset.forName("UTF-8")));
				tcx2Course.course();

				course = tcx2Course.getCourse();
				
				input.close();
			} catch (Exception ex) {
				Log.e(Coxswain.TAG, "cannot read course", ex);
				toast(context.getString(R.string.garmin_export_track_course_unavailable));
			}
		}

		return course;
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
