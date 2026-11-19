# Imaj's Events privacy information

Imaj's Events is an opt-in RuneLite competition client. Its network connection is disabled by default and connects only to `https://imajs-events-service.imajs-events.workers.dev` after the player enables it.

## Data sent

Enrollment sends the entered competition name, competition password, and the RuneScape name of the character currently logged into RuneLite. The raw password is used only for that request and is not stored by the plugin or service.

The plugin reads the logged-in name for enrollment, authorized RSN changes, and checking reports against the registered character. RSN-change requests send the current logged-in name and the one-time organizer code. An enrollment token stored in the RuneLite profile authenticates subsequent requests and associates reports with the registered character and competition.

After enrollment, the plugin may send the following only when enabled by the competition profile:

- event type, client timestamp, and qualifying grouped loot item names, IDs, quantities, values, and source;
- pet name, duplicate status, and the game-generated pet message;
- level-up, player-death, kill-count/completion, and speedrun details;
- matching system/game messages selected by organizer-owned wildcard rules;
- combined equipment and inventory risk-value totals;
- optional loot, pet, and completion game-view screenshots separately enabled by the participant; and
- explicitly started Special Kill boss and count, two screenshots with equipment/inventory value audits, starting and ending risk totals and their decrease, pricing information, the official game-generated completion time and its game message when available, the RuneScape names visible in the loaded scene at completion, and a boolean indicating whether the starting equipment or inventory contained a Twisted bow, Scythe of vitur, or Tumeken's shadow.

Requests necessarily disclose the participant's IP address to the service. Custom rules do not inspect player chat, clan chat, friends chat, or private messages. The plugin does not inspect or modify another plugin's configuration. The local Value Check feature and login update notices send nothing to the service.

All screenshot upload settings are off by default. Loot, pet, and ordinary completion screenshots each require organizer permission and the corresponding local upload setting. Special Kill requires organizer-enabled Special Kill, local completion screenshots, and an attempt the player explicitly starts. Screenshots can contain visible game information, including equipment and inventory. Chat and private-message widgets are hidden during capture by default; turning off that setting can include visible messages. The full equipment/inventory list is not sent as a separate event-data field, but can appear in the screenshot's value audit.

The plugin remembers the last update notice version in the RuneLite profile so notices do not repeat on every login or world hop. This notice is local to the participant's client and is not a public chat broadcast.

## Storage and forwarding

The service stores participant identity, hashed enrollment credentials, roster data, and accepted event metadata until the competition organizer requests removal. Uploaded screenshots are forwarded directly to the competition's Discord webhook and are not retained by the Imaj's Events service. Discord applies its own data practices to messages and attachments delivered to the organizer's webhook.

## Access and deletion

Competition organizers control their roster, destination Discord webhook, and enabled profile. Questions and data-removal requests can be submitted through the [Imaj's Events service issue tracker](https://github.com/jamikhan112233/imajs-events-service/issues).

## Payments

The plugin and service have no paid or subscription-gated functionality.
