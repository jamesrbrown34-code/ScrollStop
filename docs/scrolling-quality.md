# Scrolling quality: "useful vs bad scrolling"

**Status:** Recommendation (no code written)
**Audience:** Product owner, domain model
**Question:** "Can we set up useful scrolling vs bad scrolling — i.e. scrolling with no clicks is deemed bad because you're just scrolling without interacting? Is this good, or does it break away from the point of the app?"

**Verdict up front:** The framing inverts the app's purpose. "Scrolling with no interaction" is *not* a new bad thing to detect — it **is** doomscrolling, the thing the app already detects. Labelling it "bad" is redundant, and "tapped = good" is a false signal (tap-driven feed-hopping on Shorts/Reels/TikTok is among the worst doomscrolling). **Do not build "useful vs bad scrolling" as a classification.** A narrower, purely descriptive **passive scrolling** insight (Premium, never drives reminders or goals) is the only version that fits the app — and it should be treated as optional/deferred, not core.

---

## 1. How ScrollBeat counts scrolling today

The full detection chain:

1. `ScrollMonitorService.onAccessibilityEvent` (`service/ScrollMonitorService.kt`) receives events and immediately drops anything not from a **tracked app** (`TrackedAppsGraph...isTracked(packageName)`). Only two event types are registered (`res/xml/accessibility_service_config.xml`): `TYPE_WINDOW_STATE_CHANGED` (app open/switch → starts a session) and `TYPE_VIEW_SCROLLED` (→ counts one scroll). `canRetrieveWindowContent="false"`, `notificationTimeout="100"` (≈100 ms coalescing).
2. `ScrollTracker.onScrollEvent` (`core/ScrollTracker.kt`) skips counting while **pause tracking** is active, starts a per-app session if none, increments the session `scrollCount`, calls `analytics.recordScroll(packageName, nowWall)`, and evaluates `DoomscrollAwareness`.
3. `LocalScrollAnalyticsRepository` (`data/ScrollAnalyticsRepository.kt`) persists two stores, each with 365-day retention in SharedPreferences JSON:
   - `ScrollEvent` — per-day: `scrolls`, `interruptionsShown`, `hourlyScrolls`.
   - `AppScrollEvent` — per-day per-app: `scrolls`, `interruptionsShown`.
4. `DoomscrollAwareness` (`core/DoomscrollAwareness.kt`) raises a session tier by time (2/5/10 min) or session scrolls (100/300/500), and a daily tier for reminder copy.
5. `ScrollTracker` fires a **reminder** when the daily count hits the **reminder limit** cadence (Free: global daily count % `reminderLimit`; Premium: per-app count % per-app limit) — gated by **quiet hours**, **reminder cooldown**, and pause.

### What "a scroll" means in the model

A scroll is *any* `TYPE_VIEW_SCROLLED` event from a tracked app while tracking isn't paused. It does not distinguish **how** the user scrolled — drag, fling, wheel, or programmatic/auto-scroll (feeds that scroll on their own, overscroll effects) all count. There is no notion of "good" or "bad" scrolls anywhere in the model; **every** scroll is the tracked unit and every scroll is equally eligible to advance the reminder cadence, the **today's goal**, and the streak. That is deliberate and worth preserving.

A **session** exists only in memory in `ScrollTracker` (`sessionApp`, `sessionStartTime`, `scrollCount`). Sessions are *not persisted*. They end only on `resetToIdle()` (service connect/unbind) or when another tracked app opens — switching to the home screen or an untracked app does **not** end a session, because untracked `TYPE_WINDOW_STATE_CHANGED` events are dropped by the guard before they reach `onAppChanged`.

---

## 2. What "useful vs bad scrolling" would technically require

A "bad = no interaction" classifier needs:

