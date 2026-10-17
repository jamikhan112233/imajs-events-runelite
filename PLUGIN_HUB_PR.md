# Plugin Hub pull-request draft

## Title

Add Imaj's Events

## Description

Adds Imaj's Events, an opt-in competition notification client.

The plugin:

- is disabled for third-party networking by default;
- connects only to the official Imaj's Events Worker and requires the user to explicitly enable the connection;
- enrolls with a competition name, one-time-use-in-client competition password, and user-entered RSN;
- stores only an opaque enrollment token, not the competition password;
- sends only event types enabled by the server-owned competition profile;
- supports organizer-authorized, participant-specific one-time RSN changes;
- uses injected RuneLite `OkHttpClient` and `Gson`;
- performs no blocking network calls on the client thread;
- does not inspect or modify any other plugin configuration;
- does not read the currently logged-in RuneScape name.
- has no paid or subscription-gated functionality.

The current supported notifications are qualifying grouped Loot Tracker drops, level-ups, the enrolled player's death, kill counts/completions, speedrun time messages, and organizer-defined system-message patterns. Kill-count reports can include a combined equipment/inventory risk-value total. Loot and completion screenshots are sent only when the server-owned competition profile and the player both opt in. Special Kill is explicitly started by the player and sends starting/ending screenshots, risk totals, and the official game-generated completion time. Custom message rules inspect only RuneLite system/game-message types, not player conversations.

Network/privacy warning: enabling the connection submits the user's IP address, entered RSN, enabled competition event data, combined risk-value totals, and separately opted-in game-view screenshots to a third-party Imaj's Events service that is not controlled or verified by RuneLite developers. The service retains participant, roster, credential-hash, and accepted event metadata until organizer-requested removal. Uploaded screenshots are forwarded directly to the competition Discord webhook and are not retained by the service.

