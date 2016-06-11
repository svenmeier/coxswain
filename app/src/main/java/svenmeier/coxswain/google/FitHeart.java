package svenmeier.coxswain.google;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.concurrent.TimeUnit;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.R;

/**
 */
public class FitHeart {

	private static final DataType TYPE = DataType.TYPE_HEART_RATE_BPM;

	private Connection connection;

	private Context context;

	private final int requestCode;

	public float heartRate = 0;

	public FitHeart(Context context, int requestCode) {
		this.context = context;
		this.requestCode = requestCode;
	}

	public void connect() {
		connection = new Connection();
	}

	public void disconnect() {
		if (connection != null) {
			connection.destroy();
			connection = null;
		}
	}

	public void onRequestResult(int resultCode) {
		if (connection != null) {
			connection.onRequestResult(resultCode);
		}
	}

	private class Connection implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, OnDataPointListener {

		private final GoogleApiClient client;

		public Connection() {
			client = new GoogleApiClient.Builder(context)
					.addApi(Fitness.SENSORS_API)
					.addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.build();

			client.connect();
		}

		@Override
		public void onConnectionFailed(ConnectionResult result) {
			if (context instanceof Activity) {
				Activity activity = (Activity)context;

				if (result.hasResolution()) {
					try {
						result.startResolutionForResult(activity, requestCode);
					} catch (IntentSender.SendIntentException e) {
						Log.e(Coxswain.TAG, "connection failed", e);
					}
				} else {
					// Show the localized error dialog
					GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
							activity, 0, null).show();
				}
			} else {
				Toast.makeText(context, R.string.googlefit_failed, Toast.LENGTH_LONG).show();
			}
		}

		public void onRequestResult(int resultCode) {
			if (resultCode == Activity.RESULT_OK) {
				if (client.isConnecting() == false && client.isConnected() == false) {
					client.connect();
				}
			}
		}

		@Override
		public void onConnected(@Nullable Bundle bundle) {
			Fitness.SensorsApi.findDataSources(client,
					new DataSourcesRequest.Builder()
							.setDataTypes(TYPE)
							.setDataSourceTypes(DataSource.TYPE_RAW)
							.build())
					.setResultCallback(new ResultCallback<DataSourcesResult>() {
						@Override
						public void onResult(DataSourcesResult dataSourcesResult) {
							SensorRequest request = new SensorRequest.Builder()
									.setDataType(TYPE)
									.setSamplingRate(1, TimeUnit.SECONDS)
									.build();

							Fitness.SensorsApi.add(client, request, Connection.this);
						}
					});

		}

		@Override
		public void onConnectionSuspended(int i) {
		}

		public void destroy() {
			Fitness.SensorsApi.remove(client, this);
		}

		@Override
		public void onDataPoint(DataPoint dataPoint) {
			for (Field field : dataPoint.getDataType().getFields()) {
				Value value = dataPoint.getValue(field);

				heartRate = value.asFloat();
			}
		}
	}
}
