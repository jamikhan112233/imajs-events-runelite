# Patch notes

## 0.4.0 — Special Kill integration improvements

- Added a local login notice once per release per RuneLite profile, including a summary of Special Kill data and screenshot behavior and a README link.
- Reworked the README for players and corrected its privacy disclosures, including automatic logged-in RSN use and all Special Kill data. Moved developer and submission guidance into `CONTRIBUTING.md`.
- Added organizer-defined Special Kill validation support.
- Special Kill submissions include RuneScape names visible in the loaded scene at completion and a boolean indicating whether the starting equipment or inventory contained a Twisted bow, Scythe of vitur, or Tumeken's shadow. Special Kill requires organizer permission, local completion screenshot consent, and an attempt explicitly started by the player.
- Updated the plugin description to: `Connects your RuneLite client to a competition hosted by Imaj's Events`.

## 0.3.0 — Pet events and safer enrollment

- Added pet-drop notifications with pet name and duplicate status.
- Added optional pet screenshots, requiring both organizer approval and local player consent.
- Enrollment no longer accepts a typed RSN; it uses the character currently logged into RuneLite.
- Every outbound event verifies the logged-in character matches the registered RSN.
- One-time RSN-change codes now apply to the currently logged-in renamed character.
- Re-enrolling an approved RSN rotates the previous token so stale clients cannot lock the roster entry.
- Added Battleships-compatible `Pet Drop` Discord formatting.
