package svenmeier.coxswain.rower.wired;

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
		calculator.strokeEnd(measurement, now);
		calculator.strokeEnd(measurement, now + 200);

		now += 1000;
		calculator.strokeStart(measurement, now);
		calculator.strokeStart(measurement, now + 200);

		now += 1000;
		calculator.strokeEnd(measurement, now);
		calculator.strokeEnd(measurement, now + 200);

		assertEquals(8, measurement.getStrokeRatio());

		now += 2000;
		calculator.strokeStart(measurement, now);
		calculator.strokeStart(measurement, now + 200);

		now += 1000;
		calculator.strokeEnd(measurement, now);
		calculator.strokeEnd(measurement, now + 200);

		assertEquals(16, measurement.getStrokeRatio());

		now += 1500;
		calculator.strokeStart(measurement, now);
		calculator.strokeStart(measurement, now + 200);

		now += 1000;
		calculator.strokeEnd(measurement, now);
		calculator.strokeEnd(measurement, now + 500);

		assertEquals(12, measurement.getStrokeRatio());

		now += 500;
		calculator.strokeStart(measurement, now);
		calculator.strokeStart(measurement, now + 200);

		now += 1000;
		calculator.strokeEnd(measurement, now);
		calculator.strokeEnd(measurement, now + 200);

		assertEquals(4, measurement.getStrokeRatio());
	}
}
