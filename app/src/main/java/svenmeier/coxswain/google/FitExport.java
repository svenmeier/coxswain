package svenmeier.coxswain.google;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Export;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

/**
 */
public class FitExport implements Export {

	private Context context;

	private Handler handler = new Handler();

	private final Gym gym;

	private Workout workout;

	private Connection connection;

	public FitExport(Context context) {
		this.context = context;

		this.handler = new Handler();

		this.gym = Gym.instance(context);
	}

	@Override
	public void start(Workout workout) {
		this.workout = workout;

		connection = new Connection();
	}

	private void toast(final String text) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, text, Toast.LENGTH_LONG).show();
			}
		});
	}

	private class Connection implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, Runnable {

		private final GoogleApiClient client;

		public Connection() {
			client = new GoogleApiClient.Builder(context)
					.addApi(Fitness.SESSIONS_API)
					.addApi(Fitness.HISTORY_API)
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
			if (result.hasResolution() && context instanceof Activity) {
				try {
					result.startResolutionForResult((Activity) context, 1);
					return;
				} catch (IntentSender.SendIntentException e) {
				}
			}

			toast(context.getString(R.string.googlefit_export_failed));
		}

		/**
		 * TODO who calls this
		 */
		public void onResolutionResult(int resultCode) {
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
			toast(context.getString(R.string.googlefit_export_starting));

			List<Snapshot> snapshots = gym.getSnapshots(workout).list();
			try {
				Workout2Fit workout2Fit = new Workout2Fit();

				Status status;

				Session session = workout2Fit.session(workout);
				SessionInsertRequest insertSession = new SessionInsertRequest.Builder()
						.setSession(session)
						.build();
				status = Fitness.SessionsApi.insertSession(client, insertSession).await(1, TimeUnit.MINUTES);
				if (status.isSuccess() == false) {
					Log.e(Coxswain.TAG, "insert session failed " + status);
					toast(context.getString(R.string.googlefit_export_failed));
					return;
				}

				for (DataSet dataSet : workout2Fit.dataSets(workout, snapshots)) {
					status = Fitness.HistoryApi.insertData(client, dataSet).await(1, TimeUnit.MINUTES);
					if (status.isSuccess() == false) {
						Log.e(Coxswain.TAG, "insert dataset failed " + status);
						toast(context.getString(R.string.googlefit_export_failed));
						return;
					}
				}

				toast(context.getString(R.string.googlefit_export_finished));
			} finally {
				snapshots.clear();

				client.disconnect();
			}
		}
	}
}