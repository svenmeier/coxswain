package svenmeier.coxswain.garmin;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.io.Import;

/**
 */
public class TcxImport implements Import<Workout> {

	private Context context;

	private Handler handler = new Handler();

	private final Gym gym;

	public TcxImport(Context context) {
		this.context = context;

		this.handler = new Handler();

		this.gym = Gym.instance(context);
	}

	public void start(Uri uri) {
		new Reading(uri);
	}

	private class Reading implements Runnable {

		private final Uri uri;

		public Reading(Uri uri) {
			this.uri = uri;

			new Thread(this).start();
		}

		@Override
		public void run() {
			toast(context.getString(R.string.garmin_import_starting));

			try {
				write();
			} catch (Exception e) {
				Log.e(Coxswain.TAG, "export failed", e);
				toast(context.getString(R.string.garmin_import_failed));
				return;
			}

			toast(String.format(context.getString(R.string.garmin_import_finished)));
		}

		private void write() throws IOException, ParseException {

			Pair<Workout, List<Snapshot>> pair;

			Reader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(uri)));
			try {
				pair = new TCX2Workout(reader).workout();
			} finally {
				reader.close();
			}

			gym.add(pair.first, pair.second);
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
