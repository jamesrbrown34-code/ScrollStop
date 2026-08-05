# ScrollStop Roadmap

How to read this: phases are in **build order** — later phases assume earlier ones. Each item is tagged **Free** or **Premium** and given a priority (P1 = high, P3 = low). "Why" explains the value; "Notes" point at the code that needs to change.

## Phase 1 — Tracking foundation

**1.1 Free · P1 — App selection (curated library)** ✅ implemented
- *Why:* The app used to track 6 hardcoded apps (`AppFilter.kt`). If your app isn't in the list, the app is useless to you. This is the single biggest core-usefulness gap.
- *Notes:* `TrackedAppsRepository` + `CuratedLibrary` (14 apps) replace the hardcoded set; a "Tracked Apps" card with an edit dialog lets users pick which apps are watched. Defaults to the original 6. Grounds everything later (per-app limits, app-level insights).

**1.2 Internal · P0 — Per-app analytics** ✅ implemented
- *Why:* Today analytics are aggregated per-day across all apps (`ScrollEvent` has no package dimension). App-level insights (Free) and per-app limits/tones (Premium) both need per-app data. Building this once serves both.
- *Notes:* `ScrollAnalyticsRepository` now records `AppScrollEvent(date, package, scrolls, interruptions)` alongside the daily store in the same persistence file. Nothing surfaces it in the UI yet — it's ready for 2.5 and 3.1.

## Phase 2 — Free core usefulness

**2.1 Free · P1 — Quiet hours** ✅ implemented
- *Why:* Reminders at 2am (or in meetings) feel hostile. A "don't remind me during these hours" window makes the app respectful rather than nagging.
- *Notes:* `QuietHoursRepository` (one daily window, off by default, midnight-wrapping) gates `InterventionManager.trigger` in `ScrollTracker`; scrolling still counts. Configured via a "Quiet Hours" card + edit dialog with a switch and start/end time pickers.

**2.2 Free · P2 — Pause tracking** ✅ implemented
- *Why:* Sometimes you genuinely need to scroll (work, travel). A quick pause from the notification makes tracking feel like it's on the user's terms.
- *Notes:* Reminder notifications carry "Pause 30 min" / "Pause 1 hr" actions → `PauseReceiver` → `PauseRepository` (persisted "paused until"). While paused, `ScrollTracker` skips counting and reminders. Dashboard shows a "Tracking paused until HH:mm" indicator on the Tracked Apps card.

**2.3 Free · P2 — Weekly report** ✅ implemented
- *Why:* The dashboard already has weekly data (streak, best day, improvement). An end-of-week summary card makes the analytics feel rewarding and keeps the app part of the user's routine.
- *Notes:* Replaces the old Insights card. "This Week" card: total scrolls, current streak, best day, improvement vs last week, reminders given this week. All derived from existing `DashboardRepository` data (plus a new `interruptionsThisWeek` field).

**2.4 Free · P2 — Today's goal** ✅ implemented
- *Why:* A "stay under N scrolls today" target with on-track progress gives the day a frame and gives users a reason to open the app mid-day.
- *Notes:* `TodayGoalRepository` (default 500, separate from the reminder limit). "Today's Goal" card shows "312 / 500" with a progress bar and inline preset chips + custom field.

**2.5 Free · P3 — App-level insights** ✅ implemented
- *Why:* "Which app ate my day?" is the most compelling insight the app can show. Depends on 1.2.
- *Notes:* `DashboardRepository` now aggregates `appEvents` into `topAppsToday`; a "Top Apps Today" card shows the top 3 apps with scroll counts.

## Phase 3 — Premium: per-app controls

**3.1 Premium · P1 — Per-app reminder limits** ✅ implemented
- *Why:* The flagship premium feature. Users want to be strict with Instagram but lenient with YouTube.
- *Design:* Global reminder limit stays as the default; per-app limits are **overrides**. Apps without an override use the global value. Additive, degrades gracefully.
- *Notes:* Persisted as a JSON map in `reminder_settings` DataStore (`ReminderSettings.perAppLimits`). Premium users route triggers to per-app daily counts (from `appEvents`); free users keep the total-daily cadence. Overrides set via the Tracked Apps dialog (tap a per-app limit row → picker dialog).

**3.2 Premium · P2 — Per-app tones** ✅ implemented
- *Why:* Natural extension once per-app settings exist; near-free to add.
- *Notes:* `ReminderSettings.perAppTones` JSON map in the same DataStore. Per-app tone selection in the Tracked Apps dialog (tap a per-app tone row → picker dialog with RadioButton tone list).

## Phase 4 — Premium expansion

**4.1 Premium · P2 — Track-any-app** ✅ implemented
- *Why:* The curated free library covers popular apps; premium unlocks arbitrary installed apps for the long tail.
- *Notes:* `getInstalledApps(context)` via `PackageManager` + `<queries>` in manifest. "+ Add any app" button (premium-gated) in the Tracked Apps dialog opens an installed-apps picker.

**4.2 Premium · P2 — Deep analytics** ✅ implemented (day-level)
- *Why:* Power-user analytics are a classic premium upsell and ride on the per-app analytics infrastructure.
- *Notes:* Retention extended from 30 to 60 days. "Daily Top App" card (premium-gated) shows which app dominated each of the last 7 days. (Hourly heatmap + CSV export deferred.)

**4.3 Premium · P3 — Home-screen widget** ✅ implemented
- *Why:* A glanceable widget keeps the app present in daily life without opening it.
- *Notes:* 2×1 `ScrollStopWidget` showing today's scrolls, reading from the analytics SharedPreferences. Tapping opens the app.

**4.4 Premium · P3 — Streak freeze** ✅ implemented
- *Why:* One bad day wiping a long streak feels punishing; a freeze protects the habit.
- *Notes:* `DashboardRepository.isPremium` mutable flag; premium users get one auto-freeze — a missed day is skipped in the streak count. Zero-config.

## Phase 5 — Retention & conversion (later)

- **Free · P3 — Stronger streaks** — milestones ("7-day streak!") with light celebration copy, to make the dashboard a daily habit.
- **Free/Premium · P3 — Premium previews** — tasteful "next reminder at 200 instead of 100" teasers in the reminder card, once retention exists. Do **not** ship conversion pressure before Phases 1–4 land; the app must be worth keeping first.

## Non-goals (for now)

- Ads / monetisation beyond the subscription.
- Cloud sync/backup across devices.
- Social/sharing features.
- iOS or web.
