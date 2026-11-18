# Patch notes

## 0.4.0 — Special Kill integration improvements

- Added organizer-defined Special Kill validation support.
- Special Kill submissions can include nearby player names and limited validation metadata when enabled by the competition profile.
- Updated the plugin description to: `Connects your RuneLite client to a competition hosted by Imaj's Events`.

## 0.3.0 — Pet events and safer enrollment

- Added pet-drop notifications with pet name and duplicate status.
- Added optional pet screenshots, requiring both organizer approval and local player consent.
- Enrollment no longer accepts a typed RSN; it uses the character currently logged into RuneLite.
- Every outbound event verifies the logged-in character matches the registered RSN.
- One-time RSN-change codes now apply to the currently logged-in renamed character.
- Re-enrolling an approved RSN rotates the previous token so stale clients cannot lock the roster entry.
- Added Battleships-compatible `Pet Drop` Discord formatting.
