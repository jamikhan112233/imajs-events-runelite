package com.imajsevents;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CompetitionProfile
{
	private String competitionName = "";
	private String registeredRsn = "";
	private boolean lootEnabled;
	private int minimumLootValue;
	private List<String> allowedLootItems = new ArrayList<>();
	private List<String> ignoredLootItems = new ArrayList<>();
	private boolean lootScreenshotsEnabled;
	private boolean levelEnabled;
	private boolean deathEnabled;
	private boolean killCountEnabled;
	private boolean setupCaptureEnabled;
	private boolean killCountScreenshotsEnabled;
	private boolean specialKillEnabled;
	private boolean speedrunEnabled;
	private boolean customChatEnabled;
	private List<String> customChatMessages = new ArrayList<>();

	public static CompetitionProfile disabled()
	{
		return new CompetitionProfile();
	}
}
