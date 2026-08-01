# Decisions

## Decision: Local-First Daily Analytics

Date: 2026-06-23

Context:
Users need progress feedback to return daily, but scrolling behavior is sensitive behavioral data.

Decision:
Store daily scroll totals, session counts, longest session duration, and highest awareness level locally in SharedPreferences.

Alternatives considered:
Cloud analytics, no analytics, database-backed event logging.

Tradeoffs:
SharedPreferences is simple and private but limited for long-term trend queries. A database will be better once weekly and app-specific reports require richer history.

Expected outcome:
Users see immediate daily progress without sacrificing trust or requiring accounts.


## Decision: Include Scroll Pace in Risk Evaluation

Date: 2026-06-23

Context:
Raw session duration and total scroll count can miss short, intense bursts of compulsive scrolling.

Decision:
Add scrolls-per-minute intensity scoring and include it as a first-class signal in doomscroll awareness evaluation.

Alternatives considered:
Only display pace without interventions; wait for a larger ML-style risk model.

Tradeoffs:
Simple thresholds are easier to explain and test, but they may need tuning after real-world usage.

Expected outcome:
Users receive earlier, more contextually relevant nudges during high-intensity scrolling sessions.


## Decision: Scroll Sessions Start on Actual Scrolling

Date: 2026-06-23

Context:
Counting a session as soon as a tracked app opens can inflate daily session totals and session duration when the user has not started scrolling.

Decision:
Treat app changes as a ready state, start counted sessions on the first scroll event, and reset sessions after 60 seconds of inactivity.

Alternatives considered:
Count every tracked app foreground event as a session; use a longer five-minute timeout.

Tradeoffs:
A 60-second timeout may split some slow reading sessions, but it keeps doomscroll metrics focused on active scrolling behavior.

Expected outcome:
Daily analytics better represent real scrolling behavior and interventions become less likely to fire after idle time.


## Decision: Dashboard Must Scroll Before Adding More Cards

Date: 2026-06-23

Context:
The dashboard now includes live session metrics and daily progress. Fixed-height centered content can clip on smaller phones or with larger accessibility font sizes.

Decision:
Make the primary dashboard vertically scrollable before adding more retention or analytics cards.

Alternatives considered:
Reduce typography and spacing; split metrics across multiple screens.

Tradeoffs:
A single scrollable screen is simpler and safer for accessibility, but future dense analytics may need dedicated report screens.

Expected outcome:
Users can access setup controls and progress metrics across device sizes without hidden content.


## Decision: Interventions Need a Snooze Escape Hatch

Date: 2026-06-24

Context:
Users may ignore, disable, or uninstall ScrollStop if interventions feel controlling during moments when they intentionally need temporary access.

Decision:
Add a notification action that snoozes interventions for 15 minutes while keeping tracking and daily analytics active. Guard notification posting when runtime notification permission is missing, while still allowing non-notification cues such as vibration.

Alternatives considered:
No override; a longer one-hour snooze; disabling the accessibility service.

Tradeoffs:
Snooze can reduce immediate intervention frequency, but it preserves user agency and should improve long-term trust and retention.

Expected outcome:
Users can temporarily opt out without abandoning the product, making interventions feel supportive rather than punitive.
