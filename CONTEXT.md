# ScrollStop

ScrollStop is an Android accessibility service that detects doomscrolling and nudges the user to stop. A **Premium** tier unlocks custom reminder controls, per-app controls, themes, streak features, and daily summaries; free users get a fixed reminder cadence with respectful limits.

## Language

**Premium**:
The paid tier that unlocks custom **reminder limits**, **reminder tones**, **per-app reminder limits**, **per-app reminder tones**, **themes**, the **streak freeze**, **per-app quiet hours**, **per-day goals**, the **daily summary notification**, and the **streak milestone** badge gallery.
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
The voice used for scroll-reminder messages (Generic, Motivator, Drill Sergeant, Action Hero, Friend, Zen), selectable by **Premium** users.
_Avoid_: Voice pack, scroll tone

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

**Per-app quiet hours**:
A **Premium** opt-out of the global **quiet hours** window for individual **tracked apps**; apps opted out keep sending reminders during quiet hours.
_Avoid_: App-specific quiet hours

**Pause tracking**:
An ad-hoc stop of tracking and reminders for a set duration, started from a reminder notification; scrolling during a pause is not counted.
_Avoid_: Snooze, break, suspension

**Today's goal**:
A daily scroll cap the user chooses to stay under, shown as on-track progress on the dashboard; separate from the **reminder limit**.
_Avoid_: Daily limit, scroll target, goal

**Per-day goals**:
A **Premium** variation of **today's goal** — a different goal for each weekday, falling back to the default goal when a day is unset.
_Avoid_: Goal per day, custom schedule

**Reminder style**:
How a scroll reminder is delivered on screen — Heads-up (expandable banner) or Full-screen (screen takeover where the phone allows it). A Free setting, global.
_Avoid_: Notification size, popup style

**Reminder cooldown**:
The minimum time ScrollStop waits between reminders, configurable in minutes. A Free setting, global.
_Avoid_: Reminder gap, throttle, quiet interval

**Daily summary notification**:
A **Premium** once-a-day local notification at a user-chosen time summarising the day's scrolling: scrolls, reminders given, and top app.
_Avoid_: Daily report, digest

**Streak freeze**:
A **Premium** feature that protects the dashboard streak — one missed day is skipped without breaking the streak. Automatic, no user action needed.
_Avoid_: Day-skip, grace day

**Streak milestones**:
Celebration points on the dashboard when the streak reaches set lengths (7, 14, 30, 60, 100, 200, 365). Basic milestones are Free; the **Premium** badge gallery shows the full set.
_Avoid_: Achievements, trophies

**Alternative apps**:
A **Premium** feature where the user picks a healthier app per tracked app; reminders for that app suggest the alternative. Currently designed, not yet built.
_Avoid_: Suggestions, replacements

**Break prompts**:
A **Premium** full-screen "take a breath" card shown after a critical reminder, guiding a short breathing break. Currently designed, not yet built.
_Avoid_: Meditate mode, breathe overlay

**Monthly breakdown**:
A Free Progress-tab view showing the last 12 months' scroll totals as a bar chart, the current month's total, and a month-over-month comparison against the previous month.
_Avoid_: Month view, monthly chart

**This Year**:
A **Premium** Progress-tab view showing the year-to-date scroll total, the best month, the average scrolls per day, and a this-year-vs-last-year comparison where data allows.
_Avoid_: Year view, YTD report

**Long-term trend**:
A **Premium** Progress-tab view charting the last 6 months and comparing the last three months with the previous three.
_Avoid_: Trend chart, history view

**Reduction plan**:
A multi-week programme with a declining weekly scroll target, chosen by the user from four difficulty levels — Easy, Medium, Hard, or Extreme. Each week's target is a fraction of the **plan baseline**; the plan tightens the effective **reminder limit** toward that target. A Free feature, separate from **today's goal** and the **reminder limit**.
_Avoid_: Scrolling challenge, cut-down programme

**Plan baseline**:
The weekly scrolling measured when a **reduction plan** starts (the sum of the last seven complete days); every **plan week**'s target is a fraction of it. A **reduction plan** cannot start until at least three of the last seven days have scroll data.
_Avoid_: Starting scrolls, reference week

**Plan week**:
A 7-day block counted from a **reduction plan**'s start date; the UI shows "Week X of Y". Weeks roll over automatically; the target is clamped to the final week once the plan completes.
_Avoid_: Stage, phase

