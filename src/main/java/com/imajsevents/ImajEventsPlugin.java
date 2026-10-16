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
import net.runelite.api.ItemComposition;
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
	private volatile boolean specialKillPreparing;
	private volatile SpecialKillAttempt specialKillAttempt;
	private volatile long specialKillGeneration;

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
		specialKillPreparing = false;
		specialKillAttempt = null;
		specialKillGeneration++;
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

	void startSpecialKill(String boss, Consumer<String> success, Consumer<String> failure)
	{
		String selectedBoss = boss == null ? "" : boss.trim();
		if (!isConnected())
		{
			failure.accept("Enroll first");
			return;
		}
		if (!profile.isSpecialKillEnabled())
		{
			failure.accept("Special Kill is disabled for this competition");
			return;
		}
		if (!config.completionScreenshots())
		{
			failure.accept("Enable Upload completion screenshots first");
			return;
		}
		if (selectedBoss.isEmpty())
		{
			failure.accept("Enter the boss name shown in its KC message");
			return;
		}
		if (specialKillPreparing || specialKillAttempt != null)
		{
			failure.accept("Cancel the current Special Kill first");
			return;
		}

		specialKillPreparing = true;
		long generation = ++specialKillGeneration;
		clientThread.invokeLater(() -> {
			SetupSnapshot startingSetup = currentSetupSnapshot();
			captureScreenshot(startingSetup, "Special Kill — starting setup", screenshot -> {
				if (generation != specialKillGeneration)
				{
					return;
				}
				specialKillPreparing = false;
				specialKillAttempt = new SpecialKillAttempt(selectedBoss, startingSetup.totalValue, screenshot);
				onUi(() -> success.accept("Recording " + selectedBoss + " — waiting for its KC message"));
			}, error -> {
				if (generation != specialKillGeneration)
				{
					return;
				}
				specialKillPreparing = false;
				onUi(() -> failure.accept(error));
			});
		});
	}

	void cancelSpecialKill()
	{
		specialKillPreparing = false;
		specialKillAttempt = null;
		specialKillGeneration++;
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
		if (profile.isKillCountEnabled() || specialKillAttempt != null)
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
					String boss = kc.group("boss").trim();
					if (!finishSpecialKillIfMatched(boss, count) && profile.isKillCountEnabled())
					{
						sendKillCount(boss, count);
					}
				}
				catch (NumberFormatException ex)
				{
					log.debug("Ignoring invalid kill-count message: {}", message);
				}
				return;
			}
		}

		if (profile.isSpeedrunEnabled() || specialKillAttempt != null)
		{
			Matcher speedrun = SPEEDRUN_PATTERN.matcher(message);
			if (speedrun.find())
			{
				if (recordSpecialKillGameTime(speedrun.group("time"), message))
				{
					return;
				}
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
		SetupSnapshot setup = null;
		if (profile.isSetupCaptureEnabled())
		{
			setup = currentSetupSnapshot();
			payload.put("setupTotalValue", setup.totalValue);
			payload.put("priceBasis", "RuneLite mapped market value with Jagex guide fallback");
		}

		if (profile.isKillCountScreenshotsEnabled() && config.completionScreenshots())
		{
			captureAndSend(payload, setup, "Completion setup");
		}
		else
		{
			serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
		}
	}

	private SetupSnapshot currentSetupSnapshot()
	{
		List<SetupItem> equipment = containerSetupItems(InventoryID.WORN);
		List<SetupItem> inventory = containerSetupItems(InventoryID.INV);
		long total = 0;
		for (SetupItem item : equipment)
		{
			total += item.totalValue;
		}
		for (SetupItem item : inventory)
		{
			total += item.totalValue;
		}
		return new SetupSnapshot(equipment, inventory, total);
	}

	private List<SetupItem> containerSetupItems(int inventoryId)
	{
		ItemContainer container = client.getItemContainer(inventoryId);
		List<SetupItem> values = new ArrayList<>();
		if (container == null)
		{
			return values;
		}
		Map<Integer, Long> quantities = new LinkedHashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			int canonicalId = itemManager.canonicalize(item.getId());
			quantities.merge(canonicalId, (long) item.getQuantity(), Long::sum);
		}
		for (Map.Entry<Integer, Long> entry : quantities.entrySet())
		{
			int canonicalId = entry.getKey();
			String name = itemManager.getItemComposition(canonicalId).getName();
			ResolvedItemValue unitValue = resolveItemValue(canonicalId, name);
			values.add(new SetupItem(name, entry.getValue(), unitValue.value, unitValue.source));
		}
		return values;
	}

	private void captureAndSend(Map<String, Object> payload, SetupSnapshot setup, String title)
	{
		captureScreenshot(setup, title,
			screenshot -> serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload,
				screenshot),
			error -> {
				log.warn("{}; sending the event without its screenshot", error);
				serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
			});
	}

	private void captureScreenshot(SetupSnapshot setup, String title, Consumer<byte[]> success,
		Consumer<String> failure)
	{
		boolean chatHidden = hideWidget(config.hideChatInScreenshots(), InterfaceID.Chatbox.CHATAREA);
		boolean pmsHidden = hideWidget(config.hideChatInScreenshots(), InterfaceID.PmChat.CONTAINER);
		Runnable restore = () -> {
			showWidget(chatHidden, InterfaceID.Chatbox.CHATAREA);
			showWidget(pmsHidden, InterfaceID.PmChat.CONTAINER);
		};
		try
		{
			// Queue a second frame so widgets hidden for privacy are absent from the captured image.
			drawManager.requestNextFrameListener(ignored ->
				clientThread.invokeLater(() -> requestScreenshotFrame(setup, title, restore, success, failure)));
		}
		catch (RuntimeException ex)
		{
			restore.run();
			log.warn("Unable to schedule screenshot", ex);
			failure.accept("Unable to schedule screenshot");
		}
	}

	private void requestScreenshotFrame(SetupSnapshot setup, String title, Runnable restore,
		Consumer<byte[]> success, Consumer<String> failure)
	{
		try
		{
			drawManager.requestNextFrameListener(frame -> {
				restore.run();
				BufferedImage copy = new BufferedImage(frame.getWidth(null), frame.getHeight(null),
					BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = copy.createGraphics();
				graphics.drawImage(frame, 0, 0, null);
				graphics.dispose();
				BufferedImage reportImage = setup == null ? copy : appendRiskValuePanel(copy, title, setup);
				executor.execute(() -> {
					try
					{
						success.accept(jpeg(reportImage));
					}
					catch (IOException ex)
					{
						log.warn("Unable to encode screenshot", ex);
						failure.accept("Unable to encode screenshot");
					}
				});
			});
		}
		catch (RuntimeException ex)
		{
			restore.run();
			log.warn("Unable to capture screenshot", ex);
			failure.accept("Unable to capture screenshot");
		}
	}

	private BufferedImage appendRiskValuePanel(BufferedImage screenshot, String title, SetupSnapshot setup)
	{
		int panelWidth = 700;
		int itemRows = Math.max(setup.equipment.size(), setup.inventory.size());
		int outputHeight = Math.max(screenshot.getHeight(), 164 + itemRows * 15);
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
		graphics.drawString("Imaj's Events — " + title, screenshot.getWidth() + 14, 24);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		graphics.setColor(new Color(235, 225, 255));
		graphics.drawString("TOTAL RISK VALUE", screenshot.getWidth() + 14, 52);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
		graphics.setColor(Color.WHITE);
		graphics.drawString(formatGp(setup.totalValue) + " gp", screenshot.getWidth() + 14, 74);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		graphics.setColor(Color.LIGHT_GRAY);
		graphics.drawString("Each line: quantity × unit risk value = line total", screenshot.getWidth() + 14, 94);
		drawSetupColumn(graphics, "EQUIPMENT", setup.equipment, screenshot.getWidth() + 14, 124);
		drawSetupColumn(graphics, "INVENTORY", setup.inventory, screenshot.getWidth() + 356, 124);
		graphics.dispose();
		return combined;
	}

	private void drawSetupColumn(Graphics2D graphics, String heading, List<SetupItem> items, int x, int y)
	{
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
		graphics.setColor(new Color(235, 225, 255));
		graphics.drawString(heading, x, y);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		int lineY = y + 20;
		if (items.isEmpty())
		{
			graphics.setColor(Color.LIGHT_GRAY);
			graphics.drawString("Empty", x, lineY);
			return;
		}
		for (SetupItem item : items)
		{
			graphics.setColor(Color.WHITE);
			graphics.drawString(shorten(item.name, 24), x, lineY);
			graphics.setColor(Color.LIGHT_GRAY);
			graphics.drawString(formatGp(item.quantity) + " × " + formatGp(item.unitPrice)
				+ " = " + formatGp(item.totalValue) + " gp [" + item.priceSource + "]", x + 145, lineY);
			lineY += 15;
		}
	}

	private static String shorten(String value, int maximum)
	{
		return value.length() <= maximum ? value : value.substring(0, maximum - 1) + "…";
	}

	private ResolvedItemValue resolveItemValue(int itemId, String itemName)
	{
		ItemComposition composition = itemManager.getItemComposition(itemId);
		Long override = findItemValueOverride(profile.getItemValueOverrides(), itemId, itemName);
		// ItemManager applies RuneLite's ItemMapping before looking up a price. Asking it for
		// every item is important: untradeables such as Ferocious gloves and Amulet of
		// rancour (s) map to the live value of their tradeable counterpart/components.
		long runeLiteValue = itemManager.getItemPriceWithSource(itemId, true);
		long guideValue = composition.getPrice();
		long value = chooseItemUnitValue(runeLiteValue, guideValue, override);
		String source = override != null ? "override"
			: runeLiteValue > 0L ? composition.isTradeable() ? "GE" : "mapped" : "guide";
		return new ResolvedItemValue(value, source);
	}

	static long chooseItemUnitValue(long runeLiteValue, long guideValue, Long override)
	{
		if (override != null)
		{
			return Math.max(0L, override);
		}
		return Math.max(0L, runeLiteValue > 0L ? runeLiteValue : guideValue);
	}

	static Long findItemValueOverride(Map<String, Long> overrides, int itemId, String itemName)
	{
		if (overrides == null || overrides.isEmpty())
		{
			return null;
		}
		Long byId = overrides.get(String.valueOf(itemId));
		if (byId != null)
		{
			return byId;
		}
		for (Map.Entry<String, Long> entry : overrides.entrySet())
		{
			if (entry.getKey() != null && entry.getValue() != null
				&& entry.getKey().trim().equalsIgnoreCase(itemName))
			{
				return entry.getValue();
			}
		}
		return null;
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

	private boolean finishSpecialKillIfMatched(String detectedBoss, int killCount)
	{
		SpecialKillAttempt attempt = specialKillAttempt;
		if (attempt == null || !bossNamesMatch(attempt.boss, detectedBoss))
		{
			return false;
		}

		if (attempt.bossCompleted)
		{
			return true;
		}
		attempt.bossCompleted = true;
		attempt.detectedBoss = detectedBoss;
		attempt.killCount = killCount;
		SetupSnapshot endingSetup = currentSetupSnapshot();
		attempt.endingGuideValue = endingSetup.totalValue;

		if (panel != null)
		{
			panel.showSpecialKillStatus("Finishing " + detectedBoss + "...");
		}
		captureScreenshot(endingSetup, "Special Kill — ending setup", endingScreenshot -> {
			attempt.endingScreenshot = endingScreenshot;
			trySendSpecialKill(attempt);
			if (panel != null)
			{
				panel.showSpecialKillStatus(attempt.gameTime == null
					? "Boss complete — waiting for the game's time message"
					: "Special Kill sent — game time " + attempt.gameTime);
			}
		}, error -> {
			log.warn("{}; sending a normal completion instead", error);
			if (specialKillAttempt == attempt)
			{
				specialKillAttempt = null;
			}
			sendKillCount(detectedBoss, killCount);
			if (panel != null)
			{
				panel.showSpecialKillStatus("Screenshot failed; normal completion sent");
			}
		});
		return true;
	}

	private boolean recordSpecialKillGameTime(String gameTime, String message)
	{
		SpecialKillAttempt attempt = specialKillAttempt;
		if (attempt == null)
		{
			return false;
		}
		attempt.gameTime = gameTime;
		attempt.gameTimeMessage = message;
		trySendSpecialKill(attempt);
		if (panel != null)
		{
			panel.showSpecialKillStatus(attempt.sent
				? "Special Kill sent — game time " + attempt.gameTime
				: attempt.bossCompleted
					? "Game time received — finishing Special Kill"
					: "Game time received — waiting for " + attempt.boss + " KC");
		}
		return true;
	}

	private void trySendSpecialKill(SpecialKillAttempt attempt)
	{
		synchronized (attempt)
		{
			if (attempt.sent || !attempt.bossCompleted || attempt.gameTime == null
				|| attempt.endingScreenshot == null)
			{
				return;
			}
			attempt.sent = true;
		}

		Map<String, Object> payload = baseEvent("SPECIAL_KILL");
		payload.put("source", attempt.detectedBoss);
		payload.put("killCount", attempt.killCount);
		payload.put("gameTime", attempt.gameTime);
		payload.put("gameTimeMessage", attempt.gameTimeMessage);
		payload.put("startingGuideValue", attempt.startingGuideValue);
		payload.put("endingGuideValue", attempt.endingGuideValue);
		payload.put("guideValueUsed", Math.max(0L, attempt.startingGuideValue - attempt.endingGuideValue));
		payload.put("priceBasis", "RuneLite mapped market value with Jagex guide fallback");
		serviceClient.sendSpecialKillEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload,
			attempt.startingScreenshot, attempt.endingScreenshot);
		if (specialKillAttempt == attempt)
		{
			specialKillAttempt = null;
		}
	}

	static boolean bossNamesMatch(String selectedBoss, String detectedBoss)
	{
		return normalizeBossName(selectedBoss).equals(normalizeBossName(detectedBoss));
	}

	private static String normalizeBossName(String value)
	{
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private void sendLoot(String source, java.util.Collection<ItemStack> items)
	{
		if (!isConnected() || items.isEmpty())
		{
			return;
		}

		Map<Integer, Long> quantities = new LinkedHashMap<>();
		for (ItemStack item : items)
		{
			int canonicalId = itemManager.canonicalize(item.getId());
			quantities.merge(canonicalId, (long) item.getQuantity(), Long::sum);
		}

		List<Map<String, Object>> acceptedItems = new ArrayList<>();
		for (Map.Entry<Integer, Long> entry : quantities.entrySet())
		{
			int itemId = entry.getKey();
			long quantity = entry.getValue();
			String itemName = itemManager.getItemComposition(itemId).getName();
			long unitPrice = resolveItemValue(itemId, itemName).value;
			long totalValue = unitPrice * quantity;
			if (totalValue < profile.getMinimumLootValue()
				|| !lootItemAllowed(profile.getAllowedLootItems(), profile.getIgnoredLootItems(),
					itemName, itemId))
			{
				continue;
			}

			Map<String, Object> acceptedItem = new LinkedHashMap<>();
			acceptedItem.put("itemId", itemId);
			acceptedItem.put("itemName", itemName);
			acceptedItem.put("quantity", quantity);
			acceptedItem.put("unitPrice", unitPrice);
			acceptedItem.put("totalValue", totalValue);
			acceptedItems.add(acceptedItem);
		}

		if (acceptedItems.isEmpty())
		{
			return;
		}
		Map<String, Object> payload = baseEvent("LOOT");
		payload.put("source", source == null ? "Unknown" : source);
		payload.put("items", acceptedItems);
		if (profile.isLootScreenshotsEnabled() && config.lootScreenshots())
		{
			captureScreenshot(null, "", screenshot ->
				serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload, screenshot), error -> {
				log.warn("{}; sending loot without a screenshot", error);
				serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
			});
		}
		else
		{
			serviceClient.sendEvent(config.serviceBaseUrl(), config.enrollmentToken(), payload);
		}
	}

	static boolean lootItemAllowed(List<String> allowed, List<String> ignored, String itemName, int itemId)
	{
		if (matchesLootRule(ignored, itemName, itemId))
		{
			return false;
		}
		return allowed == null || allowed.isEmpty() || matchesLootRule(allowed, itemName, itemId);
	}

	private static boolean matchesLootRule(List<String> rules, String itemName, int itemId)
	{
		if (rules == null)
		{
			return false;
		}
		for (String candidate : rules)
		{
			String rule = candidate == null ? "" : candidate.trim();
			if (!rule.isEmpty() && (rule.equals(String.valueOf(itemId)) || wildcardMatch(rule, itemName)))
			{
				return true;
			}
		}
		return false;
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

	private static final class SpecialKillAttempt
	{
		private final String boss;
		private final long startingGuideValue;
		private final byte[] startingScreenshot;
		private volatile boolean bossCompleted;
		private volatile String detectedBoss;
		private volatile int killCount;
		private volatile long endingGuideValue;
		private volatile byte[] endingScreenshot;
		private volatile String gameTime;
		private volatile String gameTimeMessage;
		private boolean sent;

		private SpecialKillAttempt(String boss, long startingGuideValue, byte[] startingScreenshot)
		{
			this.boss = boss;
			this.startingGuideValue = startingGuideValue;
			this.startingScreenshot = startingScreenshot;
		}
	}

	private static final class SetupSnapshot
	{
		private final List<SetupItem> equipment;
		private final List<SetupItem> inventory;
		private final long totalValue;

		private SetupSnapshot(List<SetupItem> equipment, List<SetupItem> inventory, long totalValue)
		{
			this.equipment = equipment;
			this.inventory = inventory;
			this.totalValue = totalValue;
		}
	}

	private static final class SetupItem
	{
		private final String name;
		private final long quantity;
		private final long unitPrice;
		private final long totalValue;
		private final String priceSource;

		private SetupItem(String name, long quantity, long unitPrice, String priceSource)
		{
			this.name = name;
			this.quantity = quantity;
			this.unitPrice = unitPrice;
			this.totalValue = quantity * unitPrice;
			this.priceSource = priceSource;
		}
	}

	private static final class ResolvedItemValue
	{
		private final long value;
		private final String source;

		private ResolvedItemValue(long value, String source)
		{
			this.value = value;
			this.source = source;
		}
	}
}
