package com.imajsevents;

import lombok.Data;

@Data
public class CompetitionProfile
{
	private String competitionName = "";
	private String registeredRsn = "";
	private boolean lootEnabled;
	private int minimumLootValue;
	private boolean levelEnabled;
	private boolean deathEnabled;

	public static CompetitionProfile disabled()
	{
		return new CompetitionProfile();
	}
}
