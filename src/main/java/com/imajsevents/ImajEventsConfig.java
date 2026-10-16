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

	@ConfigItem(
		keyName = "thirdPartyConnection",
		name = "Enable event connection",
		description = "Allows this plugin to send enrolled competition events to the configured service",
		position = 0,
		section = connectionSection,
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean thirdPartyConnection()
	{
		return false;
	}

	@ConfigItem(
		keyName = "serviceBaseUrl",
		name = "Service URL",
		description = "Base URL supplied by the Imaj's Events organizer",
		position = 1,
		section = connectionSection
	)
	default String serviceBaseUrl()
	{
		return "";
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
