# Imaj's Events

Imaj's Events connects RuneLite to competitions hosted by Imaj's Events. It can report competition loot, pets, levels, deaths, kill counts, completion times, and selected game messages to your organizer's Discord channel.

The event connection and screenshot uploads are **off by default**. Your competition organizer chooses which events are supported, and you choose whether to enable the connection and each screenshot option.

## Getting started

1. Install **Imaj's Events** from RuneLite's Plugin Hub and enable the plugin.
2. Log into the RuneScape character you want to enter in the competition.
3. Read the [Privacy](#privacy) section, then turn on **Enable event connection** in the plugin settings.
4. Open the Imaj's Events side panel. Enter the **Competition** name and **Competition password** supplied by your organizer, then press **Enroll**. Use your competition password, never your RuneScape or Jagex account password.
5. Keep RuneLite's built-in **Loot Tracker** enabled if your competition tracks loot.
6. If you want to upload screenshots, enable the relevant **Upload loot screenshots**, **Upload pet screenshots**, or **Upload completion screenshots** setting. Your organizer must also permit that screenshot type; Special Kill requires the organizer to enable Special Kill and you to enable completion screenshots.

Enrollment reads your currently logged-in RuneScape name automatically. There is no name to type. Reports are sent only while that character matches your registered character. Your enrollment is remembered between RuneLite sessions, so you do not need to enter the competition password each time.

To stop connecting to the event service, turn off **Enable event connection** or disable the plugin.

## Competition features

Your organizer controls which of these features are active. Competition settings refresh when the plugin starts and about every five minutes while playing.

| Feature | What it does |
| --- | --- |
| Loot | Groups qualifying item stacks from the same Loot Tracker event into one report. Organizer item filters and minimum values determine which drops count. |
| Pets | Reports the pet name, duplicate status, and the game's pet message. Pet screenshots have their own upload setting. |
| Levels and deaths | Reports level increases and player deaths. |
| Kill counts and completions | Reports the activity and count, with combined equipment and inventory risk value if your organizer enables it. Completion screenshots are optional. |
| Speedruns | Reports official game-generated completion times. |
| Custom game messages | Reports system messages matching the organizer's rules, such as a raid completion or new personal best. These rules do not read player conversations. |
| Special Kill | Records an attempt you explicitly start, including starting and ending screenshots, risk values, and the official completion time when available. |

For reliable pet names, enable RuneScape's **untradeable-drop notifications**. The plugin waits up to five game ticks for a matching untradeable-drop or collection-log message. If the game does not provide a name, it reports `Unknown pet`; Battleships will not guess a tile.

Loot filters can use item names, item IDs, or `*` wildcards such as `Godsword shard *`. Ignore rules always take priority. Minimum values apply to each item stack, rather than the combined drop. Ask your organizer if an expected item is filtered out.

## Risk values and Value Check

Press **Value Check** in the side panel while logged in to see your current equipment value, inventory value, and combined total. This check is entirely local: it sends no report and takes no screenshot.

Values use your organizer's item-value overrides first, RuneLite's market values second, and Jagex guide values when no market value is available. RuneLite's tradeable counterpart or component values are used for mapped untradeable items. For example, Ferocious gloves use Hydra leather's value.

When a completion screenshot includes a setup audit, its equipment and inventory panel shows quantity × unit value = line total. Labels identify organizer overrides, market values, mapped items, or guide values. The full item list is visible in that screenshot panel; it is not sent as a separate equipment or inventory list in event data.

## Screenshots

Loot, pet, and ordinary completion screenshots each require their corresponding organizer permission and your local upload setting. Special Kill requires organizer-enabled Special Kill, your completion screenshot setting, and an attempt you explicitly start. All three upload settings default to off.

Loot screenshots are also subject to your organizer's screenshot minimum. One screenshot accompanies the grouped loot report. Completion and Special Kill screenshots can include an equipment and inventory value audit.

**Hide chat in screenshots** is on by default and hides the chatbox and private-message widgets during capture. If you turn it off, visible messages may appear in uploaded images. Other visible game information can remain in screenshots.

## Special Kill

Special Kill records only attempts you start in the side panel:

1. Make sure your organizer has enabled Special Kill and you have enabled **Upload completion screenshots**.
2. Enter the **KC boss name** from your event instructions and press **Start Special Kill**. This captures the starting screenshot and risk value.
3. Complete the selected boss. Another boss does not finish the attempt. For **DKs** or **Dagannoth Kings**, complete all three kings.
4. The matching completion captures the ending screenshot and risk value. The report waits for the game's official time message when required.

The report includes RuneScape names visible in the loaded scene at completion and whether your starting equipment or inventory contained a Twisted bow, Scythe of vitur, or Tumeken's shadow. These details help organizers check event rules.

Press **Cancel Special Kill** to cancel an active attempt. Restarting RuneLite also clears it. A successful Special Kill report replaces the ordinary kill-count report for that completion.

## Changing your RuneScape name

Ask your organizer for a participant-specific, one-time RSN-change code. Log into your renamed character, enter the code under **Authorized RSN change**, and press **Change RSN**. The plugin reads the new name from the logged-in character. Codes expire after 15 minutes and can be used only once.

## Update notices

After login, the plugin displays a local chat notice once per release in each RuneLite profile. It summarizes relevant changes, calls out changes to shared data or screenshots, and points to this README. It also appears if you enable an updated plugin while already logged in. It does not repeat on later logins or world hops in the same profile.

The notice is displayed only in your client. Read the [patch notes](PATCH_NOTES.md) for release details.

## Privacy

The network connection is opt-in and disabled by default. When enabled, the plugin connects only to the official Imaj's Events service at `https://imajs-events-service.imajs-events.workers.dev`. Requests necessarily disclose your IP address to that service. The organizer's Discord webhook receives accepted reports and uploaded screenshots. You cannot redirect uploads to another service host from the plugin.

The plugin reads the currently logged-in RuneScape name for enrollment, authorized name changes, and checking that reports belong to your registered character. It does not inspect or modify another plugin's configuration.

Depending on the feature and settings, the plugin can send:

- **Enrollment:** the competition name and password you enter, together with the RuneScape name read from your logged-in character. The raw competition password is used for enrollment and is not stored by the plugin or service. The plugin stores an enrollment token to authenticate subsequent requests.
- **Name changes:** your current logged-in RuneScape name and the one-time organizer code, authenticated with your enrollment token.
- **Enabled events:** event type and client timestamp, qualifying loot item names, IDs, quantities, values and source; pet name, duplicate status and game message; skill and level; player death; activity and kill/completion count; official times; and matching system messages and their organizer-defined rule.
- **Setup values:** combined equipment and inventory risk-value totals and pricing information when enabled by your competition.
- **Special Kill:** boss and count, official completion time and its game message when available, starting and ending risk values and their decrease, pricing information, RuneScape names visible in the loaded scene at completion, and a boolean indicating whether the starting equipment or inventory contained a Twisted bow, Scythe of vitur, or Tumeken's shadow.
- **Screenshots:** game-view images for separately enabled loot, pet, or completion reports. Explicitly started Special Kill attempts send two screenshots with their value audits. These images can show equipment, inventory, and other visible game information. Chat and private-message widgets are hidden by default.

Custom message rules inspect only game-generated system messages. They do not read player chat, clan chat, friends chat, or private messages. **Value Check** and update notices are local and send nothing to the event service.

The service stores participant identity, hashed enrollment credentials, roster data, and accepted event metadata until the competition organizer requests removal. Uploaded screenshots are forwarded to the competition's Discord webhook and are not retained by the Imaj's Events service. Discord applies its own data practices to the messages and attachments it receives.

Questions and data-removal requests can be submitted through the [Imaj's Events service issue tracker](https://github.com/jamikhan112233/imajs-events-service/issues). See [PRIVACY.md](PRIVACY.md) for the full privacy summary. The plugin and service have no paid or subscription-gated functionality.

## License

BSD 2-Clause License.
