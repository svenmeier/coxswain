package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Snapshot;

import static org.junit.Assert.assertEquals;

/**
 */
public class RatioCalculatorTest {

	@Test
	public void test() {
		Snapshot snapshot = new Snapshot();

		RatioCalculator calculator = new RatioCalculator();

		long now = 100000;
		calculator.recovering(snapshot, now);
		calculator.recovering(snapshot, now + 200);

		now += 1000;
		calculator.pulling(snapshot, now);
		calculator.pulling(snapshot, now + 200);

		now += 1000;
		calculator.recovering(snapshot, now);
		calculator.recovering(snapshot, now + 200);

		assertEquals(Integer.valueOf(100), snapshot.strokeRatio.get());

		now += 2000;
		calculator.pulling(snapshot, now);
		calculator.pulling(snapshot, now + 200);

		now += 1000;
		calculator.recovering(snapshot, now);
		calculator.recovering(snapshot, now + 200);

		assertEquals(Integer.valueOf(200), snapshot.strokeRatio.get());

		now += 1500;
		calculator.pulling(snapshot, now);
		calculator.pulling(snapshot, now + 200);

		now += 1000;
		calculator.recovering(snapshot, now);
		calculator.recovering(snapshot, now + 500);

		assertEquals(Integer.valueOf(150), snapshot.strokeRatio.get());

		now += 500;
		calculator.pulling(snapshot, now);
		calculator.pulling(snapshot, now + 200);

		now += 1000;
		calculator.recovering(snapshot, now);
		calculator.recovering(snapshot, now + 200);

		assertEquals(Integer.valueOf(50), snapshot.strokeRatio.get());
	}
}
