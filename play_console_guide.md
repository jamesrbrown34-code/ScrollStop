# ScrollBeat — Play Console Launch Kit

A single reference for everything left in Play Console. Pairs with:
- `store_listing.md` — title, short & full description, asset checklist
- `accessibility_declaration.md` — the accessibility form
- `PRIVACY_POLICY.md` — host this somewhere public (e.g. GitHub Pages) and paste the URL

---

## 0. Before you start (done)

- ApplicationId set to `com.scrollstop.android` ✅ (build passes)
- Product IDs in code: `scrollstop_monthly`, `scrollstop_yearly` (subscriptions)
- `compileSdk`/`targetSdk` bumped to **36** (Play requires it from Aug 31 2026) ✅
- Release signing wired (`keystore.properties`, gitignored) — `./gradlew bundleRelease` works ✅
- Signed release AAB built and verified: `app/build/outputs/bundle/release/app-release.aab` ✅
- Release-candidate work committed on `prem` (working tree clean) ✅

## 1. Developer account + merchant setup + service fee

1. Go to https://console.play.google.com → **Create app** (pay the one-time **$25** if you haven't).
2. Complete the **Developer profile** (contact email, address) — required before publishing.
3. **Get paid — merchant account (prerequisite for subscriptions).** Set up a **Google Payments merchant account** / payments profile (Play Console → **Monetise → Monetisation setup**, or payments.google.com). Provide legal name, banking details for payouts, and tax info. Review can take up to ~2 weeks — start it early.
4. **Enroll for the 15% service fee** (Play Console service-fee section): create an **account group**, declare associated developer accounts (only yours — for a solo account that's "none / this one"), and accept the T&Cs. Free; locks in 15% on your first $1M/year of revenue.

## 2. Create the app

1. Play Console → **Create app**.
2. App name: `ScrollBeat`. Default language: English. App or game: **App**.
3. Free vs paid: **Free** (you monetise via the in-app subscription).
4. Category suggestion: **Health & Fitness** (sub: Self Management) or **Productivity**.

## 3. Store listing

Paste from `store_listing.md` (already refreshed with reduction plans, cooldown, monthly breakdown, daily summary). Upload:
- **App icon** 512×512 (reuse the adaptive icon foreground/background).
- **Feature graphic** 1024×500.
- **2–8 screenshots** of the real app (recommend 6–8; none exist yet — capture on a device/emulator).
- **Privacy policy URL** — host `PRIVACY_POLICY.md` publicly (e.g. GitHub Pages) and paste the link.

## 4. Content rating questionnaire — answers

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

## 5. Data safety form — answers

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

## 6. Accessibility declaration

Play Console → **App content → Accessibility**. Paste from `accessibility_declaration.md`. Emphasise `canRetrieveWindowContent="false"` — the service counts scroll events only and never reads screen content.

## 7. Subscriptions

Play Console → **Monetise → Products → Subscriptions** (requires the merchant account from step 1).

Create **ScrollBeat Monthly**:
- Product ID: `scrollstop_monthly`  ← must match code exactly
- Name: "ScrollBeat Monthly"
- Add a **base plan**: auto-renewing, billing period **1 month**, price **$1.99** (add local-market prices as you like; Google converts).
- Enable a **free trial: 7 days** on the base plan.

Create **ScrollBeat Yearly**:
- Product ID: `scrollstop_yearly`  ← must match code exactly
- Name: "ScrollBeat Yearly"
- Add a **base plan**: auto-renewing, billing period **1 year**, price **$14.99**.
- Enable a **free trial: 7 days** on the base plan.

The code already picks the trial offer automatically (`BillingRepository.launchPurchase` with `preferTrial = true`), and the paywall shows "1 week free", the struck-through monthly-equivalent, and the dynamic Save-37% badge.

## 8. License testing + internal testing

1. Play Console → **Setup → License testing** → add your Google account email(s).
2. Upload the signed AAB to the **Internal testing** track and add yourself as a tester.
3. Install the internal-testing build on a device signed into that account. Purchases by license testers are **not charged**.
4. Test end-to-end against the checklist in `docs/wayfinding/0009-release-qa.md`: enable flow, scroll counting, reminder at limit, cooldown, quiet hours, pause, widget, subscribe (trial) → premium unlocks, Restore after reinstall, cancel → premium locks. Confirm the debug premium override is absent in the release build.

## 9. Closed testing — the long pole

Because this is a **personal** developer account, Play requires a closed test with **12+ opted-in testers for 14 consecutive days** before production access (the exact count is shown in Console — some accounts see 20). This is pure waiting, so recruit testers now:
- Friends/family, r/nosurf, r/digitalminimalism, r/Android — anywhere real users gather.
- Create the **Closed testing** track, upload the AAB, enroll testers, and keep the build updated during the window.
- Do **not** use fake accounts — Play rejects and can ban.

## 10. Publish to production

1. After closed-test approval: promote the release through to **Production**.
2. Staged rollout **1% → 10% → 25% → 100%** over ~1 week.
3. Keep the keystore + `keystore.properties` safe and backed up — losing it means you can never update the app.

---

**Reminders before you go live:**
- `versionCode = 1` / `versionName = "1.0"` is set; **only ever increment `versionCode`** from here on.
- Keep the keystore + `keystore.properties` out of version control.
- The `USE_FULL_SCREEN_INTENT` permission may prompt a review question — the accessibility declaration + data safety answers already set up the justification (full-screen is opt-in and degrades to heads-up).
- Missing pieces still on you: screenshots + feature graphic (none in repo), privacy-policy hosting + contact email, and merchant account setup.
