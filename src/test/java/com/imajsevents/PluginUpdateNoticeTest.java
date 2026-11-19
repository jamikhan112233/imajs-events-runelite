package com.imajsevents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;

public class PluginUpdateNoticeTest
{
	@Test
	public void waitsForLoginAndDoesNotRepeatAcrossTicksHopsOrSessions()
	{
		AtomicReference<String> savedVersion = new AtomicReference<>("");
		List<String> messages = new ArrayList<>();
		PluginUpdateNotice.showIfNeeded(false, savedVersion.get(), messages::add, savedVersion::set);
		Assert.assertTrue(messages.isEmpty());
		Assert.assertEquals("", savedVersion.get());

		PluginUpdateNotice.showIfNeeded(true, savedVersion.get(), messages::add, savedVersion::set);
		Assert.assertFalse(messages.isEmpty());
		Assert.assertEquals(PluginUpdateNotice.VERSION, savedVersion.get());
		int initialCount = messages.size();
		for (int tick = 0; tick < 5; tick++)
		{
			PluginUpdateNotice.showIfNeeded(true, savedVersion.get(), messages::add, savedVersion::set);
		}
		PluginUpdateNotice.showIfNeeded(false, savedVersion.get(), messages::add, savedVersion::set);
		// A new session restores the version from the profile rather than relying on memory.
		AtomicReference<String> restoredVersion = new AtomicReference<>(savedVersion.get());
		PluginUpdateNotice.showIfNeeded(true, restoredVersion.get(), messages::add, restoredVersion::set);
		Assert.assertEquals(initialCount, messages.size());
	}

	@Test
	public void showsAnUnseenReleaseIndependentlyForEachProfile()
	{
		AtomicReference<String> firstProfile = new AtomicReference<>("0.3.0");
		AtomicReference<String> secondProfile = new AtomicReference<>("");
		List<String> messages = new ArrayList<>();
		PluginUpdateNotice.showIfNeeded(true, firstProfile.get(), messages::add, firstProfile::set);
		int perReleaseCount = messages.size();
		Assert.assertTrue(perReleaseCount > 0);
		PluginUpdateNotice.showIfNeeded(true, secondProfile.get(), messages::add, secondProfile::set);
		Assert.assertEquals(perReleaseCount * 2, messages.size());
		PluginUpdateNotice.showIfNeeded(true, firstProfile.get(), messages::add, firstProfile::set);
		Assert.assertEquals(perReleaseCount * 2, messages.size());
	}

	@Test
	public void remembersTheVersionOnlyAfterAllMessagesAreDisplayed()
	{
		AtomicReference<String> savedVersion = new AtomicReference<>("");
		try
		{
			PluginUpdateNotice.showIfNeeded(true, savedVersion.get(), message -> {
				throw new IllegalStateException("Chat unavailable");
			}, savedVersion::set);
			Assert.fail("Expected the chat failure");
		}
		catch (IllegalStateException expected)
		{
			Assert.assertEquals("", savedVersion.get());
		}
		List<String> messages = new ArrayList<>();
		PluginUpdateNotice.showIfNeeded(true, savedVersion.get(), messages::add, savedVersion::set);
		Assert.assertFalse(messages.isEmpty());
		Assert.assertEquals(PluginUpdateNotice.VERSION, savedVersion.get());
	}
}
