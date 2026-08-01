# Architecture

## System Overview

ScrollStop is an Android application that uses an AccessibilityService to observe scroll events in selected apps, maintains an in-memory active scrolling session, triggers interventions when behavior crosses awareness thresholds, and presents live progress in a Compose UI.

## Major Modules

* `ScrollMonitorService`: receives accessibility events and forwards relevant app changes and scroll events.
* `AppFilter`: decides which packages should be tracked.
* `ScrollTracker`: owns active session state, applies idle timeout boundaries, updates UI state, evaluates awareness risk, and records analytics.
* `DoomscrollAwareness`: pure threshold engine that selects the highest risk signal across session duration, scroll count, and scroll intensity.
* `ScrollIntensityAnalyzer`: pure scrolls-per-minute classifier used by awareness evaluation and the live dashboard.
* `InterventionManager`: sends notifications and vibration interventions, and stores temporary snooze state locally, and guards notification posting behind runtime notification permission checks.
* `InterventionActionReceiver`: receives notification actions such as intervention snooze.
* `DailyStatsStore`: local-first persistence for daily scroll progress.
* `MainActivity`: Compose dashboard for service setup, live session metrics, and daily summary.

## Data Flow

1. Accessibility events arrive in `ScrollMonitorService`.
2. Trackable app events are passed to `ScrollTracker`.
3. `ScrollTracker` enters a ready state on app changes, starts counted sessions on the first scroll event, resets sessions after inactivity, evaluates scroll intensity, and evaluates awareness with `DoomscrollAwareness`.
4. New awareness levels trigger `InterventionManager` unless interventions are temporarily snoozed.
5. Scroll/session deltas are persisted to `DailyStatsStore`.
6. Compose observes `ScrollTracker.uiState` and displays live metrics plus daily progress.

## Dependencies

* Android Accessibility APIs
* Android notification and vibration APIs
* Jetpack Compose Material 3
* Kotlin coroutines StateFlow
* SharedPreferences for local daily analytics

## Future Scaling Concerns

* Move from SharedPreferences to Room when retaining multi-day trend history.
* Introduce dependency injection before adding app-specific rule engines.
* Add test seams around time providers and persistence.
* Keep sensitive analytics local by default; require explicit opt-in for accountability or cloud features.
