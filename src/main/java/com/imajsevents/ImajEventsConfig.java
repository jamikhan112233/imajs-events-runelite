package com.imajsevents;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ImajEventsConfig.GROUP)
public interface ImajEventsConfig extends Config
{
	String GROUP = "imajs-events";

	@ConfigSection(
		name = "Connection",
		description = "Connection to the Imaj's Events service",
		position = 0
	)
	String connectionSection = "connection";

	@ConfigSection(
		name = "Completion screenshots",
		description = "Optional screenshots for kill-count and completion events",
		position = 1
	)
	String screenshotSection = "screenshots";

	@ConfigItem(
		keyName = "thirdPartyConnection",
		name = "Enable event connection",
		description = "Allows this plugin to send enrolled competition events to the official Imaj's Events service",
		position = 0,
		section = connectionSection,
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean thirdPartyConnection()
	{
		return false;
	}

	@ConfigItem(
		keyName = "completionScreenshots",
		name = "Upload completion screenshots",
		description = "When the competition profile also permits it, attach a screenshot to kill-count and completion reports",
		position = 0,
		section = screenshotSection,
		warning = "Screenshots may contain visible game information and are sent to the official Imaj's Events 3rd-party service"
	)
	default boolean completionScreenshots()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideChatInScreenshots",
		name = "Hide chat in screenshots",
		description = "Temporarily hides public chat and private messages from captured completion screenshots",
		position = 1,
		section = screenshotSection
	)
	default boolean hideChatInScreenshots()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lootScreenshots",
		name = "Upload loot screenshots",
		description = "When the competition profile permits it, attach a screenshot to each qualifying Loot Tracker item",
		position = 2,
		section = screenshotSection,
		warning = "Screenshots may contain visible game information and are sent to the official Imaj's Events 3rd-party service"
	)
	default boolean lootScreenshots()
	{
		return false;
	}

	@ConfigItem(
		keyName = "petScreenshots",
		name = "Upload pet screenshots",
		description = "When the competition profile permits it, attach a screenshot to pet reports",
		position = 3,
		section = screenshotSection,
		warning = "Screenshots may contain visible game information and are sent to the official Imaj's Events 3rd-party service"
	)
	default boolean petScreenshots()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enrollmentToken",
		name = "Enrollment token",
		description = "Opaque token issued by the service after enrollment",
		hidden = true
	)
	default String enrollmentToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "competitionName",
		name = "Competition name",
		description = "Competition associated with the current enrollment",
		hidden = true
	)
	default String competitionName()
	{
		return "";
	}

	@ConfigItem(
		keyName = "registeredRsn",
		name = "Registered RSN",
		description = "RSN associated with the current enrollment",
		hidden = true
	)
	default String registeredRsn()
	{
		return "";
	}

	@ConfigItem(
		keyName = "profileJson",
		name = "Competition profile",
		description = "Last competition profile returned by the service",
		hidden = true
	)
	default String profileJson()
	{
		return "";
	}
}
