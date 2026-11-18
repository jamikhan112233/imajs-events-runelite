# Patch notes

## 0.4.0 — Budget kills and group verification

- Added `DKs` as a Special Kill target. One attempt completes only after Rex, Prime, and Supreme have all been killed.
- Special Kill submissions now include nearby player names so team group kills can require matching submissions from every participant.
- Special Kill submissions now include whether the starting setup contained a Twisted bow, Scythe of vitur, or Tumeken's shadow for the Inferno no-megas rule.
- Updated the plugin description to: `Connects your RuneLite client to a competition hosted by Imaj's Events`.

## 0.3.0 — Pet events and safer enrollment

- Added pet-drop notifications with pet name and duplicate status.
- Added optional pet screenshots, requiring both organizer approval and local player consent.
- Enrollment no longer accepts a typed RSN; it uses the character currently logged into RuneLite.
- Every outbound event verifies the logged-in character matches the registered RSN.
- One-time RSN-change codes now apply to the currently logged-in renamed character.
- Re-enrolling an approved RSN rotates the previous token so stale clients cannot lock the roster entry.
- Added Battleships-compatible `Pet Drop` Discord formatting.
