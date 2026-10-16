package com.imajsevents;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
