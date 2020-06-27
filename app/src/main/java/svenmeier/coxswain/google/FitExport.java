package svenmeier.coxswain.google;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.io.Export;

/**
 */
public class FitExport extends Export<Workout> {

	private Handler handler = new Handler();

	private final Gym gym;

	private Workout workout;

	private Connection connection;

	public FitExport(Context context) {
		super(context);

		this.handler = new Handler();

		this.gym = Gym.instance(this.context);
	}

	@Override
	public void start(Workout workout, boolean automatic) {
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

	private class Connection implements Runnable, OnCompleteListener<Void>, OnFailureListener, OnSuccessListener<Void> {

		private final int REQUEST_CODE = 1;

		private final Workout2Fit workout2Fit;

		private final GoogleSignInAccount account;

		public Connection() {
			workout2Fit = new Workout2Fit();

			account = GoogleSignIn.getLastSignedInAccount(context);

			FitnessOptions.Builder builder = FitnessOptions.builder();
			for (Workout2Fit.Mapper mapper : workout2Fit.mappers()) {
				builder.addDataType(mapper.type(), FitnessOptions.ACCESS_WRITE);
			}

			FitnessOptions fitnessOptions = builder.build();
			if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
				if (context instanceof Activity) {
					GoogleSignIn.requestPermissions((Activity)context, REQUEST_CODE, account, fitnessOptions);
				} else {
					toast(context.getString(R.string.googlefit_export_permissions_manual));
				}
			} else {
				new Thread(this).start();
			}
		}

		/**
		 * TODO activity should delegate results
		 */
		public void onResult(int requestCode, int resultCode) {
			if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			}
		}

		@Override
		public void run() {
			toast(context.getString(R.string.googlefit_export_starting));
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

			List<Snapshot> snapshots = gym.getSnapshots(workout).list();

			try {

				SessionInsertRequest.Builder builder = new SessionInsertRequest.Builder()
						.setSession(workout2Fit.session(workout));
				for (Workout2Fit.Mapper mapper : workout2Fit.mappers()) {
					builder.addDataSet(mapper.dataSet(workout, snapshots));
				}

				SessionInsertRequest request = builder.build();

				Fitness.getSessionsClient(context, account)
						.insertSession(request)
						.addOnFailureListener(this)
						.addOnSuccessListener(this)
						.addOnCompleteListener(this);
			} catch (Exception ex) {
				Log.e(Coxswain.TAG, "googlefit failed",  ex);

				toast(context.getString(R.string.googlefit_export_failed));
			} finally {
				snapshots.clear();
			}
		}

		@Override
		public void onFailure(@NonNull Exception ex) {
			Log.e(Coxswain.TAG, "googlefit failed",  ex);

			toast(context.getString(R.string.googlefit_export_failed));
		}

		@Override
		public void onSuccess(Void aVoid) {
			toast(context.getString(R.string.googlefit_export_finished));
		}

		@Override
		public void onComplete(@NonNull Task<Void> task) {
		}
	}
}