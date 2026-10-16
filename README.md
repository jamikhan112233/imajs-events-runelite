# Imaj's Events

Imaj's Events is a RuneLite competition client. A participant enrolls with a competition name, a competition password, and the RuneScape name registered for that event. The competition service returns a profile that controls which supported notifications are enabled.

This project is independent from Dink. It follows similar event-notification patterns, but it does not depend on Dink, copy Dink's configuration, or read/write another plugin's settings.

## Supported profile settings

The service currently controls:

- Loot notifications and their minimum total Grand Exchange value
- Level-up notifications
- Death notifications

Profiles refresh when the plugin starts and approximately every five minutes.

## Enrollment and RSN changes

- The competition password is entered in the Imaj's Events side panel and is kept only in memory for the enrollment request.
- The plugin stores the opaque enrollment token returned by the service; it does not store the competition password.
- An organizer must generate a participant-specific, one-time RSN-change code.
- The code expires after 15 minutes and is consumed by a successful change.

## Privacy

The network connection is opt-in and disabled by default. When enabled, requests necessarily disclose the user's IP address to the configured Imaj's Events service. Enrollment sends the participant-entered competition name, password, and RSN. Event requests send the registered RSN indirectly through the enrollment token plus the enabled event details.

The plugin never reads the currently logged-in RuneScape name and never changes Dink or any other plugin's configuration.

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
3. Confirm a qualifying loot event reaches only the competition webhook.
4. Confirm disabled event types and loot below the configured minimum are not posted.
5. Generate an RSN code for the participant, use it once, and confirm reuse and expired codes fail.
6. Restart RuneLite and confirm the opaque enrollment is restored without re-entering the competition password.

## Plugin Hub submission

The repository is intentionally Java-only and uses the standard Plugin Hub build. After an in-game test is confirmed:

1. Publish this repository publicly.
2. Pin the commit to submit.
3. Add a manifest to `runelite/plugin-hub` pointing to the repository and commit.
4. Include a clear network/privacy warning in the manifest and pull-request description.

RuneLite reviewers make the final eligibility decision, especially for plugins that send opted-in game events to a third-party service.

## License

BSD 2-Clause License.
