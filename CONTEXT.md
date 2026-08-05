# ScrollStop

ScrollStop is an Android accessibility service that detects doomscrolling and nudges the user to stop. A **Premium** tier unlocks custom scroll-reminder limits, tones, per-app controls, and a streak freeze; free users are capped at a fixed limit.

## Language

**Premium**:
The paid tier that unlocks custom **reminder limits**, **reminder tones**, **per-app reminder limits**, **per-app reminder tones**, and the **streak freeze**.
_Avoid_: Pro, premium features, upgrade

**Premium entitlement**:
Whether a user has valid, active access to Premium.
_Avoid_: Purchased, subscribed, owns premium

**Reminder limit**:
The number of scrolls between reminders, customizable by **Premium** users.
_Avoid_: Scroll limit, scroll-reminder limit

**Per-app reminder limit**:
A per-app override of the global **reminder limit**, set by **Premium** users for individual **tracked apps**.
_Avoid_: App-specific limit

**Reminder tone**:
The voice used for scroll-reminder messages (Generic, Motivator, Drill Sergeant, Action Hero), selectable by **Premium** users.
_Avoid_: Voice pack, reminder style, scroll tone

**Per-app reminder tone**:
A per-app override of the global **reminder tone**, set by **Premium** users for individual **tracked apps**.
_Avoid_: App-specific tone

**Theme**:
A **Premium** styling option that changes the app's colour accent (Monochrome, Forest, Ocean, Ember); free users get Monochrome only.
_Avoid_: Colour scheme, skin, appearance pack

**Tracked apps**:
The set of apps ScrollStop monitors, chosen by the user from a curated library of known scrolling apps.
_Avoid_: Target apps, watched apps, filter list

**Quiet hours**:
A daily time window during which scroll reminders are silenced; scrolling is still counted, just not interrupted.
_Avoid_: Do-not-disturb, sleep mode, silent hours

**Pause tracking**:
An ad-hoc stop of tracking and reminders for a set duration, started from a reminder notification; scrolling during a pause is not counted.
_Avoid_: Snooze, break, suspension

**Today's goal**:
A daily scroll cap the user chooses to stay under, shown as on-track progress on the dashboard; separate from the **reminder limit**.
_Avoid_: Daily limit, scroll target, goal

**Streak freeze**:
A **Premium** feature that protects the dashboard streak — one missed day is skipped without breaking the streak. Automatic, no user action needed.
_Avoid_: Day-skip, grace day

**Debug premium override**:
A debug-build-only mechanism that forces **Premium entitlement** to `true` regardless of billing status, so premium behaviour can be tested without a real purchase. A debug-only toggle on the main screen switches it on and off; it defaults to on in debug builds and is ignored in release.
_Avoid_: Developer mode, test mode

## Relationships

- **Premium entitlement** unlocks custom **reminder limits**, **reminder tones**, **per-app reminder limits**, **per-app reminder tones**, **themes**, and the **streak freeze**.
- **Tracked apps** are monitored per-app, producing per-app scroll analytics that **per-app reminder limits** build on.
- **Per-app reminder limits** and **per-app reminder tones** are overrides; if absent the global **reminder limit** and **reminder tone** are used.
- **Quiet hours** and **pause tracking** are Free capabilities that pause reminders; **quiet hours** still count scrolling, **pause tracking** does not.
- **Today's goal** is a Free daily scroll target, separate from the **reminder limit**.
- **Streak freeze** automatically protects the streak when a day is missed; it's **premium-only**.
- **Debug premium override** (debug builds only) forces **Premium entitlement** to `true`.

## Example dialogue

> **Dev:** "In the debug build, why is premium unlocked without a purchase?"
> **Domain expert:** "Because the **debug premium override** forces **premium entitlement** to `true` — billing is never consulted in debug builds, so you can test premium behaviour without paying."
>
> **Dev:** "But I want to see what free users see."
> **Domain expert:** "Flip the **debug premium override** toggle off at the bottom of the main screen — **premium entitlement** drops and the app behaves as a free install. It's a debug-only switch; release builds ignore it."

## Flagged ambiguities

- "Premium features" was used loosely; the paid capabilities include custom **reminder limits**, **reminder tones**, **per-app overrides**, and the **streak freeze**; the paid tier is named **Premium**.
- "Purchased" vs "entitled": the code stores real Play Billing status in `BillingState.isPremium`, but **premium entitlement** is what actually gates behaviour (via `PremiumManager`). In debug builds the override and the billing value diverge by design.
