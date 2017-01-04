package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.rower.Rower;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link RatioCalculator}.
 */
public class RatioCalculatorTest {

	Rower rower = new Rower() {
		@Override
		public boolean open() {
			return false;
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public boolean row() {
			return false;
		}

		@Override
		public void close() {

		}

		@Override
		public String getName() {
			return null;
		}
	};

	@Test
	public void test() {
		RatioCalculator calculator = new RatioCalculator();

		long now = 100000;
		calculator.recovering(rower, now);
		calculator.recovering(rower, now + 200);

		now += 1000;
		calculator.pulling(rower, now);
		calculator.pulling(rower, now + 200);

		now += 1000;
		calculator.recovering(rower, now);
		calculator.recovering(rower, now + 200);

		assertEquals(8, rower.strokeRatio);

		now += 2000;
		calculator.pulling(rower, now);
		calculator.pulling(rower, now + 200);

		now += 1000;
		calculator.recovering(rower, now);
		calculator.recovering(rower, now + 200);

		assertEquals(16, rower.strokeRatio);

		now += 1500;
		calculator.pulling(rower, now);
		calculator.pulling(rower, now + 200);

		now += 1000;
		calculator.recovering(rower, now);
		calculator.recovering(rower, now + 500);

		assertEquals(12, rower.strokeRatio);

		now += 500;
		calculator.pulling(rower, now);
		calculator.pulling(rower, now + 200);

		now += 1000;
		calculator.recovering(rower, now);
		calculator.recovering(rower, now + 200);

		assertEquals(4, rower.strokeRatio);
	}
}
