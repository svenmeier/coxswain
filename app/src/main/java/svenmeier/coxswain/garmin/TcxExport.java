package svenmeier.coxswain.garmin;

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
import svenmeier.coxswain.Export;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

/**
 */
public class TcxExport implements Runnable, Export {

	public static final String SUFFIX = ".tcx";

	private Context context;

	private Handler handler = new Handler();

	private final Gym gym;

	private Workout workout;

	public TcxExport(Context context) {
		this.context = context;

		this.handler = new Handler();

		this.gym = Gym.instance(context);
	}

	@Override
	public void start(Workout workout) {
		this.workout = workout;

		new Thread(this).start();
	}

	@Override
	public void onRequestResult(int resultCode) {
	}

	private void toast(final String text) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, text, Toast.LENGTH_LONG).show();
			}
		});
	}

	public String getFileName() {
		StringBuilder name = new StringBuilder();

		name.append(workout.name.get());
		name.append('-');
		name.append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(workout.start.get()));
		name.append(SUFFIX);

		return name.toString();
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

		toast(String.format(context.getString(R.string.garmin_export_finished), file.getAbsolutePath()));
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
