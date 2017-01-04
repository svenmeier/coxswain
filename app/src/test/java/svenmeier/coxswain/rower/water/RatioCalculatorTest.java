package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Measurement;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link RatioCalculator}.
 */
public class RatioCalculatorTest {

	Measurement measurement = new Measurement();

	@Test
	public void test() {
		RatioCalculator calculator = new RatioCalculator();

		long now = 100000;
		calculator.recovering(measurement, now);
		calculator.recovering(measurement, now + 200);

		now += 1000;
		calculator.pulling(measurement, now);
		calculator.pulling(measurement, now + 200);

		now += 1000;
		calculator.recovering(measurement, now);
		calculator.recovering(measurement, now + 200);

		assertEquals(8, measurement.strokeRatio);

		now += 2000;
		calculator.pulling(measurement, now);
		calculator.pulling(measurement, now + 200);

		now += 1000;
		calculator.recovering(measurement, now);
		calculator.recovering(measurement, now + 200);

		assertEquals(16, measurement.strokeRatio);

		now += 1500;
		calculator.pulling(measurement, now);
		calculator.pulling(measurement, now + 200);

		now += 1000;
		calculator.recovering(measurement, now);
		calculator.recovering(measurement, now + 500);

		assertEquals(12, measurement.strokeRatio);

		now += 500;
		calculator.pulling(measurement, now);
		calculator.pulling(measurement, now + 200);

		now += 1000;
		calculator.recovering(measurement, now);
		calculator.recovering(measurement, now + 200);

		assertEquals(4, measurement.strokeRatio);
	}
}
