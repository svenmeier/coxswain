package svenmeier.coxswain.google;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import svenmeier.coxswain.BuildConfig;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

/**
 */
public class Workout2Fit {

	public static final int MAX_DATAPOINTS = 1000;

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
				.setName(workout.programName("UNKNOWN"))
				.setIdentifier(dateFormat.format(workout.start.get()))
				.setActivity(FitnessActivities.ROWING_MACHINE)
				.setStartTime(timestamp(workout, 0), TimeUnit.MILLISECONDS)
				.setEndTime(timestamp(workout, workout.duration.get()), TimeUnit.MILLISECONDS)
				.build();
	}

	public Collection<Mapper> mappers() {
		List<Mapper> mappers = new ArrayList<>();

		mappers.add(new AggregateCaloriesExpended());
		mappers.add(new AggregateDistanceDelta());
		mappers.add(new Speed());
		mappers.add(new HeartRateBpm());
		mappers.add(new Power());

		return mappers;
	}

	public abstract class Mapper {

		public DataSet dataSet(Workout workout, List<Snapshot> snapshots) {
			DataSource dataSource = new DataSource.Builder()
					.setType(DataSource.TYPE_RAW)
					.setDataType(type())
					.setAppPackageName(BuildConfig.APPLICATION_ID)
					.build();

			DataSet.Builder dataSet = DataSet.builder(dataSource);

			map(dataSource, dataSet, workout, snapshots);

			return dataSet.build();
		}

		public abstract DataType type();

		protected abstract void map(DataSource dataSource, DataSet.Builder dataSet, Workout workout, List<Snapshot> snapshots);
	}

	private abstract class AbstractSnapshotMapper extends Mapper {

		@Override
		protected void map(DataSource dataSource, DataSet.Builder dataSet, Workout workout, List<Snapshot> snapshots) {

			int size = snapshots.size();
			for (int i = 0; i < Math.min(size, MAX_DATAPOINTS); i++) {
				int index;
				if (size < MAX_DATAPOINTS) {
					index = i;
				} else {
					index = (size * i / MAX_DATAPOINTS);
				}
				Snapshot snapshot = snapshots.get(index);

				DataPoint.Builder pointr = DataPoint.builder(dataSource);
				pointr.setTimestamp(timestamp(workout, index), TimeUnit.MILLISECONDS);
				map(snapshot, pointr);
				dataSet.add(pointr.build());
			}
		}

		protected abstract void map(Snapshot snapshot, DataPoint.Builder point);
	}

	private class Speed extends AbstractSnapshotMapper {
		@Override
		public DataType type() {
			return DataType.TYPE_SPEED;
		}

		@Override
		public void map(Snapshot snapshot, DataPoint.Builder point) {
			point.setField(Field.FIELD_SPEED, (float)snapshot.speed.get() / 100f);
		}
	}

	private class HeartRateBpm extends AbstractSnapshotMapper {
		@Override
		public DataType type() {
			return DataType.TYPE_HEART_RATE_BPM;
		}

		@Override
		public void map(Snapshot snapshot, DataPoint.Builder point) {
			point.setField(Field.FIELD_BPM, (float)snapshot.pulse.get());
		}
	}

	private class Power extends AbstractSnapshotMapper {
		@Override
		public DataType type() {
			return DataType.TYPE_POWER_SAMPLE;
		}

		@Override
		public void map(Snapshot snapshot, DataPoint.Builder point) {
			point.setField(Field.FIELD_WATTS, (float)snapshot.power.get());
		}
	}

	private abstract class AbstractWorkoutMapper extends Mapper {

		@Override
		protected void map(DataSource dataSource, DataSet.Builder dataSet, Workout workout, List<Snapshot> snapshots) {
			DataPoint.Builder point = DataPoint.builder(dataSource);
			point.setTimeInterval(timestamp(workout, 0), timestamp(workout, workout.duration.get()), TimeUnit.MILLISECONDS);
			map(workout, point);
			dataSet.add(point.build());
		}

		public abstract void map(Workout workout, DataPoint.Builder point);
	}

	private class AggregateDistanceDelta extends AbstractWorkoutMapper {

		@Override
		public DataType type() {
			// aggregated over time interval
			return DataType.AGGREGATE_DISTANCE_DELTA;
		}

		@Override
		public void map(Workout workout, DataPoint.Builder point) {
			point.setField(Field.FIELD_DISTANCE, (float)workout.distance.get());
		}
	}

	private class AggregateCaloriesExpended extends AbstractWorkoutMapper {

		@Override
		public DataType type() {
			// aggregated over time interval
			return DataType.AGGREGATE_CALORIES_EXPENDED;
		}

		@Override
		public void map(Workout workout, DataPoint.Builder point) {
			point.setField(Field.FIELD_CALORIES, (float)workout.energy.get());
		}
	}
}