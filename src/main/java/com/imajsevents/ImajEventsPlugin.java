package com.imajsevents;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ItemStack;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Imaj's Events",
	description = "Connects opted-in RuneLite events to an Imaj's Events competition",
	tags = {"events", "competition", "loot", "levels"}
)
public class ImajEventsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ImajEventsConfig config;

	@Inject
	private ImajEventsServiceClient serviceClient;

	private final Map<Skill, Integer> observedLevels = new EnumMap<>(Skill.class);
	private volatile CompetitionProfile profile = CompetitionProfile.disabled();
	private ImajEventsPanel panel;
	private NavigationButton navigationButton;
	private int profileRefreshTicks;

	@Override
	protected void startUp()
	{
		panel = new ImajEventsPanel(this);
		navigationButton = NavigationButton.builder()
			.tooltip("Imaj's Events")
			.icon(createIcon())
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		loadStoredProfile();
		refreshProfile();
		log.info("Imaj's Events started");
	}

	@Override
	protected void shutDown()
	{
		observedLevels.clear();
		profileRefreshTicks = 0;
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		panel = null;
		navigationButton = null;
		profile = CompetitionProfile.disabled();
		log.info("Imaj's Events stopped");
	}

	void enroll(String competition, char[] password, String rsn,
		Consumer<CompetitionProfile> success, Consumer<String> failure)
	{
		if (!config.thirdPartyConnection())
		{
			java.util.Arrays.fill(password, '\0');
			failure.accept("Enable the event connection in plugin settings first");
			return;
		}
		if (competition.trim().isEmpty() || password.length == 0 || rsn.trim().isEmpty())
		{
			java.util.Arrays.fill(password, '\0');
			failure.accept("Competition, password, and RSN are required");
			return;
		}

		serviceClient.enroll(config.serviceBaseUrl(), competition, password, rsn, response -> {
			if (response.getEnrollmentToken() == null || response.getProfile() == null)
			{
				onUi(() -> failure.accept("The event service returned an incomplete enrollment"));
				return;
			}
			configManager.setConfiguration(ImajEventsConfig.GROUP, "enrollmentToken", response.getEnrollmentToken());
			applyProfile(response.getProfile());
			onUi(() -> success.accept(response.getProfile()));
		}, error -> onUi(() -> failure.accept(error)));
	}

	void changeRsn(String newRsn, String code, Consumer<CompetitionProfile> success, Consumer<String> failure)
	{
		if (!isConnected())
		{
			failure.accept("Enroll first");
			return;
		}
		if (newRsn.trim().isEmpty() || code.trim().isEmpty())
		{
			failure.accept("New RSN and one-time code are required");
			return;
		}
		serviceClient.changeRsn(config.serviceBaseUrl(), config.enrollmentToken(), newRsn, code,
			updated -> {
				applyProfile(updated);
				onUi(() -> success.accept(updated));
			}, error -> onUi(() -> failure.accept(error)));
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		if (profile.isLootEnabled())
		{
			sendLoot(event.getNpc().getName(), event.getItems());
		}
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (profile.isLootEnabled())
		{
			sendLoot(event.getName(), event.getItems());
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		int current = event.getLevel();
		Integer previous = observedLevels.put(event.getSkill(), current);
		if (!profile.isLevelEnabled() || previous == null || current <= previous)
		{
			return;
		}
		Map<String, Object> payload = baseEvent("LEVEL");
		payload.put("skill", event.getSkill().getName());
		payload.put("level", current);
		serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
	}

	@Subscribe
	public void onGameTick(GameTick ignored)
	{
		if (++profileRefreshTicks >= 500)
		{
			profileRefreshTicks = 0;
			refreshProfile();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ImajEventsConfig.GROUP.equals(event.getGroup())
			&& ("thirdPartyConnection".equals(event.getKey()) || "serviceBaseUrl".equals(event.getKey())))
		{
			profileRefreshTicks = 0;
			refreshProfile();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		Actor actor = event.getActor();
		if (profile.isDeathEnabled() && actor == client.getLocalPlayer())
		{
			serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), baseEvent("DEATH"));
		}
	}

	private void sendLoot(String source, java.util.Collection<ItemStack> items)
	{
		if (!isConnected() || items.isEmpty())
		{
			return;
		}
		long totalValue = 0;
		List<Map<String, Object>> serializedItems = new ArrayList<>();
		for (ItemStack item : items)
		{
			int price = itemManager.getItemPrice(item.getId());
			long value = (long) price * item.getQuantity();
			totalValue += value;
			Map<String, Object> serialized = new LinkedHashMap<>();
			serialized.put("itemId", item.getId());
			serialized.put("quantity", item.getQuantity());
			serialized.put("unitPrice", price);
			serializedItems.add(serialized);
		}
		if (totalValue < profile.getMinimumLootValue())
		{
			return;
		}
		Map<String, Object> payload = baseEvent("LOOT");
		payload.put("source", source == null ? "Unknown" : source);
		payload.put("totalValue", totalValue);
		payload.put("items", serializedItems);
		serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
	}

	private Map<String, Object> baseEvent(String type)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", type);
		payload.put("clientTimestamp", System.currentTimeMillis());
		return payload;
	}

	private void refreshProfile()
	{
		if (!isConnected())
		{
			return;
		}
		serviceClient.refreshProfile(config.serviceBaseUrl(), config.enrollmentToken(), this::applyProfile,
			error -> {
				log.debug("Unable to refresh Imaj's Events profile: {}", error);
				if (panel != null)
				{
					panel.showStatus(error);
				}
			});
	}

	private boolean isConnected()
	{
		return config.thirdPartyConnection()
			&& !config.serviceBaseUrl().trim().isEmpty()
			&& !config.enrollmentToken().trim().isEmpty();
	}

	private void loadStoredProfile()
	{
		if (!config.profileJson().trim().isEmpty())
		{
			try
			{
				profile = gson.fromJson(config.profileJson(), CompetitionProfile.class);
			}
			catch (RuntimeException ex)
			{
				log.debug("Ignoring invalid stored Imaj's Events profile", ex);
				profile = CompetitionProfile.disabled();
			}
		}
		if (panel != null)
		{
			panel.showProfile(profile);
		}
	}

	private void applyProfile(CompetitionProfile updated)
	{
		profile = updated;
		configManager.setConfiguration(ImajEventsConfig.GROUP, "competitionName", updated.getCompetitionName());
		configManager.setConfiguration(ImajEventsConfig.GROUP, "registeredRsn", updated.getRegisteredRsn());
		configManager.setConfiguration(ImajEventsConfig.GROUP, "profileJson", gson.toJson(updated));
		if (panel != null)
		{
			panel.showProfile(updated);
		}
	}

	private void onUi(Runnable runnable)
	{
		SwingUtilities.invokeLater(runnable);
	}

	private BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(126, 87, 194));
		graphics.fillRoundRect(1, 1, 30, 30, 9, 9);
		graphics.setColor(Color.WHITE);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
		graphics.drawString("I", 13, 24);
		graphics.dispose();
		return image;
	}

	@Provides
	ImajEventsConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(ImajEventsConfig.class);
	}
}
