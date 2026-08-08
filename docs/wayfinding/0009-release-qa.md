---
title: Release QA — signed build, debug off, billing test
type: task
status: open
blocked-by: []
---

## Question

Verify the release build behaves like a production install before anything is submitted.

Checklist (agent drives where it can; human runs the device steps):
- `./gradlew bundleRelease` produces a signed AAB (`app/build/outputs/bundle/release/`).
- On a fresh install of the release build: no "Debug premium override" toggle in Settings; Premium locked without a purchase; no debug prefs bleeding through (clean uninstall first).
- Smoke-test the enable flow, a reminder, the widget, and the cooldown/settings changes.
- With the license tester account: monthly + yearly purchase and Restore both work (ties to 0008).

Resolved when the release build passes and any defects found are filed as new tickets or fixed.
