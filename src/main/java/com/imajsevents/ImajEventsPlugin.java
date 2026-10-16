package com.imajsevents;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemStack;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Imaj's Events",
	description = "Connects opted-in RuneLite events to an Imaj's Events competition",
	tags = {"events", "competition", "loot", "levels", "kills", "speedruns"}
)
public class ImajEventsPlugin extends Plugin
{
	private static final Pattern KC_PATTERN = Pattern.compile(
		"Your (?<boss>.+?) (?:kill|chest|completion|success|harvest|opened) count is: ?(?<kc>[\\d,]+)\\b",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern COMPLETED_PATTERN = Pattern.compile(
		"Your (?:completed|subdued) (?<boss>.+?) count is: ?(?<kc>[\\d,]+)\\b",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern SPEEDRUN_PATTERN = Pattern.compile(
		"(?<label>Duration|Challenge time|Completion time|Speedrun duration|Subdued in)\\s*:?\\s*" +
			"(?<time>(?:\\d+:){1,2}\\d{2}(?:\\.\\d+)?)",
		Pattern.CASE_INSENSITIVE);
	private static final int MAX_CUSTOM_RULES = 50;
	private static final float JPEG_QUALITY = 0.85f;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ConfigManager configManager;
	@Inject private Gson gson;
	@Inject private ItemManager itemManager;
	@Inject private DrawManager drawManager;
	@Inject private ScheduledExecutorService executor;
	@Inject private ImajEventsConfig config;
	@Inject private ImajEventsServiceClient serviceClient;

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
	public void onChatMessage(ChatMessage event)
	{
		if (!isConnected() || (event.getType() != ChatMessageType.GAMEMESSAGE
			&& event.getType() != ChatMessageType.SPAM))
		{
			return;
		}

		String message = Text.removeTags(event.getMessage()).trim();
		if (profile.isKillCountEnabled())
		{
			Matcher kc = KC_PATTERN.matcher(message);
			if (!kc.find())
			{
				kc = COMPLETED_PATTERN.matcher(message);
			}
			if (kc.find(0))
			{
				try
				{
					int count = Integer.parseInt(kc.group("kc").replace(",", ""));
					sendKillCount(kc.group("boss").trim(), count);
				}
				catch (NumberFormatException ex)
				{
					log.debug("Ignoring invalid kill-count message: {}", message);
				}
				return;
			}
		}

		if (profile.isSpeedrunEnabled())
		{
			Matcher speedrun = SPEEDRUN_PATTERN.matcher(message);
			if (speedrun.find())
			{
				Map<String, Object> payload = baseEvent("SPEEDRUN");
				payload.put("label", speedrun.group("label"));
				payload.put("duration", speedrun.group("time"));
				payload.put("message", message);
				serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
				return;
			}
		}

		if (profile.isCustomChatEnabled())
		{
			String rule = matchingCustomRule(message);
			if (rule != null)
			{
				Map<String, Object> payload = baseEvent("CUSTOM_CHAT");
				payload.put("matchedRule", rule);
				payload.put("message", message);
				serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
			}
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

	private void sendKillCount(String source, int killCount)
	{
		Map<String, Object> payload = baseEvent("KILL_COUNT");
		payload.put("source", source);
		payload.put("killCount", killCount);
		if (profile.isSetupCaptureEnabled())
		{
			SetupPart equipment = serializeContainer(InventoryID.WORN);
			SetupPart inventory = serializeContainer(InventoryID.INV);
			payload.put("equipment", equipment.items);
			payload.put("inventory", inventory.items);
			payload.put("equipmentValue", equipment.value);
			payload.put("inventoryValue", inventory.value);
			payload.put("setupTotalValue", equipment.value + inventory.value);
		}

		if (profile.isKillCountScreenshotsEnabled() && config.completionScreenshots())
		{
			captureAndSend(payload);
		}
		else
		{
			serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
		}
	}

	private SetupPart serializeContainer(int inventoryId)
	{
		ItemContainer container = client.getItemContainer(inventoryId);
		List<Map<String, Object>> result = new ArrayList<>();
		long total = 0;
		if (container == null)
		{
			return new SetupPart(result, total);
		}
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			int canonicalId = itemManager.canonicalize(item.getId());
			int unitPrice = canonicalId == 995 ? 1 : canonicalId == 13204 ? 1000
				: itemManager.getItemPrice(canonicalId);
			long value = (long) unitPrice * item.getQuantity();
			total += value;
			Map<String, Object> serialized = new LinkedHashMap<>();
			serialized.put("itemId", canonicalId);
			serialized.put("name", itemManager.getItemComposition(canonicalId).getName());
			serialized.put("quantity", item.getQuantity());
			serialized.put("unitPrice", unitPrice);
			serialized.put("totalValue", value);
			result.add(serialized);
		}
		return new SetupPart(result, total);
	}

	private void captureAndSend(Map<String, Object> payload)
	{
		boolean chatHidden = hideWidget(config.hideChatInScreenshots(), InterfaceID.Chatbox.CHATAREA);
		boolean pmsHidden = hideWidget(config.hideChatInScreenshots(), InterfaceID.PmChat.CONTAINER);
		Runnable restore = () -> {
			showWidget(chatHidden, InterfaceID.Chatbox.CHATAREA);
			showWidget(pmsHidden, InterfaceID.PmChat.CONTAINER);
		};
		try
		{
			drawManager.requestNextFrameListener(frame -> {
				restore.run();
				BufferedImage copy = new BufferedImage(frame.getWidth(null), frame.getHeight(null), BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = copy.createGraphics();
				graphics.drawImage(frame, 0, 0, null);
				graphics.dispose();
				BufferedImage reportImage = appendSetupPanel(copy, payload);
				executor.execute(() -> {
					try
					{
						serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload, jpeg(reportImage));
					}
					catch (IOException ex)
					{
						log.debug("Unable to encode completion screenshot", ex);
						serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
					}
				});
			});
		}
		catch (RuntimeException ex)
		{
			restore.run();
			log.debug("Unable to capture completion screenshot", ex);
			serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
		}
	}

	private BufferedImage appendSetupPanel(BufferedImage screenshot, Map<String, Object> payload)
	{
		if (!payload.containsKey("setupTotalValue"))
		{
			return screenshot;
		}
		List<String> lines = new ArrayList<>();
		lines.add("EQUIPMENT");
		appendItemLines(lines, payload.get("equipment"));
		lines.add("");
		lines.add("INVENTORY");
		appendItemLines(lines, payload.get("inventory"));
		lines.add("");
		lines.add("TOTAL: " + formatGp(payload.get("setupTotalValue")) + " gp");

		int panelWidth = 390;
		int lineHeight = 17;
		int outputHeight = Math.max(screenshot.getHeight(), 28 + lines.size() * lineHeight);
		BufferedImage combined = new BufferedImage(screenshot.getWidth() + panelWidth,
			outputHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = combined.createGraphics();
		graphics.setColor(new Color(28, 30, 34));
		graphics.fillRect(0, 0, combined.getWidth(), combined.getHeight());
		graphics.drawImage(screenshot, 0, 0, null);
		graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
			java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
		graphics.setColor(new Color(190, 160, 255));
		graphics.drawString("Imaj's Events — completion setup", screenshot.getWidth() + 14, 22);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		int y = 42;
		for (String line : lines)
		{
			boolean heading = "EQUIPMENT".equals(line) || "INVENTORY".equals(line) || line.startsWith("TOTAL:");
			graphics.setColor(heading ? new Color(235, 225, 255) : Color.WHITE);
			graphics.drawString(line, screenshot.getWidth() + 14, y);
			y += lineHeight;
		}
		graphics.dispose();
		return combined;
	}

	private void appendItemLines(List<String> lines, Object value)
	{
		if (!(value instanceof List))
		{
			lines.add("None recorded");
			return;
		}
		for (Object item : (List<?>) value)
		{
			if (!(item instanceof Map))
			{
				continue;
			}
			Map<?, ?> fields = (Map<?, ?>) item;
			String name = String.valueOf(fields.get("name"));
			String quantity = String.valueOf(fields.get("quantity"));
			String line = name + " x" + quantity + " — " + formatGp(fields.get("totalValue")) + " gp";
			lines.add(line.length() > 48 ? line.substring(0, 47) + "…" : line);
		}
	}

	private String formatGp(Object value)
	{
		if (!(value instanceof Number))
		{
			return "0";
		}
		return NumberFormat.getIntegerInstance(Locale.US).format(((Number) value).longValue());
	}

	private boolean hideWidget(boolean hide, int componentId)
	{
		if (!hide)
		{
			return false;
		}
		Widget widget = client.getWidget(componentId);
		if (widget == null || widget.isHidden())
		{
			return false;
		}
		widget.setHidden(true);
		return true;
	}

	private void showWidget(boolean wasHidden, int componentId)
	{
		if (!wasHidden)
		{
			return;
		}
		clientThread.invokeLater(() -> {
			Widget widget = client.getWidget(componentId);
			if (widget != null)
			{
				widget.setHidden(false);
			}
		});
	}

	private static byte[] jpeg(BufferedImage image) throws IOException
	{
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (ImageOutputStream stream = ImageIO.createImageOutputStream(output))
		{
			writer.setOutput(stream);
			ImageWriteParam parameters = writer.getDefaultWriteParam();
			parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			parameters.setCompressionQuality(JPEG_QUALITY);
			writer.write(null, new IIOImage(image, null, null), parameters);
		}
		finally
		{
			writer.dispose();
		}
		return output.toByteArray();
	}

	private String matchingCustomRule(String message)
	{
		List<String> rules = profile.getCustomChatMessages();
		if (rules == null)
		{
			return null;
		}
		for (int index = 0; index < Math.min(rules.size(), MAX_CUSTOM_RULES); index++)
		{
			String rule = rules.get(index);
			if (rule != null && !rule.trim().isEmpty() && wildcardMatch(rule.trim(), message))
			{
				return rule.trim();
			}
		}
		return null;
	}

	static boolean wildcardMatch(String pattern, String value)
	{
		String expected = pattern.toLowerCase(Locale.ROOT);
		String actual = value.toLowerCase(Locale.ROOT);
		String[] parts = expected.split("\\*", -1);
		int position = 0;
		if (!expected.startsWith("*") && !actual.startsWith(parts[0]))
		{
			return false;
		}
		for (String part : parts)
		{
			if (part.isEmpty())
			{
				continue;
			}
			int found = actual.indexOf(part, position);
			if (found < 0)
			{
				return false;
			}
			position = found + part.length();
		}
		String last = parts[parts.length - 1];
		return expected.endsWith("*") || actual.endsWith(last);
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
		graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
			java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
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

	private static final class SetupPart
	{
		private final List<Map<String, Object>> items;
		private final long value;

		private SetupPart(List<Map<String, Object>> items, long value)
		{
			this.items = items;
			this.value = value;
		}
	}
}
