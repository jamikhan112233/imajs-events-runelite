package com.imajsevents;

import org.junit.Assert;
import org.junit.Test;

public class ImajEventsSpecialKillTest
{
	@Test
	public void dksSelectionMatchesEveryKingOnly()
	{
		Assert.assertTrue(ImajEventsPlugin.bossNamesMatch("DKs", "Dagannoth Rex"));
		Assert.assertTrue(ImajEventsPlugin.bossNamesMatch("Dagannoth Kings", "Dagannoth Prime"));
		Assert.assertTrue(ImajEventsPlugin.bossNamesMatch("Dagannoth Kings", "Dagannoth Supreme"));
		Assert.assertFalse(ImajEventsPlugin.bossNamesMatch("DKs", "Sarachnis"));
	}

	@Test
	public void detectsMegaRareNameVariants()
	{
		Assert.assertTrue(ImajEventsPlugin.isMegaRareItemName("Twisted bow"));
		Assert.assertTrue(ImajEventsPlugin.isMegaRareItemName("Scythe of vitur (uncharged)"));
		Assert.assertTrue(ImajEventsPlugin.isMegaRareItemName("Tumeken's shadow"));
		Assert.assertFalse(ImajEventsPlugin.isMegaRareItemName("Bow of faerdhinen"));
	}
}
