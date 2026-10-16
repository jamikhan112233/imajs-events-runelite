package com.imajsevents;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImajEventsPricingTest
{
	@Test
	public void marketValueWinsWhenAvailable()
	{
		assertEquals(1_250_000L, ImajEventsPlugin.chooseSetupUnitValue(1_250_000L, 225_000L));
	}

	@Test
	public void inGameValueIsUsedForUntradeables()
	{
		assertEquals(225_000L, ImajEventsPlugin.chooseSetupUnitValue(0L, 225_000L));
	}

	@Test
	public void negativeMissingValuesAreClampedToZero()
	{
		assertEquals(0L, ImajEventsPlugin.chooseSetupUnitValue(0L, -1L));
	}
}
