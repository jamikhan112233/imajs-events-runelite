package com.imajsevents;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImajEventsPricingTest
{
	@Test
	public void currentGeValueIsUsedForTradeableItems()
	{
		assertEquals(1_250_000L, ImajEventsPlugin.chooseItemUnitValue(true, 1_250_000L, 65_000L, null));
	}

	@Test
	public void guideValueIsUsedForUntradeableItems()
	{
		assertEquals(99_000L, ImajEventsPlugin.chooseItemUnitValue(false, 0L, 99_000L, null));
	}

	@Test
	public void guideValueIsUsedWhenTradeablePriceIsMissing()
	{
		assertEquals(65_000L, ImajEventsPlugin.chooseItemUnitValue(true, 0L, 65_000L, null));
	}

	@Test
	public void organizerOverrideAlwaysWins()
	{
		assertEquals(225_000L, ImajEventsPlugin.chooseItemUnitValue(false, 0L, 99_000L, 225_000L));
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
