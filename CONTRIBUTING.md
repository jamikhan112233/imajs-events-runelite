# Contributing to Imaj's Events

## Development

Requirements:

- Java 11 or newer (the plugin targets Java 11).
- Access to the deployed official Imaj's Events service and an organizer-provided test competition for integration tests. The plugin intentionally does not accept a custom service host.

Run checks:

```bash
./gradlew clean test
```

Launch the RuneLite development client:

```bash
./gradlew run
```

For Jagex Accounts, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions.

## Manual integration checks

1. Leave **Enable event connection** off and confirm enrollment is refused.
2. Enable it, log into an approved roster character, and enroll using a valid competition and competition password. Confirm the RSN is read from that character.
3. Confirm qualifying loot, enabled kill counts, times, pets, and custom system messages reach the competition webhook. Confirm a different logged-in character cannot submit for that enrollment.
4. Confirm disabled event types, nonmatching messages, and loot below the configured minimum are not posted.
5. Complete an activity and verify the screenshot's item-value lines and total match the setup while the ordinary KC webhook text omits the value.
6. Verify loot, pet, and ordinary completion screenshots each require their corresponding organizer and local permissions. Confirm all upload settings default to off and their warnings remain present.
7. Start a Special Kill, confirm another boss does not finish it, then complete the selected boss and verify both screenshots, official time, risk values, nearby player names, and starting mega-rare flag. Confirm starting requires organizer-enabled Special Kill and local completion screenshots.
8. Generate an RSN-change code, apply it while logged into the renamed character, and confirm reuse and expired codes fail.
9. Restart RuneLite and confirm enrollment is restored without re-entering the competition password.
10. With an unseen release, log in and confirm the update notice appears once. Check world hops, later logins, and disabling/re-enabling the plugin do not repeat it. Check enabling the plugin while already logged in and changing RuneLite profiles. The notice must remain local and work with the event connection disabled.

## Documentation and release guidance

The README is published on the RuneLite website from the commit pinned in Plugin Hub. Keep it focused on installation, use, settings, troubleshooting, and privacy for players. Keep development and submission instructions here rather than in the README.

For every release:

1. Compare all outgoing requests and screenshot behavior with the README Privacy section and `PRIVACY.md`. Document every data field category, destination, opt-in control, and relevant storage or forwarding behavior. Check for contradictions elsewhere in the README.
2. Update `PATCH_NOTES.md` and the version and messages in `PluginUpdateNotice`. The notice should name the plugin and version, summarize the release, clearly disclose changed data or screenshot behavior, and direct players to the README. Keep it local and once per version per RuneLite profile.
3. Preserve opt-in settings, off-by-default screenshot uploads, and the corresponding setting warnings. A login notice supplements the privacy documentation and does not replace consent.
4. Run automated and manual checks, then pin the reviewed source commit in the Plugin Hub manifest. Updating only the default-branch README does not update the website's pinned copy.
5. Preserve the maintainer-approved install warning for routine updates. It is shown at installation; changing non-commit manifest fields blocks automatic review. If behavior falls outside the approved warning, discuss it with a maintainer, especially before changing screenshot defaults.

For this plugin, the approved warning is:

```text
warning=This plugin submits your IP address, and may submit various account information, to a 3rd-party server not controlled or verified by RuneLite developers.
```

Use `plugin-hub-submission/imajs-events` as the manifest template, replace its commit with the actual reviewed source SHA, and update `plugins/imajs-events` in the existing Plugin Hub pull request. Keep the template's warning synchronized with the approved warning. RuneLite reviewers make the final eligibility decision.

Reviewer context: [privacy and login notices](https://github.com/runelite/plugin-hub/pull/20915#issuecomment-7020013500), [screenshot defaults and warnings](https://github.com/runelite/plugin-hub/pull/20915#issuecomment-7020333758), [accurate privacy documentation](https://github.com/runelite/plugin-hub/pull/20915#issuecomment-7020597292), and [player-facing README](https://github.com/runelite/plugin-hub/pull/20915#issuecomment-7020679053).
