package svenmeier.coxswain.google;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import svenmeier.coxswain.BuildConfig;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

/**
 */
public class Workout2Fit {

	private SimpleDateFormat dateFormat;

	public Workout2Fit() {
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private DataSet dataSet(DataType type) {
		DataSource dataSource = new DataSource.Builder()
				.setType(DataSource.TYPE_RAW)
				.setDataType(type)
				.setAppPackageName(BuildConfig.APPLICATION_ID)
				.build();

		return DataSet.create(dataSource);
	}

	private long timestamp(Workout workout, int seconds) {
		return workout.start.get() + (seconds * 1000);
	}

	public SessionInsertRequest request(Workout workout, List<Snapshot> snapshots) {

		Session session = new Session.Builder()
				.setName(workout.name.get())
				.setIdentifier(dateFormat.format(workout.start.get()))
				.setActivity(FitnessActivities.ROWING_MACHINE)
				.setStartTime(workout.start.get(), TimeUnit.MILLISECONDS)
				.setEndTime(timestamp(workout, workout.duration.get()), TimeUnit.MILLISECONDS)
				.build();

		SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
				.setSession(session)
				.addDataSet(distanceCumulative(workout, snapshots))
				.addDataSet(stepCountCadence(workout, snapshots))
				.addDataSet(speed(workout, snapshots))
				.addDataSet(heartRateBpm(workout, snapshots))
				.build();

		return insertRequest;
	}

	private DataSet distanceCumulative(Workout workout, List<Snapshot> snapshots) {
		DataSet dataSet = dataSet(DataType.TYPE_DISTANCE_CUMULATIVE);

		for (int index = 0; index < snapshots.size(); index++) {
			Snapshot snapshot = snapshots.get(index);

			DataPoint dataPoint = dataSet.createDataPoint();
			dataPoint.setTimestamp(timestamp(workout, index), TimeUnit.MILLISECONDS);
			dataPoint.setFloatValues(snapshot.distance.get());
			dataSet.add(dataPoint);
		}

		return dataSet;
	}

	private DataSet stepCountCadence(Workout workout, List<Snapshot> snapshots) {
		DataSet dataSet = dataSet(DataType.TYPE_STEP_COUNT_CADENCE);

		for (int index = 0; index < snapshots.size(); index++) {
			Snapshot snapshot = snapshots.get(index);

			DataPoint dataPoint = dataSet.createDataPoint();
			dataPoint.setTimestamp(timestamp(workout, index), TimeUnit.MILLISECONDS);
			dataPoint.setFloatValues(snapshot.strokeRate.get() / 100);
			dataSet.add(dataPoint);
		}

		return dataSet;
	}

	private DataSet speed(Workout workout, List<Snapshot> snapshots) {
		DataSet dataSet = dataSet(DataType.TYPE_SPEED);

		for (int index = 0; index < snapshots.size(); index++) {
			Snapshot snapshot = snapshots.get(index);

			DataPoint dataPoint = dataSet.createDataPoint();
			dataPoint.setTimestamp(timestamp(workout, index), TimeUnit.MILLISECONDS);
			dataPoint.setFloatValues(snapshot.speed.get() / 100);
			dataSet.add(dataPoint);
		}

		return dataSet;
	}

	private DataSet heartRateBpm(Workout workout, List<Snapshot> snapshots) {
		DataSet dataSet = dataSet(DataType.TYPE_HEART_RATE_BPM);

		for (int index = 0; index < snapshots.size(); index++) {
			Snapshot snapshot = snapshots.get(index);

			DataPoint dataPoint = dataSet.createDataPoint();
			dataPoint.setTimestamp(timestamp(workout, index), TimeUnit.MILLISECONDS);
			dataPoint.setFloatValues(snapshot.pulse.get());
			dataSet.add(dataPoint);
		}

		return dataSet;
	}
}
