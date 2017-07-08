package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Measurement;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link EnergyCalculator}.
 */
public class EnergyCalculatorTest {

	@Test
	public void test() {
		assertEquals(0, new EnergyCalculator(68).energy(0));
	}

	@Test
	public void test26minutes() {
		assertEquals(261, new EnergyCalculator(65).energy(273*1000));
		assertEquals(299, new EnergyCalculator(75).energy(273*1000));
		assertEquals(337, new EnergyCalculator(85).energy(273*1000));

		assertEquals(355, new EnergyCalculator(90).energy(273*1000));

		assertEquals(374, new EnergyCalculator(95).energy(273*1000));
		assertEquals(412, new EnergyCalculator(105).energy(273*1000));
		assertEquals(450, new EnergyCalculator(115).energy(273*1000));
		assertEquals(488, new EnergyCalculator(125).energy(273*1000));
	}

	@Test
	public void test40minutes() {
		assertEquals(408, new EnergyCalculator(65).energy(420*1000));
		assertEquals(484, new EnergyCalculator(85).energy(420*1000));

		assertEquals(502, new EnergyCalculator(90).energy(420*1000));

		assertEquals(521, new EnergyCalculator(95).energy(420*1000));
		assertEquals(559, new EnergyCalculator(105).energy(420*1000));
		assertEquals(635, new EnergyCalculator(125).energy(420*1000));
	}
}
