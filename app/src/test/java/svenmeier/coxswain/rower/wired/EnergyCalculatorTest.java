package svenmeier.coxswain.rower.wired;

import org.junit.Test;

import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.EnergyCalculator;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link EnergyCalculator}.
 */
public class EnergyCalculatorTest {

	private void assertAdjusted(int out, int weight, int in) {
		Measurement measurement = new Measurement();

		measurement.energy = in;
		new EnergyCalculator(weight).adjust(measurement);

		assertEquals(out, measurement.energy);
	}

	@Test
	public void test() {
		assertAdjusted(0, 68, 0);
	}

	@Test
	public void test26minutes() {
		assertAdjusted(261, 65, 273);
		assertAdjusted(299, 75, 273);
		assertAdjusted(337, 85, 273);

		assertAdjusted(355, 90, 273);

		assertAdjusted(374, 95, 273);
		assertAdjusted(412, 105, 273);
		assertAdjusted(450, 115, 273);
		assertAdjusted(488, 125, 273);
	}

	@Test
	public void test40minutes() {
		assertAdjusted(408, 65, 420);
		assertAdjusted(484, 85, 420);

		assertAdjusted(502, 90, 420);

		assertAdjusted(521, 95, 420);
		assertAdjusted(559, 105, 420);
		assertAdjusted(635, 125, 420);
	}
}
