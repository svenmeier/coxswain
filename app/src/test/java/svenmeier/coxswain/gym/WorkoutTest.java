package svenmeier.coxswain.gym;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Test for {@link Workout}.
 */
public class WorkoutTest {

	@Test
	public void measurement() {
		Measurement measurement = new Measurement();

		Workout workout = new Workout();

		measurement.reset();;
		workout.onMeasured(measurement);
		assertEquals(Integer.valueOf(0), workout.duration.get());
		assertEquals(Integer.valueOf(0), workout.distance.get());
		assertEquals(Integer.valueOf(0), workout.strokes.get());
		assertEquals(Integer.valueOf(0), workout.energy.get());

		measurement.setDuration(0);
		measurement.setDistance(1);
		measurement.setStrokes(1);
		measurement.setEnergy(1);
		workout.onMeasured(measurement);
		assertEquals(Integer.valueOf(0), workout.duration.get());
		assertEquals(Integer.valueOf(1), workout.distance.get());
		assertEquals(Integer.valueOf(1), workout.strokes.get());
		assertEquals(Integer.valueOf(1), workout.energy.get());

		measurement.setDuration(1);
		measurement.setDistance(2);
		measurement.setStrokes(2);
		measurement.setEnergy(2);
		workout.onMeasured(measurement);
		assertEquals(Integer.valueOf(1), workout.duration.get());
		assertEquals(Integer.valueOf(2), workout.distance.get());
		assertEquals(Integer.valueOf(2), workout.strokes.get());
		assertEquals(Integer.valueOf(2), workout.energy.get());

		measurement.setDuration(1);
		measurement.setDistance(3);
		measurement.setStrokes(3);
		measurement.setEnergy(3);
		workout.onMeasured(measurement);
		assertEquals(Integer.valueOf(1), workout.duration.get());
		assertEquals(Integer.valueOf(3), workout.distance.get());
		assertEquals(Integer.valueOf(3), workout.strokes.get());
		assertEquals(Integer.valueOf(3), workout.energy.get());

		measurement.setDuration(2);
		measurement.setDistance(4);
		measurement.setStrokes(4);
		measurement.setEnergy(4);
		workout.onMeasured(measurement);
		assertEquals(Integer.valueOf(2), workout.duration.get());
		assertEquals(Integer.valueOf(4), workout.distance.get());
		assertEquals(Integer.valueOf(4), workout.strokes.get());
		assertEquals(Integer.valueOf(4), workout.energy.get());
	}
}