- **New accessibility events observed:** `TYPE_VIEW_CLICKED` (and optionally `TYPE_VIEW_LONG_CLICKED`) added to `android:accessibilityEventTypes` in `accessibility_service_config.xml`, plus a new handler in `ScrollMonitorService`. Click events are among the highest-frequency, noisiest accessibility events (they fire for buttons, list items, media controls, IME keys, ad surfaces).
- **Attributing scrolls to "interacted or not":** because sessions aren't persisted and boundaries are fuzzy, this needs either per-session in-memory engagement state plus a *new session-flush mechanism* (untracked window change, an idle timer, midnight rollover), or per-scroll look-back windows. There is no per-scroll tap timestamp today, so per-scroll attribution is a genuinely new subsystem.
- **A definition of "interaction" that is stable:** distinguishing a tap-on-feed-content from a tap-on-a-function (pause, settings, share) would require inspecting view classes/roles — with `canRetrieveWindowContent="false"` you only get what rides on the event (`className`), so any refinement is heuristic.
- **Aggregation/analytics changes:** new counters on `ScrollEvent`/`AppScrollEvent` (per-day and per-app), encoder/decoder migration, and new dashboard aggregates.
- **Decisioning changes (the dangerous part):** the owner's framing implies "bad" scrolls are the ones that should matter — feeding reminders, the goal, or the streak. That rewrites the app's core cadence.

### The privacy/disclosure cost

ScrollBeat's position is local-only, on-device, minimal footprint. The wayfinding map treats the accessibility disclosure as a first-class submission blocker (`docs/wayfinding/0011-accessibility-disclosure.md`). Adding click events changes what the service "accesses" — the disclosure copy, the Play store description, and the data-safety answers would all have to be updated to say the service observes taps, not just scrolls. That is a real, visible cost, not an implementation detail.

---

## 3. The product question, answered head-on

### Why "no taps = bad" misreads the app (the case against)

1. **It is redundant.** The app's entire premise is that passive, consumption-only scrolling is the problem. Every scroll it already counts, and every reminder it already fires, targets exactly the behaviour the proposal would call "bad". Re-labelling no-tap scrolling as "bad" adds a second word for the same thing and implies the other scrolls are fine — weakening the message "scrolling is the thing to watch."
2. **"No taps" is a weak, sometimes inverted proxy for "bad".** Tap-driven doomscrolling is real and often *worse*: rapid tap-through of Shorts/Reels/TikTok is high-intensity consumption with perfect "engagement". A classifier built on "taps = useful" would call the worst sessions the good ones. It cannot distinguish a tap on a video (consumption) from a tap that actually serves a goal.
3. **Taps are noisy and unrelated to scrolling quality.** People tap for reasons that have nothing to do with productive use: pausing, liking reflexively, opening a link, dismissing an ad, adjusting volume. Engagement is not usefulness.
4. **It breaks the domain vocabulary.** The glossary explicitly forbids "clicks" as a synonym for scrolls and keeps one clean unit (scrolls). A "good vs bad scroll" axis reintroduces exactly the muddle the domain model exists to prevent, and gives users a rationalisation: "I tapped, so this isn't doomscrolling."
5. **It adds UI and mental-model burden.** The dashboard is opinionated around a single number: scrolls today. A "mindless vs engaged" split adds a second, competing framing of the day, and the copy risk of judging users ("bad scrolling") is high.
6. **It grows the footprint** (accessibility disclosure, noisiest event type, battery) — against the app's stated local-only/minimal stance.

### The case for *something* here

The underlying instinct is legitimate: not all scrolling feels alike, and "was I interacting or just absorbing?" is a useful question about one's day. A **descriptive** answer to that question — without classifying scrolls as good/bad and without changing any behaviour — could make Premium analytics meaningfully richer.

### Recommendation

