package com.imajsevents;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceUrlsTest
{
	@Test
	public void removesTrailingSlashes()
	{
		assertEquals("https://events.example/api/v1/profile",
			ServiceUrls.endpoint(" https://events.example/// ", "/api/v1/profile"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsPlainHttpAwayFromLocalhost()
	{
		ServiceUrls.normalizeBaseUrl("http://events.example");
	}

	@Test
	public void permitsLocalDevelopment()
	{
		assertEquals("http://127.0.0.1:8787", ServiceUrls.normalizeBaseUrl("http://127.0.0.1:8787/"));
	}

	@Test
	public void customMessageWildcardsAreCaseInsensitive()
	{
		assertTrue(ImajEventsPlugin.wildcardMatch(
			"You have completed * raids.", "You have completed 20 RAIDS."));
		assertFalse(ImajEventsPlugin.wildcardMatch(
			"New personal best: *", "Your Vorkath kill count is: 3."));
	}
}
