---
title: Configure the subscriptions and pricing in Play Console
type: task
status: open
blocked-by:
  - 0001-play-account-and-app
---

## Question

Create the two subscriptions so the in-app paywall purchases work, matching the product IDs in code exactly.

Hands a human a checklist (from `play_console_guide.md`):
- Subscription `scrollstop_monthly`: base plan, 1-month auto-renewing, $1.99 (or localised prices), 7-day free trial.
- Subscription `scrollstop_yearly`: base plan, 1-year auto-renewing, $14.99, 7-day free trial.
- Add yourself as a license tester and run a test purchase + restore on a release build (see 0009).

Resolved when both subscriptions exist and a test purchase completes without charge. Record the subscription/offer IDs and any pricing-market notes in the resolution comment.
