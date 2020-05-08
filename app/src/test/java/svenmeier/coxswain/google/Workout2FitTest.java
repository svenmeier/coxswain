package svenmeier.coxswain.google;

import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test for {@link Workout2Fit}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = svenmeier.coxswain.BuildConfig.class)
public class Workout2FitTest {

	private static final long Mon_Jun_15_2015 = 1434326400000l;

	@Test
	public void snapshots() throws IOException {
		Workout workout = new Workout();
		workout.start.set(Mon_Jun_15_2015 + (60 * 1000));
		workout.duration.set(2);
		workout.distance.set(6);
		workout.strokes.set(2);
		workout.energy.set(3);

		List<Snapshot> snapshots = new ArrayList<>();

		for (int i = 0; i < Workout2Fit.MAX_DATAPOINTS + 1; i++) {
			Snapshot snapshot = new Snapshot();
			snapshot.speed.set(4_50);
			snapshot.pulse.set(80);
			snapshot.strokeRate.set(25);
			snapshot.distance.set(2);
			snapshot.strokes.set(0);
			snapshots.add(snapshot);
		}
		Workout2Fit workout2Fit = new Workout2Fit();
		Session session = workout2Fit.session(workout);

		Collection<Workout2Fit.Mapper> mappers = workout2Fit.mappers();

		Iterator<Workout2Fit.Mapper> mapper = mappers.iterator();
		assertEquals(DataType.AGGREGATE_CALORIES_EXPENDED, mapper.next().type());
		assertEquals(DataType.AGGREGATE_DISTANCE_DELTA, mapper.next().type());
		assertEquals(DataType.TYPE_SPEED, mapper.next().type());
		assertEquals(DataType.TYPE_HEART_RATE_BPM, mapper.next().type());
		assertEquals(DataType.TYPE_POWER_SAMPLE, mapper.next().type());
		assertFalse(mapper.hasNext());
	}
}