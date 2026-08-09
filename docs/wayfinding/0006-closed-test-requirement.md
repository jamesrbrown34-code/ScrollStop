---
title: Confirm the closed-testing requirement for this account
type: research
status: closed
blocked-by: []
---

## Question

Google Play's new-account rules (personal accounts created after Nov 2023) can require a closed test with 12+ opted-in testers for 14 days before production is granted. Does this apply to ScrollBeat's account, and exactly how do we satisfy it?

Research (AFK, via a research subagent):
- The current Play Console testing requirements for new personal vs business accounts.
- The exact criteria (12 testers, 14 days, closed-test track), how they're counted, and how to declare them in the console.
- Whether an app with a subscription/accessibility service is treated differently.

The answer feeds 0007 (track strategy) and 0010 (submission timing). Depends on the account type from 0001 — if that's unresolved, answer for the personal-account case and note the business-account variant.

## Resolution (2026-08-05, research subagent)

- The 12-tester / 14-consecutive-days closed test applies to **personal accounts created after 13 Nov 2023**. Production and Pre-registration tracks stay disabled until met; days are consecutive per tester (opting out resets that tester's clock).
- **Business/organisation accounts are exempt** but require a D-U-N-S number + identity verification.
- New personal accounts also require **device verification** (physical, non-rooted Android 10+ via the Play Console app).
- Practical path: Closed testing track → tester email list → 12+ opted-in for 14 consecutive days → "Apply for production" on the Dashboard → review usually ≤7 days.
- Consequence: budget ~3–4 weeks. The account type recorded in **Create the Play developer account and app shell** (0001) decides which path applies, and the device-verification step was added to 0001.

