# Imaj's Events

Imaj's Events is an opt-in RuneLite competition client. RuneLite detects enabled in-game event signals and reports them to an event service, which records and forwards them for competition use.

A participant enrolls with a competition name, competition password, and the RuneScape name registered for that event. The service returns a server-owned profile that controls exactly which supported notifications may be sent.

## Supported profile settings

The competition service can independently enable:

- Grouped Loot Tracker notifications, a minimum per-item stack value, organizer-owned allow/ignore item rules, and separately opt-in screenshots
- Level-up notifications
- Player-death notifications
- Kill-count and completion notifications
- A combined equipment-plus-inventory risk-value total when a kill count/completion is detected
- Opt-in completion screenshots
- Opt-in Special Kills with starting/ending screenshots, risk values, and the game's official completion time
- Speedrun time notifications from game-generated completion messages
- Custom system/game-message rules using case-insensitive `*` wildcards

Profiles refresh when the plugin starts and approximately every five minutes.

## Loot items and organizer filters

Loot is sourced from RuneLite's built-in Loot Tracker event. Keep Loot Tracker enabled while using Imaj's Events. All qualifying item stacks from one Loot Tracker source event are submitted together in one Discord message; multiple quantities of the same item remain one stack.

The competition profile controls two server-owned lists:

- `allowedLootItems`: empty allows all items; otherwise only matching items are sent.
- `ignoredLootItems`: matching items are never sent and override the allowlist.

Rules accept exact item names, numeric item IDs, or case-insensitive `*` wildcards such as `Godsword shard *`. The minimum loot value is applied to each individual item stack using the competition's item-value rules, not the combined drop.

## Setup risk values and screenshots

Item values use the following priority throughout the plugin: an organizer-defined `itemValueOverrides` entry first; RuneLite's current market value second; and the built-in Jagex item-definition guide value only when RuneLite has no market value. RuneLite's `ItemMapping` is applied even when the equipped item is untradeable, matching tradeable counterparts/components used for risk valuation. For example, Ferocious gloves use Hydra leather's current value and Amulet of rancour (s) uses the normal Amulet of rancour's current value. Override keys may be an exact case-insensitive item name or canonical numeric item ID, with the ID taking priority. An override remains available for items that have no suitable RuneLite mapping.

When setup capture is enabled, the KC webhook text does not display its equipment-plus-inventory value. Its screenshot audit panel lists every line as quantity × unit value = line total and labels the source as `[override]`, `[GE]`, `[mapped]`, or `[guide]`, so the calculation can be checked without transmitting the item breakdown as event data. `[mapped]` means an untradeable item was valued through RuneLite's tradeable counterpart/component mapping.

Screenshots require both sides to opt in. Completion screenshots require organizer permission plus the player's **Upload completion screenshots** switch. Loot screenshots use a separate organizer permission and local **Upload loot screenshots** switch; one captured frame is attached to the grouped source message. Setup screenshots include the total and its equipment/inventory line-item audit. Chat and private-message widgets are hidden during capture by default.

## Special Kill

Special Kill is inactive unless the player explicitly uses the side panel:

1. Enter the boss name as it appears in the boss's game-generated KC message.
2. Press **Start Special Kill** to capture the starting frame and combined risk value.
3. Complete that selected boss. Other boss completions do not end the attempt.
4. The matching KC message captures the ending frame and value. The report waits for the game's `Duration`, `Challenge time`, `Completion time`, `Speedrun duration`, or `Subdued in` chat line, then sends that official time with both screenshots and risk totals.

The Special Kill report replaces the ordinary KC report so Discord does not receive a duplicate. The player can cancel an active attempt at any time, and restarting RuneLite clears it.

## Kill counts, speedruns, and custom messages

Kill-count detection accepts game-generated messages such as:

```text
Your Vorkath kill count is: 123.
Your completed Chambers of Xeric count is: 12.
```

Speedrun detection accepts game-generated time labels including `Duration`, `Challenge time`, `Completion time`, `Speedrun duration`, and `Subdued in`.

Custom messages are controlled by the server profile. Rules are safe wildcard strings, not regular expressions. For example:

```text
You have completed * Tombs of Amascut raids.
New personal best: *
```

The plugin evaluates only RuneLite `GAMEMESSAGE` and `SPAM` system-message types. It does not read player chat, clan chat, friends chat, or private messages for custom rules.

## Enrollment and RSN changes

- The competition password is entered in the Imaj's Events side panel and is kept only in memory for the enrollment request.
- The plugin stores the opaque enrollment token returned by the service; it does not store the competition password.
- An organizer must generate a participant-specific, one-time RSN-change code.
- The code expires after 15 minutes and is consumed by a successful change.

## Privacy

The network connection is opt-in and disabled by default. When enabled, requests necessarily disclose the user's IP address to the configured Imaj's Events service. Enrollment sends the participant-entered competition name, password, and RSN. Event requests send the enabled event details and can include combined setup risk-value totals and optional game screenshots. Special Kill sends its selected boss, the official game-generated completion time, starting/ending risk totals, and two screenshots.

The plugin does not read the currently logged-in RuneScape name and does not inspect or modify another plugin's configuration.

## Development

Requirements:

- Java 11+
- A deployed `imajs-events-service` Worker

Run checks:

```bash
./gradlew clean test
```

Launch the RuneLite development client:

```bash
./gradlew run
```

For Jagex Accounts, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions.

Test the following manually in game:

1. Leave **Enable event connection** off and confirm enrollment is refused.
2. Enable it, enter the Worker HTTPS URL, and enroll with a valid competition, password, and RSN.
3. Confirm qualifying loot, enabled kill counts, times, and custom system messages reach only the competition webhook.
4. Confirm disabled event types, nonmatching messages, and loot below the configured minimum are not posted.
5. Complete an activity and verify the screenshot's item-value lines and total match the setup while the KC webhook text omits the value.
6. Verify loot and completion screenshots each require their corresponding organizer and local permissions.
7. Start a Special Kill, confirm another boss does not finish it, then complete the selected boss and verify two screenshots and the game's official time.
8. Generate an RSN code for the participant, use it once, and confirm reuse and expired codes fail.
9. Restart RuneLite and confirm the opaque enrollment is restored without re-entering the competition password.

## Plugin Hub submission

The repository is intentionally Java-only and uses the standard Plugin Hub build. After an in-game test is confirmed:

1. Pin the source commit to submit.
2. Update the manifest in `plugin-hub-submission/imajs-events`.
3. Add that manifest to `runelite/plugin-hub`.
4. Include the network, setup-data, and optional-screenshot privacy warning in the pull-request description.

RuneLite reviewers make the final eligibility decision for plugins that send opted-in game events to a third-party service.

## License

BSD 2-Clause License.
