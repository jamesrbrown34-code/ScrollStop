---
title: Decide the in-app accessibility disclosure and consent
type: grilling
status: open
blocked-by:
  - 0002-accessibility-fsi-review-risk
---

## Question

Play requires a prominent in-app disclosure + affirmative consent for the accessibility service: shown during normal usage, separate from the privacy policy, describing what the service accesses (scroll events in user-chosen tracked apps, local-only) and how it's used. The declaration also asks for a video of this flow.

Decide with the user (grilling + domain-modeling):
- Where it lives: inside the onboarding Enable step, or as a dedicated consent card/dialog.
- The exact wording — tie it to CONTEXT.md terms (tracked apps, on-device only, Premium).
- How consent is persisted and when it re-shows (first run only, after app updates, after accessibility is re-enabled).
- The copy/flow the declaration video will show.

The build follows this decision and blocks submission (0010).
