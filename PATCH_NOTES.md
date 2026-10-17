# Patch notes

## 0.3.0 — Pet events and safer enrollment

- Added pet-drop notifications with pet name and duplicate status.
- Added optional pet screenshots, requiring both organizer approval and local player consent.
- Enrollment no longer accepts a typed RSN; it uses the character currently logged into RuneLite.
- Every outbound event verifies the logged-in character matches the registered RSN.
- One-time RSN-change codes now apply to the currently logged-in renamed character.
- Re-enrolling an approved RSN rotates the previous token so stale clients cannot lock the roster entry.
- Added Battleships-compatible `Pet Drop` Discord formatting.