1. **Reject "useful vs bad scrolling" as framed.** Do not classify scrolls, do not relabel them, do not feed a "badness" signal into reminders, the **today's goal**, or the streak. Any version that makes taps affect *what the app nags about* drifts from the point of the app and is genuinely harmful to its message.
2. **If we want the insight at all, build the narrow version: "passive scrolling"** — a Premium-only, purely informational metric that reports the share of a day's (and a tracked app's) scrolls that happened in sessions with no interaction. It keeps the invariant "every scroll is the tracked unit" and never changes behaviour. It should be gated Premium (consistent with `AppDominanceCard` / `HourlyHeatmapCard` / `MilestonesCard` in `ProgressTab.kt`) and deferred to the post-launch feature track — it is not core value, and the accessibility disclosure change it forces should not block launch (see `plan.md` non-goals and the wayfinding map's stance that new Premium features are a separate effort).

Design for the narrow version follows.

---

## 4. Design: "passive scrolling" (Premium, descriptive-only)

### 4.1 Ground rules

- `taps` and `passiveScrolls` are **counting units**, never synonyms for scrolls, and never "clicks".
- **Nothing** here feeds `DoomscrollAwareness`, the **reminder limit** cadence, the **today's goal**, or the streak. Reminder copy is untouched.
- Free users see nothing new; data is still tracked in the background (cheap, mirroring how per-app analytics are recorded for all users), so an upgrade retro-fills the insight.

### 4.2 Accessibility events

Add `typeViewClicked` to `android:accessibilityEventTypes` in `accessibility_service_config.xml` (skip `LONG_CLICKED` — marginal and noisier). In `ScrollMonitorService`:

```
AccessibilityEvent.TYPE_VIEW_CLICKED -> ScrollTracker.onTapEvent(packageName, event.eventTime)
```

The existing tracked-app guard applies unchanged. No window-content retrieval; optionally filter by `event.className` later if measurements show IME/keyboard noise (do not count taps while an `EditText` has focus if noise is real — verify before building it in).

### 4.3 Data model

`data/ScrollAnalyticsRepository.kt` — extend the existing per-day/per-app records with defaults (backward compatible):

```
ScrollEvent     + taps: Int = 0, passiveScrolls: Int = 0
AppScrollEvent  + taps: Int = 0, passiveScrolls: Int = 0
```

New repository calls (next to `recordScroll` / `recordInterruption`):

```
recordTap(packageName, timestamp)          // increments taps (daily + app-day)
recordSessionEnd(packageName, scrolls, hadInteraction, timestamp)
   // if !hadInteraction: adds scrolls to passiveScrolls (daily + app-day)
   // engagedScrolls is derived: scrolls - passiveScrolls (never stored)
```

`DashboardRepository` derives `todayPassiveScrolls`, `passiveShare = passive/todayScrolls`, and per-app `TopApp.passiveScrolls` from the app store.

**Why a session-flag, not a tap count, is the right primitive:** Android coalesces/throttles accessibility events (the service already runs `notificationTimeout="100"`). A precise tap *count* will be wrong. But we only need "did the user interact at least once this session" — a boolean is robust to coalescing. This is the strongest technical argument for the session-flag design over "count all the taps."

### 4.4 Tracking changes in `ScrollTracker` (session lifecycle — the real scope)

Today sessions end only on service connect/unbind or tracked-app switch, which makes attribution unreliable. To say "these scrolls had no interaction," the session must be flushed at its real end. Scope of work:

- Add `sessionHadTap: Boolean` to session state; `onTapEvent` sets it.
- **Flush on untracked window change:** in `ScrollMonitorService`, before the tracked-app guard, if a `TYPE_WINDOW_STATE_CHANGED` arrives for a different package and a session is open, call `onSessionEnd(...)` first. This fixes the today-existing gap where going to the home screen never ends a session.
- **Flush on idle:** a service-side coroutine (e.g. every 60 s, flush a session with no scrolls for N minutes) — note `ScrollTracker.onIdleCheck` currently runs only from MainActivity's UI loop (`MainActivity.kt:176`, 1 s loop), so it is *not* a background timer; the service needs its own.
- **Flush on `onUnbind`** (already routes through `resetToIdle`).
- **Midnight rollover:** `recordSessionEnd` stamps the flush timestamp, so a session straddling midnight attributes its passive scrolls to the flush date — acceptable, document it.

### 4.5 UI (Premium-only)

- **ProgressTab:** a "Passive scrolling" card gated `isPremium` like the other Premium cards: "X% of today's scrolls were passive" with a small passive-vs-engaged split bar, plus a per-app breakdown in the Tracked Apps dialog (e.g. "Instagram · 62% passive"). Copy stays non-judgmental ("Scrolling without a tap is autopilot — aware scrolling is a choice"). Never "bad"/"good".
- **No changes** to TodayTab, Today's Goal, reminders, or onboarding.

### 4.6 Free/Premium gating

Premium-only surfacing, consistent with `ProgressTab.kt:41-43`. Free tier: taps/passive recorded in background, not displayed. No paywall pressure beyond the existing Premium banner.

### 4.7 Migration of retained data

`decode`/`decodeApp` in `LocalScrollAnalyticsRepository` read with `optInt("taps", 0)` / `optInt("passive", 0)`; `encode` writes them. Existing 365-day JSON decodes to 0 — no data loss, no re-keying, retention unchanged. New fields ride the same `scroll_analytics` SharedPreferences.

---

## 5. Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| **Event coalescing/throttling** undercounts taps | Use the session-level "interacted yes/no" flag, not precise tap counts; coalescing cannot erase a session that had a tap. Never surface raw tap counts. |
| **Click-event noise** (media controls, IME, ad surfaces, synthetic clicks) | The flag is robust to spurious taps (any tap = interacted). Optionally drop taps while an `EditText` is focused, measured before building it in. |
| **Battery/CPU** from the highest-frequency event type | Keep `notificationTimeout=100` (already batching); guard to tracked apps only; flag update is O(1). Measure on a device; if hot, gate event registration behind the Premium setting that unlocks the card. |
| **False signal: taps ≠ useful** (tap-through Shorts/Reels) | Never present taps as a quality judgement; the metric is "interacted vs not", labelled as such; copy explicitly avoids implying taps are good. |
| **Vocabulary drift** ("clicks", "good/bad scrolls") | Glossary additions below; copy review against CONTEXT.md. |
| **Play disclosure mismatch** | Adding an observed event type must be reflected in the in-app disclosure (`docs/wayfinding/0011`) and the store description. Do this only as part of a submission touch, or post-launch to avoid blocking launch. |
| **Session-boundary fuzziness** (abandoned sessions, home-screen exit) | Flush on untracked window change + service idle timer + onUnbind (4.4). This is the bulk of the engineering cost; it also fixes a latent gap in today's session model. |
| **Rationalisation risk** ("I tapped, so I'm fine") | The feature never changes reminders/goals; the message is "you scrolled passively," not "you scrolled well." |

---

## 6. Bottom line

- **Do not build "useful vs bad scrolling".** It labels as bad the very thing the app exists to detect, would miscall the worst tap-driven doomscrolling as fine, drags in the "clicks" vocabulary, widens the accessibility footprint, and gives users a rationalisation.
- **Optional, narrower version — "passive scrolling":** Premium, descriptive-only, session-flag based, surfaced as an informational split in ProgressTab. Worth building only after launch, and only if the insight clears the disclosure/privacy bar.
- Either way, do not let taps influence reminders, the **today's goal**, or the streak. The app's core value — a single, opinionated scroll count with respectful nudges — stays intact.

## Glossary additions proposed for CONTEXT.md

- **Passive scrolling**: The share of a day's scrolls (or a tracked app's scrolls) that happened in scrolling sessions with no interaction — no taps on tappable views. A **Premium** insight, descriptive only; it never affects reminders, the **today's goal**, or the streak.
  *Avoid*: bad scrolling, mindless scrolling, zombie scrolling, useless scrolling.
- **Engaged scrolling**: The complement of **passive scrolling** — scrolls in sessions where the user tapped a tappable view at least once. Not a quality judgement.
  *Avoid*: good scrolling, useful scrolling, interactive scrolling.
- **Taps**: User activations of tappable views in a **tracked app** (Android `TYPE_VIEW_CLICKED`). A counting unit, never a synonym for scrolls.
  *Avoid*: clicks.
- **Scrolling session**: A continuous period in a **tracked app** from first scroll or app open until the app leaves the foreground. Currently an in-memory-only concept in the service; "passive scrolling" requires it to be flushed properly.
  *Avoid*: browsing session, session length.

### Relationships to add

- **Passive scrolling** is a **Premium** insight derived from **taps** and **scrolls**; it never changes **reminder limits**, **today's goal**, or the streak.
