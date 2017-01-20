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

		measurement.duration = 0;
		measurement.distance = 0;
		measurement.strokes = 0;
		measurement.energy = 0;
		assertEquals(false, workout.onMeasured(measurement));
		assertEquals(Integer.valueOf(0), workout.duration.get());
		assertEquals(Integer.valueOf(0), workout.distance.get());
		assertEquals(Integer.valueOf(0), workout.strokes.get());
		assertEquals(Integer.valueOf(0), workout.energy.get());

		measurement.duration = 0;
		measurement.distance = 1;
		measurement.strokes = 1;
		measurement.energy = 1;
		assertEquals(false, workout.onMeasured(measurement));
		assertEquals(Integer.valueOf(0), workout.duration.get());
		assertEquals(Integer.valueOf(1), workout.distance.get());
		assertEquals(Integer.valueOf(1), workout.strokes.get());
		assertEquals(Integer.valueOf(1), workout.energy.get());

		measurement.duration = 1;
		measurement.distance = 2;
		measurement.strokes = 2;
		measurement.energy = 2;
		assertEquals(true, workout.onMeasured(measurement));
		assertEquals(Integer.valueOf(1), workout.duration.get());
		assertEquals(Integer.valueOf(2), workout.distance.get());
		assertEquals(Integer.valueOf(2), workout.strokes.get());
		assertEquals(Integer.valueOf(2), workout.energy.get());

		measurement.duration = 1;
		measurement.distance = 3;
		measurement.strokes = 3;
		measurement.energy = 3;
		assertEquals(false, workout.onMeasured(measurement));
		assertEquals(Integer.valueOf(1), workout.duration.get());
		assertEquals(Integer.valueOf(3), workout.distance.get());
		assertEquals(Integer.valueOf(3), workout.strokes.get());
		assertEquals(Integer.valueOf(3), workout.energy.get());

		measurement.duration = 2;
		measurement.distance = 4;
		measurement.strokes = 4;
		measurement.energy = 4;
		assertEquals(true, workout.onMeasured(measurement));
		assertEquals(Integer.valueOf(2), workout.duration.get());
		assertEquals(Integer.valueOf(4), workout.distance.get());
		assertEquals(Integer.valueOf(4), workout.strokes.get());
		assertEquals(Integer.valueOf(4), workout.energy.get());
	}
}
