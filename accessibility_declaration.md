# ScrollStop — Accessibility Declaration (Play Console)

Use this in Play Console → **Policy → App content → Accessibility**. Adapt the wording to fit the exact form fields; the key points below are what Google's reviewer looks for.

## 1. Do you declare an accessibility service in your app's manifest?
Yes. The app declares one service, `com.scrollstop.app.service.ScrollMonitorService`, an `AccessibilityService` bound with `android.permission.BIND_ACCESSIBILITY_SERVICE`.

## 2. What does the accessibility service do? What is its purpose?

ScrollStop is an app that helps people notice and reduce mindless scrolling. The accessibility service exists for **one purpose only**: to count scroll events in the apps the user has chosen to track, so the app can show a gentle reminder when they have scrolled too much.

Specifically, the service listens for two event types:
- `TYPE_VIEW_SCROLLED` — to count scrolls.
- `TYPE_WINDOW_STATE_CHANGED` — to know which app is in the foreground.

That is the entire scope. The service does **not** read screen content, does not retrieve window content (`canRetrieveWindowContent="false"`), does not capture text, screenshots, or input, and does not interact with or control other apps.

## 3. Which parts of the app use the service, and why is it required?

- The service powers **scroll tracking and reminders** — the app's core function. Without it, the app cannot count scrolls or decide when to remind the user.
- Every other part of the app (dashboard, statistics, settings) works independently; the service only enables the tracking/reminder feature.
- The service is **off by default**. The user must explicitly enable it in their device's accessibility settings; ScrollStop walks them through this during onboarding.
- The user chooses **exactly which apps are tracked** from a curated list. The service reacts only to events from those apps.

## 4. Data handling

- No screen content is ever read: `canRetrieveWindowContent` is `false`.
- Scroll counts are stored **locally on the device only**. Nothing is collected, transmitted, or shared.
- Users can disable the service or remove the app's accessibility access at any time from device settings.

## 5. Who benefits?

- People who want to reduce doomscrolling and reclaim their time.
- The reminders are gentle, optional, and can be paused or disabled at any time (including per-session "pause tracking" and scheduled "quiet hours").

## 6. Transparency to users

- The service's on-device settings screen and ScrollStop's onboarding both describe exactly what the service does.
- The Play Store listing and the app's privacy policy state that the app does not collect or transmit any data.

---

**Supporting detail for your own reference (not needed in the form):**
- Service declaration: `app/src/main/AndroidManifest.xml` → `ScrollMonitorService` with `android.permission.BIND_ACCESSIBILITY_SERVICE`.
- Config: `app/src/main/res/xml/accessibility_service_config.xml` → `accessibilityEventTypes="typeViewScrolled|typeWindowStateChanged"`, `canRetrieveWindowContent="false"`.
