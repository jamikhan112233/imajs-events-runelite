package com.imajsevents;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImajEventsPricingTest
{
	@Test
	public void builtInGuideValueIsUsedDirectly()
	{
		assertEquals(225_000L, ImajEventsPlugin.builtInGuideValue(225_000L));
	}

	@Test
	public void zeroValueRemainsZero()
	{
		assertEquals(0L, ImajEventsPlugin.builtInGuideValue(0L));
	}

	@Test
	public void negativeMissingValuesAreClampedToZero()
	{
		assertEquals(0L, ImajEventsPlugin.builtInGuideValue(-1L));
	}
}
