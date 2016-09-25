package svenmeier.coxswain.google;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;

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
				.setName(workout.name("UNKNOWN"))
				.setIdentifier(dateFormat.format(workout.start.get()))
				.setActivity(FitnessActivities.ROWING_MACHINE)
				.setStartTime(timestamp(workout, 0), TimeUnit.MILLISECONDS)
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
						mappers.add(new CaloriesExpended());
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

						return mapper.dataSet(workout, snapshots);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	private abstract class Mapper {

		public DataSet dataSet(Workout workout, List<Snapshot> snapshots) {
			DataSource dataSource = new DataSource.Builder()
					.setType(DataSource.TYPE_RAW)
					.setDataType(type())
					.setAppPackageName(BuildConfig.APPLICATION_ID)
					.build();

			DataSet dataSet = DataSet.create(dataSource);

			map(dataSet, workout, snapshots);

			return dataSet;
		}

		protected abstract DataType type();

		protected abstract void map(DataSet dataSet, Workout workout, List<Snapshot> snapshots);
	}

	private abstract class AbstractSnapshotMapper extends Mapper {

		@Override
		protected void map(DataSet dataSet, Workout workout, List<Snapshot> snapshots) {
			for (int index = 0; index < snapshots.size(); index++) {
				Snapshot snapshot = snapshots.get(index);

				DataPoint point = dataSet.createDataPoint();
				point.setTimestamp(timestamp(workout, index), TimeUnit.MILLISECONDS);
				map(snapshot, point);
				dataSet.add(point);
			}
		}

		protected abstract void map(Snapshot snapshot, DataPoint point);
	}

	private class StepCountCadence extends AbstractSnapshotMapper {
		@Override
		public DataType type() {
			return DataType.TYPE_STEP_COUNT_CADENCE;
		}

		@Override
		public void map(Snapshot snapshot, DataPoint point) {
			point.setFloatValues(snapshot.strokeRate.get() / 100f);
		}
	}

	private class Speed extends AbstractSnapshotMapper {
		@Override
		public DataType type() {
			return DataType.TYPE_SPEED;
		}

		@Override
		public void map(Snapshot snapshot, DataPoint point) {
			point.setFloatValues(snapshot.speed.get() / 100f);
		}
	}

	private class HeartRateBpm extends AbstractSnapshotMapper {
		@Override
		public DataType type() {
			return DataType.TYPE_HEART_RATE_BPM;
		}

		@Override
		public void map(Snapshot snapshot, DataPoint point) {
			point.setFloatValues((float)snapshot.pulse.get());
		}
	}

	private abstract class AbstractWorkoutMapper extends Mapper {

		@Override
		protected void map(DataSet dataSet, Workout workout, List<Snapshot> snapshots) {
			DataPoint point = dataSet.createDataPoint();
			point.setTimeInterval(timestamp(workout, 0), timestamp(workout, workout.duration.get()), TimeUnit.MILLISECONDS);
			map(workout, point);
			dataSet.add(point);
		}

		public abstract void map(Workout workout, DataPoint point);
	}

	private class DistanceCumulative extends AbstractWorkoutMapper {

		@Override
		public DataType type() {
			return DataType.AGGREGATE_DISTANCE_DELTA;
		}

		@Override
		public void map(Workout workout, DataPoint point) {

			point.setFloatValues(workout.distance.get());
		}
	}

	private class CaloriesExpended extends AbstractWorkoutMapper {

		@Override
		public DataType type() {
			return DataType.AGGREGATE_CALORIES_EXPENDED;
		}

		@Override
		public void map(Workout workout, DataPoint point) {

			point.setFloatValues(workout.energy.get());
		}
	}
}