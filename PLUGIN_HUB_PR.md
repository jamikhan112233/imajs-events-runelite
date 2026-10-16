# Plugin Hub pull-request draft

## Title

Add Imaj's Events

## Description

Adds Imaj's Events, an opt-in competition notification client.

The plugin:

- is disabled for third-party networking by default;
- requires the user to enter a service URL and explicitly enable the connection;
- enrolls with a competition name, one-time-use-in-client competition password, and user-entered RSN;
- stores only an opaque enrollment token, not the competition password;
- sends only event types enabled by the server-owned competition profile;
- supports organizer-authorized, participant-specific one-time RSN changes;
- uses injected RuneLite `OkHttpClient` and `Gson`;
- performs no blocking network calls on the client thread;
- does not inspect or modify any other plugin configuration;
- does not read the currently logged-in RuneScape name.

The current supported notifications are qualifying loot, level-ups, player death, kill counts/completions, speedrun time messages, and organizer-defined system-message patterns. Kill-count reports can include current equipment/inventory names, quantities, Grand Exchange values, and a screenshot only when the competition profile and the player both opt in. Custom message rules inspect only RuneLite system/game-message types, not player conversations.

Network/privacy warning: enabling the connection submits the user's IP address, user-entered RSN, and enabled competition event details to a third-party Imaj's Events service that is not controlled or verified by RuneLite developers. If enabled, completion reports may also submit equipment/inventory details, Grand Exchange values, and an optional screenshot.

