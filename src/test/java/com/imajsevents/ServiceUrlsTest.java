package com.imajsevents;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceUrlsTest
{
	@Test
	public void usesOfficialServiceUrl()
	{
		assertEquals("https://imajs-events-service.imajs-events.workers.dev/api/v1/profile",
			ServiceUrls.endpoint("/api/v1/profile"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidServicePath()
	{
		ServiceUrls.endpoint("api/v1/profile");
	}

	@Test
	public void customMessageWildcardsAreCaseInsensitive()
	{
		assertTrue(ImajEventsPlugin.wildcardMatch(
			"You have completed * raids.", "You have completed 20 RAIDS."));
		assertFalse(ImajEventsPlugin.wildcardMatch(
			"New personal best: *", "Your Vorkath kill count is: 3."));
	}

	@Test
	public void lootItemRulesSupportAllowIgnoreIdsAndWildcards()
	{
		assertTrue(ImajEventsPlugin.lootItemAllowed(
			Collections.emptyList(), Collections.emptyList(), "Twisted bow", 20997));
		assertTrue(ImajEventsPlugin.lootItemAllowed(
			Arrays.asList("Twisted bow"), Collections.emptyList(), "TWISTED BOW", 20997));
		assertTrue(ImajEventsPlugin.lootItemAllowed(
			Arrays.asList("Godsword shard *"), Collections.emptyList(), "Godsword shard 2", 11820));
		assertTrue(ImajEventsPlugin.lootItemAllowed(
			Arrays.asList("20997"), Collections.emptyList(), "Anything", 20997));
		assertFalse(ImajEventsPlugin.lootItemAllowed(
			Arrays.asList("Twisted bow"), Collections.emptyList(), "Coins", 995));
		assertFalse(ImajEventsPlugin.lootItemAllowed(
			Collections.emptyList(), Arrays.asList("Coins"), "Coins", 995));
	}

	@Test
	public void lootValueAndAllowlistCanUseAndOrModes()
	{
		assertFalse(ImajEventsPlugin.lootItemAccepted(
			Arrays.asList("Twisted bow"), Collections.emptyList(), "Twisted bow", 20997,
			500L, 1_000L, true));
		assertTrue(ImajEventsPlugin.lootItemAccepted(
			Arrays.asList("Twisted bow"), Collections.emptyList(), "Twisted bow", 20997,
			500L, 1_000L, false));
		assertFalse(ImajEventsPlugin.lootItemAccepted(
			Arrays.asList("Twisted bow"), Collections.emptyList(), "Coins", 995,
			2_000L, 1_000L, true));
		assertTrue(ImajEventsPlugin.lootItemAccepted(
			Arrays.asList("Twisted bow"), Collections.emptyList(), "Coins", 995,
			2_000L, 1_000L, false));
		assertFalse(ImajEventsPlugin.lootItemAccepted(
			Collections.emptyList(), Arrays.asList("Coins"), "Coins", 995,
			2_000L, 1_000L, false));
	}

	@Test
	public void specialKillBossMatchingIgnoresCaseSpacesAndPunctuation()
	{
		assertTrue(ImajEventsPlugin.bossNamesMatch("Theatre of Blood", "THEATRE-OF-BLOOD"));
		assertTrue(ImajEventsPlugin.bossNamesMatch("Phantom Muspah", "Phantom Muspah"));
		assertFalse(ImajEventsPlugin.bossNamesMatch("Vorkath", "Zulrah"));
	}
}
