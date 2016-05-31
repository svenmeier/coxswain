package svenmeier.coxswain.google;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
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

	private long timestamp(Workout workout, int seconds) {
		return workout.start.get() + (seconds * 1000);
	}

	public Session session(Workout workout) {

		return new Session.Builder()
				.setName(workout.name.get())
				.setIdentifier(dateFormat.format(workout.start.get()))
				.setActivity(FitnessActivities.ROWING_MACHINE)
				.setStartTime(workout.start.get(), TimeUnit.MILLISECONDS)
				.setEndTime(timestamp(workout, workout.duration.get()), TimeUnit.MILLISECONDS)
				.build();
	}

	public Iterable<DataSet> dataSets(final Workout workout, final List<Snapshot> snapshots) {
		return new Iterable<DataSet>() {
			@Override
			public Iterator<DataSet> iterator() {
				return new Iterator<DataSet>() {
					private List<Mapper> mappers = new ArrayList<>();

					{
					    mappers.add(new DistanceCumulative());
						mappers.add(new StepCountCadence());
						mappers.add(new Speed());
						mappers.add(new HeartRateBpm());
					}

					@Override
					public boolean hasNext() {
						return mappers.isEmpty() == false;
					}

					@Override
					public DataSet next() {
						Mapper mapper = mappers.remove(mappers.size() - 1);

						DataSource dataSource = new DataSource.Builder()
								.setType(DataSource.TYPE_RAW)
								.setDataType(mapper.type())
								.setAppPackageName(BuildConfig.APPLICATION_ID)
								.build();

						DataSet dataSet = DataSet.create(dataSource);

						for (int index = 0; index < snapshots.size(); index++) {
							Snapshot snapshot = snapshots.get(index);

							DataPoint point = dataSet.createDataPoint();
							point.setTimestamp(timestamp(workout, index), TimeUnit.MILLISECONDS);
							mapper.set(snapshot, point);
							dataSet.add(point);
						}

						return dataSet;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	private interface Mapper {

		DataType type();

		void set(Snapshot snapshot, DataPoint point);
	}

	private class DistanceCumulative implements Mapper {
		@Override
		public DataType type() {
			return DataType.TYPE_DISTANCE_CUMULATIVE;
		}

		@Override
		public void set(Snapshot snapshot, DataPoint point) {
			point.setFloatValues(snapshot.distance.get());
		}
	}

	private class StepCountCadence implements Mapper {
		@Override
		public DataType type() {
			return DataType.TYPE_STEP_COUNT_CADENCE;
		}

		@Override
		public void set(Snapshot snapshot, DataPoint point) {
			point.setFloatValues(snapshot.strokeRate.get() / 100f);
		}
	}

	private class Speed implements Mapper {
		@Override
		public DataType type() {
			return DataType.TYPE_SPEED;
		}

		@Override
		public void set(Snapshot snapshot, DataPoint point) {
			point.setFloatValues(snapshot.speed.get() / 100);
		}
	}

	private class HeartRateBpm implements Mapper {
		@Override
		public DataType type() {
			return DataType.TYPE_HEART_RATE_BPM;
		}

		@Override
		public void set(Snapshot snapshot, DataPoint point) {
			point.setFloatValues(snapshot.pulse.get());
		}
	}
}