---
title: Assess Play review risk: accessibility service and full-screen permission
type: research
status: closed
blocked-by: []
---

## Question

What does Google Play currently require of apps that declare an accessibility service and the `USE_FULL_SCREEN_INTENT` permission, and how real is the rejection risk for ScrollBeat?

Research (AFK, via a research subagent):
- Play's accessibility-service policy: what the declaration must say, any in-app "prominent disclosure" requirements, and how reviewers treat scroll-counting utilities.
- Play's policy on `USE_FULL_SCREEN_INTENT` (usually alarm/call apps): does a doomscroll reminder app qualify, and what happens if the declaration is weak — rejection, or a question-and-answer round?
- Whether an app using accessibility must avoid other Play restrictions (data safety interplay).

The answer records the facts a later decision (0003) and the submission (0010) depend on.

## Resolution (2026-08-05, research subagent)

- The accessibility declaration is mandatory for any API 31+ app with an `AccessibilityService`. Scroll-counting is a **permitted** "App functionality" use, but the app must present a **prominent in-app disclosure + affirmative consent** (checkbox/tap, separate from the privacy policy, shown during normal usage) describing what the service accesses and how it's used; the declaration also asks for a video of the disclosure flow.
- The API usage must be **documented in the Play listing** (store description).
- `USE_FULL_SCREEN_INTENT`: not an automatic rejection, but Play revokes the default grant for non-alarm/call apps and requires an FSI declaration + runtime prompt + graceful degradation. "Declare but never use" is disallowed. Community-observed: reviewers commonly advise non-alarm apps to remove it entirely.

Consequence: an in-app accessibility disclosure/consent is required before submission — graduated to **Decide the in-app accessibility disclosure and consent** (0011). The FSI facts now decide 0003.

