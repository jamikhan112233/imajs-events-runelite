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
- does not inspect, depend on, or modify Dink or any other plugin configuration;
- does not read the currently logged-in RuneScape name.

The current supported notifications are qualifying loot, level-ups, and player death. The repository README documents the exact data flow and manual test plan.

Network/privacy warning: enabling the connection submits the user's IP address, user-entered RSN, and enabled competition event details to a third-party Imaj's Events service that is not controlled or verified by RuneLite developers.

Generated-by: Claude Code
