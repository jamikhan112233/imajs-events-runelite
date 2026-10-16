# Imaj's Events

Imaj's Events is an opt-in RuneLite competition client. It runs similarly to [oCage](https://runelite.net/plugin-hub/show/ocage): RuneLite detects enabled in-game event signals and reports them to an event service, which records and forwards them for competition use.

A participant enrolls with a competition name, competition password, and the RuneScape name registered for that event. The service returns a server-owned profile that controls exactly which supported notifications may be sent.

## Supported profile settings

The competition service can independently enable:

- Loot notifications and a minimum total Grand Exchange value
- Level-up notifications
- Player-death notifications
- Kill-count and completion notifications
- Equipment and inventory snapshots when a kill count/completion is detected
- Opt-in completion screenshots
- Speedrun time notifications from game-generated completion messages
- Custom system/game-message rules using case-insensitive `*` wildcards

Profiles refresh when the plugin starts and approximately every five minutes.

## Completion setup values and screenshots

When setup capture is enabled, every detected kill-count/completion report includes:

- Equipped items
- Inventory items
- Quantity, current RuneLite Grand Exchange unit price, and total value for each item
- Separate equipment and inventory totals
- Combined setup value

Untradeable items can show a value of zero when RuneLite has no Grand Exchange mapping. Coins and platinum tokens use their face values.

Screenshots require both sides to opt in: the organizer must permit them in the competition profile and the player must enable **Upload completion screenshots** in RuneLite. Screenshots are JPEG files attached to the same completion event. When setup capture is enabled, the image receives a side panel listing the equipment, inventory, and total setup value. Chat and private-message widgets are hidden during capture by default.

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

The network connection is opt-in and disabled by default. When enabled, requests necessarily disclose the user's IP address to the configured Imaj's Events service. Enrollment sends the participant-entered competition name, password, and RSN. Event requests send the enabled event details and can include the current equipment/inventory names, quantities, values, and an optional game screenshot.

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
5. Complete an activity and verify the equipment/inventory lines and totals match the current setup.
6. Verify no screenshot is sent until both the server profile and local setting permit it; confirm chat is hidden in an allowed screenshot.
7. Generate an RSN code for the participant, use it once, and confirm reuse and expired codes fail.
8. Restart RuneLite and confirm the opaque enrollment is restored without re-entering the competition password.

## Plugin Hub submission

The repository is intentionally Java-only and uses the standard Plugin Hub build. After an in-game test is confirmed:

1. Pin the source commit to submit.
2. Update the manifest in `plugin-hub-submission/imajs-events`.
3. Add that manifest to `runelite/plugin-hub`.
4. Include the network, setup-data, and optional-screenshot privacy warning in the pull-request description.

RuneLite reviewers make the final eligibility decision for plugins that send opted-in game events to a third-party service.

## License

BSD 2-Clause License.
