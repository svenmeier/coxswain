package svenmeier.coxswain.google;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import propoid.db.Match;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Export;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

/**
 */
public class FitExport implements Export {

	private Activity activity;

	private final int requestCode;

	private Handler handler = new Handler();

	private final Gym gym;

	private Workout workout;

	private Connection connection;

	public FitExport(Activity activity, int requestCode) {
		this.activity = activity;
		this.requestCode = requestCode;

		this.handler = new Handler();

		this.gym = Gym.instance(activity);
	}

	@Override
	public void start(Workout workout) {
		this.workout = workout;

		connection = new Connection();
	}

	@Override
	public void onResult(int resultCode) {
		if (connection != null) {
			connection.onResult(resultCode);
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

	private class Connection implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, Runnable {

		private final GoogleApiClient client;

		public Connection() {
			client = new GoogleApiClient.Builder(activity)
					.addApi(Fitness.SESSIONS_API)
					.addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
					.addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
					.addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.build();

			client.connect();
		}

		@Override
		public void onConnectionFailed(ConnectionResult result) {
			if (result.hasResolution()) {
				try {
					result.startResolutionForResult(activity, requestCode);
				} catch (IntentSender.SendIntentException e) {
					Log.e(Coxswain.TAG, "export failed", e);
					toast(activity.getString(R.string.export_failed));
				}
			} else {
				// Show the localized error dialog
				GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
						activity, 0, null).show();
			}
		}

		public void onResult(int resultCode) {
			if (resultCode == Activity.RESULT_OK) {
				if (client.isConnecting() == false && client.isConnected() == false) {
					client.connect();
				}
			}
		}

		@Override
		public void onConnected(Bundle bundle) {
			new Thread(this).start();
		}

		@Override
		public void onConnectionSuspended(int i) {
		}

		@Override
		public void run() {
			toast(String.format(activity.getString(R.string.export_starting), "Google Fit"));

			List<Snapshot> snapshots = gym.getSnapshots(workout).list();
			try {

				SessionInsertRequest request = new Workout2Fit().request(workout, snapshots);

				Status status = Fitness.SessionsApi.insertSession(client, request).await(1,	TimeUnit.MINUTES);
				if (status.isSuccess()) {
					toast(String.format(activity.getString(R.string.export_finished), "Google Fit"));
				} else {
					Log.e(Coxswain.TAG, "export failed " + status);
					toast(activity.getString(R.string.export_failed));
				}
			} finally {
				snapshots.clear();

				client.disconnect();
			}
		}
	}
}