**Passive scrolling**:
The share of a day's (or **tracked app**'s) scrolls from **scrolling sessions** with no interaction. Designed as a **Premium** insight — not yet built — and never affects reminders, **today's goal**, or the streak.
_Avoid_: Bad scrolling, mindless scrolling, zombie scrolling

**Engaged scrolling**:
The complement of **passive scrolling** — scrolls in **scrolling sessions** where the user interacted at least once; not a quality judgement. Designed, not yet built.
_Avoid_: Good scrolling, useful scrolling, interactive scrolling

**Taps**:
User activations of tappable views in a **tracked app** (Android `TYPE_VIEW_CLICKED`); a counting unit that would feed **passive scrolling**, never a synonym for scrolls. Designed, not yet built.
_Avoid_: Clicks

**Scrolling session**:
A continuous period in a **tracked app** from first scroll or app open until the app leaves the foreground. Currently in-memory only and needs proper flushing before any **passive scrolling** insight can use it.
_Avoid_: Browsing session

**Debug premium override**:
A debug-build-only mechanism that forces **Premium entitlement** to `true` regardless of billing status, so premium behaviour can be tested without a real purchase. A debug-only toggle on the Settings tab switches it on and off; it defaults to off in debug builds and is ignored in release.
_Avoid_: Developer mode, test mode

## Relationships

- **Premium entitlement** unlocks custom **reminder limits**, **reminder tones**, **per-app reminder limits**, **per-app reminder tones**, **themes**, the **streak freeze**, **per-app quiet hours**, **per-day goals**, the **daily summary notification**, and the **streak milestone** badge gallery.
- **Tracked apps** are monitored per-app, producing per-app scroll analytics that **per-app reminder limits** build on.
- **Per-app reminder limits** and **per-app reminder tones** are overrides; if absent the global **reminder limit** and **reminder tone** are used.
- **Quiet hours** and **pause tracking** are Free capabilities that pause reminders; **quiet hours** still count scrolling, **pause tracking** does not.
- **Per-app quiet hours** is an opt-out of the global **quiet hours** window: every app follows the window unless it's explicitly opted out.
- **Reminder style** and **reminder cooldown** are Free global settings; **reminder cooldown** applies to all reminders regardless of tier.
- **Today's goal** is a Free daily scroll target, separate from the **reminder limit**; **per-day goals** override it per weekday for **Premium** users.
- **Streak freeze** automatically protects the streak when a day is missed; it's **premium-only**. **Streak milestones** celebrate reaching set streak lengths.
- **Monthly breakdown** is a Free Progress-tab view; **This Year** and **Long-term trend** are its **Premium** companions, all derived from the retained daily analytics.
- **Reduction plan** builds its weekly targets from the **plan baseline** and rolls through **plan weeks**; as targets decline, the plan tightens the effective **reminder limit** toward each week's allowance. It is separate from **today's goal** and the **reminder limit**.
- **Alternative apps**, **break prompts**, **passive scrolling**, **engaged scrolling**, and **taps** are designed Premium features not yet in the app; **passive scrolling** builds on **scrolling sessions**.
- **Debug premium override** (debug builds only) forces **Premium entitlement** to `true`.

## Example dialogue

> **Dev:** "In the debug build, why is premium unlocked without a purchase?"
> **Domain expert:** "Because the **debug premium override** forces **premium entitlement** to `true` — billing is never consulted in debug builds, so you can test premium behaviour without paying."
>
> **Dev:** "But I want to see what free users see."
> **Domain expert:** "Flip the **debug premium override** toggle off at the bottom of the main screen — **premium entitlement** drops and the app behaves as a free install. It's a debug-only switch; release builds ignore it."

## Flagged ambiguities

- "Premium features" was used loosely; the paid capabilities include custom **reminder limits**, **reminder tones**, **per-app overrides**, **themes**, the **streak freeze**, **per-app quiet hours**, **per-day goals**, the **daily summary notification**, and the **streak milestone** badge gallery; the paid tier is named **Premium**.
- "Purchased" vs "entitled": the code stores real Play Billing status in `BillingState.isPremium`, but **premium entitlement** is what actually gates behaviour (via `PremiumManager`). In debug builds the override and the billing value diverge by design.
- "Reminder style" was once avoided as a synonym for **reminder tone**; it is now its own canonical term (Heads-up vs Full-screen delivery).
- "Useful vs bad scrolling" was evaluated and rejected (see `docs/scrolling-quality.md`): no-interaction scrolling already *is* doomscrolling, and "tapped = good" is a false proxy. Only the descriptive **passive scrolling**/**engaged scrolling** framing is retained, as designed Premium insights not yet built.
