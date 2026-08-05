# ScrollStop — Play Console Launch Kit

A single reference for everything left in Play Console. Pairs with:
- `store_listing.md` — title, short & full description, asset checklist
- `accessibility_declaration.md` — the accessibility form
- `PRIVACY_POLICY.md` — host this somewhere public (e.g. GitHub Pages) and paste the URL

---

## 0. Before you start (done)

- ApplicationId renamed to `com.scrollstop.app` ✅ (build passes)
- Product IDs in code: `scrollstop_monthly`, `scrollstop_yearly` (subscriptions)

## 1. Developer account + create app

1. Go to https://console.play.google.com → **Create app** (pay the one-time $25 if you haven't).
2. App name: `ScrollStop`. Default language, app or game: **App**.
3. If asked for a **free or paid** app: Free (you monetise via the in-app subscription).
4. Set up your **Developer profile** (contact email, address) — required before you can publish.

## 2. Store listing

Paste from `store_listing.md`. Upload: app icon (512×512), feature graphic (1024×500), and 2–8 screenshots of the real app.

## 3. Content rating questionnaire — answers

Play Console → **App content → Content rating**. Answer honestly; for this app that means:

- Target audience: **All ages** (or Teens — your call; content is clean either way).
- Alcohol, tobacco, drugs: **No / None**
- Profanity or crude humor: **No**
- Sexual content or nudity: **No**
- Violence, blood, gore: **No**
- Weapons: **No**
- Gambling: **No** (no real-money or simulated gambling)
- User interaction / chat / social features: **No** (no P2P, no user-generated content, no in-app social)
- Location sharing: **No**
- Illegal or dangerous activity: **No**

Result will be roughly "Everyone / Everyone 10+". Note: the Drill Sergeant and Action Hero reminder tones are just voice copy — they are not violent content, so they don't change the rating.

## 4. Data safety form — answers

Play Console → **App content → Data safety**.

Main question: **"Does your app collect or share any of the required user data types?"**
→ **No.**

Why this is accurate:
- Nothing leaves the device. No account, no sign-up, no servers.
- No analytics SDKs and no crash-reporting SDKs (verified in `app/build.gradle.kts` — only Compose, DataStore, and Google Billing).
- Scroll counts and settings are stored only on-device; Play's data safety form does not require disclosing data that never leaves the device.
- Purchase/subscription handling is done entirely by **Google Play** (Google's responsibility, covered by Google's policy), not the app.

Follow-on questions (because you answered "No"):
- "Is all user data encrypted in transit?" → **Not applicable** (no data transmitted).
- "Do you provide a way for users to request deletion of their data?" → **Not applicable**.
- "Is this app directed at children under 13 / does it target the Play Families policy?" → **No**.

Permissions you must declare on the **App content → Permissions** / **Data safety** pages:
- **Notifications** (`POST_NOTIFICATIONS`) — used for scroll reminders.
- **Vibrate** — accompanies reminders.
- **Full-screen intent** (`USE_FULL_SCREEN_INTENT`) — optional full-screen reminder style where the platform allows it.
- **Accessibility service** — scroll detection (covered in the Accessibility declaration).

## 5. Accessibility declaration

Play Console → **App content → Accessibility**. Paste from `accessibility_declaration.md`.

## 6. Subscriptions

Play Console → **Monetise → Products → Subscriptions**.

Create **ScrollStop Monthly**:
- Product ID: `scrollstop_monthly`  ← must match code exactly
- Name: "ScrollStop Monthly"
- Add a **base plan**: auto-renewing, billing period **1 month**, price **$1.99** (add local-market prices as you like; Google converts).
- Enable a **free trial: 7 days** on the base plan.

Create **ScrollStop Yearly**:
- Product ID: `scrollstop_yearly`  ← must match code exactly
- Name: "ScrollStop Yearly"
- Add a **base plan**: auto-renewing, billing period **1 year**, price **$14.99**.
- Enable a **free trial: 7 days** on the base plan.

The code already picks the trial offer automatically (`BillingRepository.launchPurchase` with `preferTrial = true`), and the paywall shows "1 week free", the struck-through monthly-equivalent, and the dynamic Save-37% badge.

## 7. License testing + release signing

**Release signing (prerequisite for uploading):**
- In Android Studio: **Build → Generate Signed Bundle/APK** → create/choose a keystore → generate the `.aab`.
- Or I can wire a `signingConfig` + a `keystore.properties` (gitignored) into the `release` build type so `./gradlew bundleRelease` produces a signed AAB. Say the word.

**License testers:**
1. Play Console → **Setup → License testing** → add your Google account email(s).
2. Upload the signed AAB to **Internal testing** and add yourself as a tester.
3. Install the internal-testing build on a device signed into that account. Purchases by license testers are **not charged**.
4. Test end-to-end: subscribe monthly → premium unlocks; toggle the free-trial flow; uninstall/reinstall or tap **Restore purchases** → premium returns; cancel the subscription.

## 8. Publish

1. **Internal testing** → verify enable flow, reminders, widget, billing on a release build.
2. **Closed testing** → invite a handful of real users.
3. **Production** → release.

---

**Reminders before you go live:**
- Bump `versionName` when you cut the real release (currently `1.0`).
- Keep the keystore + `keystore.properties` out of version control.
- The `USE_FULL_SCREEN_INTENT` permission may prompt a review question — the accessibility declaration + data safety answers already set up the justification (full-screen is opt-in and degrades to heads-up).
