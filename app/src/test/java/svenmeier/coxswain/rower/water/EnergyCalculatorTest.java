package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Measurement;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link EnergyCalculator}.
 */
public class EnergyCalculatorTest {

	Measurement measurement = new Measurement();

	@Test
	public void test26minutes() {
		assertEquals(191, new EnergyCalculator(65).energy(273*1000));
		assertEquals(223, new EnergyCalculator(75).energy(273*1000));
		assertEquals(256, new EnergyCalculator(85).energy(273*1000));

		assertEquals(273, new EnergyCalculator(90).energy(273*1000));

		assertEquals(289, new EnergyCalculator(95).energy(273*1000));
		assertEquals(322, new EnergyCalculator(105).energy(273*1000));
		assertEquals(354, new EnergyCalculator(115).energy(273*1000));
		assertEquals(387, new EnergyCalculator(125).energy(273*1000));
	}

	@Test
	public void test40minutes() {
		assertEquals(294, new EnergyCalculator(65).energy(420*1000));
		assertEquals(394, new EnergyCalculator(85).energy(420*1000));

		assertEquals(420, new EnergyCalculator(90).energy(420*1000));

		assertEquals(445, new EnergyCalculator(95).energy(420*1000));
		assertEquals(495, new EnergyCalculator(105).energy(420*1000));
		assertEquals(596, new EnergyCalculator(125).energy(420*1000));
	}
}
