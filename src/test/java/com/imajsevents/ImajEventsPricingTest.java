package com.imajsevents;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImajEventsPricingTest
{
	@Test
	public void currentRuneLiteValueIsUsedForTradeableItems()
	{
		assertEquals(1_250_000L, ImajEventsPlugin.chooseItemUnitValue(1_250_000L, 65_000L, null));
	}

	@Test
	public void mappedRuneLiteValueIsUsedForUntradeableItems()
	{
		assertEquals(10_427_384L, ImajEventsPlugin.chooseItemUnitValue(10_427_384L, 130_000L, null));
	}

	@Test
	public void guideValueIsUsedWhenRuneLitePriceIsMissing()
	{
		assertEquals(65_000L, ImajEventsPlugin.chooseItemUnitValue(0L, 65_000L, null));
	}

	@Test
	public void organizerOverrideAlwaysWins()
	{
		assertEquals(225_000L, ImajEventsPlugin.chooseItemUnitValue(68_902_986L, 99_000L, 225_000L));
	}

	@Test
	public void itemIdOverrideTakesPriorityOverName()
	{
		Map<String, Long> overrides = new LinkedHashMap<>();
		overrides.put("Infernal max cape", 225_000L);
		overrides.put("21285", 300_000L);
		assertEquals(Long.valueOf(300_000L),
			ImajEventsPlugin.findItemValueOverride(overrides, 21285, "Infernal max cape"));
	}

	@Test
	public void itemNameOverrideIsCaseInsensitive()
	{
		Map<String, Long> overrides = new LinkedHashMap<>();
		overrides.put("infernal MAX cape", 225_000L);
		assertEquals(Long.valueOf(225_000L),
			ImajEventsPlugin.findItemValueOverride(overrides, 21285, "Infernal max cape"));
	}
}
