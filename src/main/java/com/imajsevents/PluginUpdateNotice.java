package com.imajsevents;

import java.util.List;
import java.util.function.Consumer;

final class PluginUpdateNotice
{
	static final String VERSION = "0.4.0";
	private static final List<String> MESSAGES = List.of(
		"[Imaj's Events] Updated to v" + VERSION + ": Special Kill reports can include nearby player names "
			+ "and whether your starting setup contained a Twisted bow, Scythe of vitur, or Tumeken's shadow.",
		"[Imaj's Events] Special Kill sends start/end screenshots for attempts you start with completion "
			+ "screenshots enabled. Uploads default to off. README and privacy: "
			+ "https://github.com/jamikhan112233/imajs-events-runelite#privacy");

	private PluginUpdateNotice()
	{
	}

	static void showIfNeeded(boolean loggedIn, String lastSeenVersion,
		Consumer<String> displayMessage, Consumer<String> rememberVersion)
	{
		if (!loggedIn || VERSION.equals(lastSeenVersion))
		{
			return;
		}
		MESSAGES.forEach(displayMessage);
		rememberVersion.accept(VERSION);
	}
}
