# Imaj's Events privacy information

Imaj's Events is an opt-in RuneLite competition client. Its network connection is disabled by default and connects only to `https://imajs-events-service.imajs-events.workers.dev` after the player enables it.

## Data sent

Enrollment sends the entered competition name, competition password, and the RuneScape name of the character currently logged into RuneLite. The raw password is used only for that request and is not stored by the plugin or service.

After enrollment, the plugin may send the following only when enabled by the competition profile:

- qualifying grouped loot details and source;
- pet name, duplicate status, and the game-generated pet message;
- level-up, player-death, kill-count/completion, and speedrun details;
- matching system/game messages selected by organizer-owned wildcard rules;
- combined equipment and inventory risk-value totals;
- optional loot, pet, and completion game-view screenshots separately enabled by the participant; and
- explicitly started Special Kill details, two screenshots, risk totals, and the official game-generated completion time.

Requests necessarily disclose the participant's IP address to the service. Custom rules do not inspect player chat, clan chat, friends chat, or private messages. The local Value Check feature sends nothing.

## Storage and forwarding

The service stores participant identity, hashed enrollment credentials, roster data, and accepted event metadata until the competition organizer requests removal. Uploaded screenshots are forwarded directly to the competition's Discord webhook and are not retained by the Imaj's Events service. Discord applies its own data practices to messages and attachments delivered to the organizer's webhook.

## Access and deletion

Competition organizers control their roster, destination Discord webhook, and enabled profile. Questions and data-removal requests can be submitted through the [Imaj's Events service issue tracker](https://github.com/jamikhan112233/imajs-events-service/issues).

## Payments

The plugin and service have no paid or subscription-gated functionality.
