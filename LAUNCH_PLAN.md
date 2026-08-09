# ScrollBeat — Launch & Growth Plan

Goal: get ScrollBeat live on Google Play, in front of the right users, and set up to iterate. This plan is the single runbook from "working app in a repo" to "published, marketed, measurable app".

**Where we are now (audit, Aug 2026):**
- ✅ All roadmap phases 1–4 shipped in code (`plan.md`): tracking, quiet hours, pause tracking, weekly report, today's goal, app insights, per-app limits/tones, track-any-app, deep analytics, widget, streak freeze.
- ✅ Release docs drafted: `play_console_guide.md`, `store_listing.md`, `accessibility_declaration.md`, `PRIVACY_POLICY.md`.
- ✅ Release signing wired (`keystore.properties` present, gitignored). A signed `app-release.aab` (~9.3 MB) exists in `app/build/outputs/bundle/release/`.
- ✅ Billing product IDs in code: `scrollstop_monthly`, `scrollstop_yearly`.
- ⚠️ **Uncommitted work in flight**: a new "reminder cooldown" feature (`ReminderCooldownRepository.kt` untracked + edits to `ScrollTracker.kt`, `MainActivity.kt`, `SettingsTab.kt`). This must be finished, reviewed, and committed before shipping.
- ❌ No store assets yet: no screenshots, feature graphic, icon exports (no PNGs in repo).
- ❌ No README, no landing page, no marketing infra.
- ❓ Unknowns to confirm: Play developer account status, targetSdk compliance as of 2026, ad budget, positioning.

---

## Phase A — Ship-readiness (code + release hygiene) — 2–4 days

**A1. Finish the reminder cooldown feature (in progress).**
- Review `ReminderCooldownRepository.kt` (free setting, default 10 min, DataStore-backed — looks consistent with `QuietHoursRepository`/`PauseRepository` patterns).
- Decide the product framing: cooldown is currently a **Free** global setting. Confirm that's intended (it limits reminder frequency, good default for free users). If it should be Premium, gate it in `SettingsTab.kt` like the tone/theme cards.
- Add it to `plan.md` (Phase 2 or 3) and to the store listing copy ("Reminder cooldown" bullet) if it ships.
- Commit on the `prem` branch, or fold into a release branch (see A4).

**A2. Version + build hygiene.**
- Bump `versionCode` (currently 1 → leave 1 for first release, but make sure it **only ever increments** from here) and set `versionName` to `1.0` (fine as-is).
- `minifyEnabled = false` in release: acceptable for v1, but verify the AAB isn't bloated and enable R8 shrinking in a follow-up release to cut size.
- Verify `targetSdk`/`compileSdk = 35` still meets Play's requirement as of Aug 2026 (Play requires new apps/updates to target a recent API). **Likely needs a bump to 36** — check Play Console's requirement and Android Studio's target-SDK warning, then bump `compileSdk`, `targetSdk`, and Compose/BOM if needed. This is the highest-risk compliance item; do it before uploading.
- Confirm the AAB is 64-bit (default true) — no action unless Play flags it.

**A3. Rebuild the signed release AAB** after committing all work:
```
./gradlew bundleRelease
```
Re-verify `app/build/outputs/bundle/release/app-release.aab` timestamp updates. (The existing AAB predates the cooldown feature — do not upload it as-is.)

**A4. Branch/commit strategy.**
- The `prem` branch is ahead of `main`. Decide: either merge `prem` → `main` and release from `main`, or release from `prem`. Recommend: **fast-forward `main` to `prem`** after review so release == main and the `codex/*` feature branches are safely history.
- Add a `README.md` to the repo root (what it is, screenshots placeholder, build instructions, privacy-policy link). It doubles as repo-presentation for anyone finding the GitHub link from marketing.

**A5. In-app polish before screenshots.**
- Add an app version display in Settings (small text) — cheap and helpful for support tickets.
- Add a support/contact entry in Settings pointing at your email (currently `PRIVACY_POLICY.md` has `[your email]` placeholder — fill it).
- Smoke-test the debug premium override toggle still works in debug (it must be ignored in release — confirmed by `BuildConfig.DEBUG` gate in `PremiumManager.kt:38`).

**Exit criteria for Phase A:** clean `git status`, signed AAB built from the exact commit you'll upload, version set, compliance (targetSdk) verified.

---

## Phase B — Play Console & store presence — 2–3 days

**B1. Developer account.** Pay the one-time **$25** registration if not done. Complete the Developer Profile (name, email, address) — required before publishing.

**B2. Create the app.** Free app, name `ScrollBeat`, default language English. Category suggestion: **Health & Fitness** (sub: Self Management) or **Productivity** — test both in search during ASO later.

**B3. Store listing assets** (the current blocker — nothing exists yet):
- **App icon 512×512** — reuse the adaptive icon foreground/background (the "S" letterform logo). Export as both adaptive and legacy icon.
- **Feature graphic 1024×500** — dark, minimalist to match the app: tagline + mockup of the Today screen. Can be generated from a Figma/Canva template.
- **Screenshots 2–8 (recommend 6–8)** — capture from a real device/emulator with a dark theme:
  1. Onboarding → enabling accessibility (the "walk me through it" screen).
  2. Today tab (scrolls, today's goal progress, top apps).
  3. A reminder notification (Uber-style popup) on the lock screen.
  4. Progress tab (weekly report, streak).
  5. Settings (quiet hours, pause, cooldown).
  6. Premium paywall (7-day trial, save 37% badge).
  7. Widget on the home screen (great visual hook).
  8. Per-app limits screen (premium).
- Each screenshot's top third must be reserved for Play's UI overlay (Google crops it) — keep key content below the fold.
- Paste the copy from `store_listing.md` (title / short / full description) into the listing.

**B4. Content rating questionnaire.** Follow `play_console_guide.md` §3: **All ages / Everyone**. Drill Sergeant & Action Hero tones are voice copy only — no rating impact.

**B5. Data safety form.** Follow §4: **"No data collected/shared"** — accurate because nothing leaves the device. Declare the permissions: Notifications, Vibrate, Full-screen intent, Accessibility.

**B6. Accessibility declaration.** Paste from `accessibility_declaration.md` (§5 of the guide). Be explicit about `canRetrieveWindowContent="false"` — this is what gets Play to approve accessibility services quickly.

**B7. Subscriptions.** Create the two products exactly matching the IDs in code:
- `scrollstop_monthly` — $1.99 / 1 month, **7-day free trial**.
- `scrollstop_yearly` — $14.99 / 1 year, **7-day free trial**.
- Add base plans as in `play_console_guide.md` §6. The code picks the trial automatically (`BillingRepository.launchPurchase` with `preferTrial`).

**B8. Privacy policy.** Fill in the email, date, then **host it publicly** (GitHub Pages on this repo is the cheapest — enable Pages, serve `PRIVACY_POLICY.md`, or a simple `docs/index.md`). Paste the URL in the listing. Also put the same URL in-app (Settings → Privacy).

**Exit criteria for Phase B:** app created, all assets uploaded, all forms submitted, subscriptions configured, privacy URL live.

---

## Phase C — Test, then release to production — ~3 weeks (mostly waiting)

**C1. Internal testing.** Upload the signed AAB → Internal testing track. Add yourself to License testing (purchases aren't charged). Verify end-to-end on a real device:
- [ ] Onboarding + accessibility enable flow works
- [ ] Scroll counting across a tracked app
- [ ] Reminder fires at limit, cooldown respected
- [ ] Quiet hours silence reminders (but still count)
- [ ] Pause tracking from notification stops counting
- [ ] Widget shows today's scrolls
- [ ] Subscribe (trial) → premium unlocks (limits/tones/theme/per-app/freeze)
- [ ] Restore purchase after reinstall
- [ ] Cancel subscription → premium locks, free features still work
- [ ] Debug premium override is OFF in release build (or absent)

**C2. Closed testing — the long pole.** Google requires new **personal** developer accounts to run a closed test with at least **20 opted-in testers for 14 continuous days** before production access (the exact number is shown in Play Console and has changed over time — check it; some accounts see 12). Plan for this NOW because it's pure waiting:
- Recruit 20+ real humans (friends, family, the communities in Phase E — a "join the beta" invite).
- Set the **closed testing track** as the promoted release and enroll testers.
- Do NOT try to shortcut with fake accounts — Play rejects and can ban.
- Use this window to collect feedback and squash bugs.

**C3. Production rollout.** After closed-test approval: staged rollout **1% → 10% → 25% → 100%** over ~1 week. Keep the keystore + `keystore.properties` safe and backed up (losing it = can't update the app ever).

**Exit criteria for Phase C:** production release live at 100%, no crash reports (see F1), first organic installs flowing.

---

## Phase D — Branding & launch asset kit — build alongside Phase C (parallel track)

**D1. Positioning — decide this once, write it everywhere.** Two candidate framings:
- **"Stop doomscrolling"** (habit-breaker, wellbeing) — matches current copy; strong with r/nosurf, digital minimalism.
- **"Take back your day"** (productivity, time-awareness) — wider audience.
Recommend keeping the current "notice & gently stop" language and using *both* framings across different channels (never mix within one channel).

**D2. Landing page (needed for Phase E).** GitHub Pages site on this repo (free, no infra):
- Hero: 6–15s looping clip or animated mockup of the reminder.
- Value props: counts scrolls in the background, never reads your screen, everything on-device, 7-day free trial.
- Two CTAs: "Get it on Google Play" + "Join the beta".
- Sections: How it works / Privacy / Pricing / FAQ.
- Footer: privacy policy, contact, social links.

**D3. Press kit (one folder, one link).** App icon, feature graphic, all screenshots, the one-pager (what/why/how/privacy/price), founder contact. This makes every review site / podcast / influencer pitch 10× easier.

**D4. Short promo video.** 6–15s, vertical (9:16) for TikTok/Reels/Shorts + one square for X: someone scrolling → reminder pops → puts phone down. This single asset powers most of Phase E content.

**Exit criteria for Phase D:** landing page live, press kit assembled, one promo video + 4–6 screenshot assets.

---

## Phase E — Launch & growth channels (the "advertised" part)

Ranked by expected ROI for a solo, bootstrapped indie app. Do 1–3 first; 4–6 when you have budget/time.

**E1. Owned community launches (free, highest ROI).** Launch-day blitz in the exact communities that want this:
- **Product Hunt** — launch page with video, screenshots, a "maker" comment thread, respond to every comment same-day. Good for press + first 500 downloads.
- **Hacker News (Show HN)** — one honest post, title that states the problem ("Show HN: ScrollBeat – an Android app that gently stops your doomscrolling"), answer every comment. Requires your repo/landing page to be tidy (hence A4/D2).
- **Reddit** — r/Android, r/digitalminimalism, r/nosurf, r/nosurf, r/productivity, r/apps, r/selfimprovement, r/ADHD. **Never post the same thread twice**; tailor each; be transparent ("I built this, it's on-device, has a free tier, 7-day trial"). r/nosurf and r/digitalminimalism are your best audiences — this is a product they actively seek.
- **Indie Hackers** — build-in-public log: weekly posts of screenshots + numbers. Builds a small engaged following.

**E2. Short-form video (organic).** This app is *made* for the "I fixed my screen time" genre:
- TikTok / YouTube Shorts / Instagram Reels: 3–5 clips a week mixing (a) your own screen-time story, (b) before/after, (c) the "200 scrolls" reminder popup reaction, (d) "3 apps that help you stop scrolling" listicles including ScrollBeat.
- Post the promo video (D4) week one and iterate on what lands. This is the single biggest organic distribution channel for habit apps right now.

**E3. Review sites & newsletters (one email each, free).**
- Android blogs: Android Police, 9to5Google, XDA, AndroidAuthority, AndroidGuys — pitch via their "news tips" email with the press kit + a promo code for a free month.
- Newsletters/app roundups: Mobile App Freelancer, Indie App Weekly, Side Project Newsletter, the Aurore newsletter.
- Small-youtube reviewers in the digital wellbeing niche (search "stop doomscrolling app") — DM with a promo code + press kit. Micro-influencers (10k–100k) convert best for this category.

**E4. Paid ads (only after Phase C is live and conversion works — don't pay to send users to a broken funnel).**
- Budget framing: assume ~5% free→paid trial conversion, $1.50–$2.50 per install → effective CAC ~$30–50 per subscriber. At $1.99/mo you cannot profit on ads alone unless yearly ($14.99) dominates and retention is strong. **Decision point below.**
- Cheapest sensible test: **Reddit ads** ($20–50/day) targeting r/nosurf-style interests; then **Meta/Instagram** interest targeting (digital minimalism, meditation, productivity); Google UAC last.
- Only run ads once you know the LTV from F1.

**E5. Partnerships.** One-way digital wellbeing roundups, minimalism podcasts (guest slot), and digital-detox apps that complement rather than compete (e.g., app-blockers) — cross-promo or bundling ideas.

**E6. ASO (App Store Optimization) — ongoing.**
- Title/subtitle: keep `ScrollBeat`, but the short description is keyword-rich. Target keywords: doomscroll(ing), screen time, digital wellbeing, focus, habit, mindful, break, phone addiction.
- Localize the listing into 2–3 languages that match your real audience (Play's free auto-translate is a start; manual for top markets).
- A/B test the feature graphic and first screenshot via Play's experiments after a couple hundred installs.

---

## Phase F — Post-launch operations — ongoing

**F1. Measurement.** The app deliberately has **no analytics SDK** (data-safety story + privacy). That's a feature, but it means you're blind. Options:
- Keep it clean: measure only via Play Console (installs, retention, crash-free %, subscription revenue, conversion funnel). This is enough to run the business.
- If you want funnel detail, add a **privacy-friendly, consent-gated** analytics option later (opt-in, declared in Data Safety). Decision point below — don't do this pre-launch.

**F2. Feedback loops.**
- Reply to every Play review (esp. 3-star — those are the fixable ones).
- Set a support email alias (`support@...` or the Play Console contact) and a simple in-app "Send feedback" if not present.
- Track the top 3 complaints monthly → feed `plan.md`.

**F3. Iteration backlog** (from `plan.md` Phase 5, unshipped): stronger streaks with milestone copy, premium previews. Add from launch feedback. Ship 1 meaningful improvement per month.

**F4. Pricing experiments.** After 2–3 months of baseline: test a longer first-trial (14 days), a launch discount, or a lifetime "supporter" IAP. Watch churn, not just signups.

**F5. Cadence.** Monthly release notes + an update that's visibly better every 4–6 weeks keeps Play's "recently updated" signal and your E2 content fresh.

---

## Decision points (you own these; plan is written to work with either answer)

1. **Ad budget for launch:** $0 (organic only) vs $200–$1000 test budget. If $0, Phase E = E1–E3 + E6 only.
2. **Analytics SDK:** keep zero-SDK (recommended for v1) vs opt-in analytics later.
3. **Reminder cooldown: Free or Premium?** Current code = Free. If Premium, that's a small gate change before the AAB is cut.
4. **Target market languages:** English-only vs localized launch.
5. **Release branch:** `main` fast-forwarded to `prem` vs release from `prem`.

---

## Master timeline (targets)

| Week | Phase | Key deliverable |
|---|---|---|
| 1 | A | Cooldown merged, version set, targetSdk verified, fresh signed AAB |
| 1 | B | Play account + app + all assets + forms + subscriptions |
| 1–2 | D | Landing page, press kit, promo video |
| 2 | C1 | Internal testing pass, bugs fixed |
| 2–5 | C2 | **Closed testing: recruit 20+ testers, 14-day window** |
| 3–5 | E1/E2 | Community launch prep + beta recruitment overlaps closed testing |
| 5–6 | C3 | Production rollout (staged) |
| 5–6 | E1 | Launch day: PH + HN + Reddit + video blitz |
| 7+ | E3–E6, F | Press pitches, ads (if budget), ASO, monthly iteration |

**Biggest risk:** the 14-day closed-testing requirement (C2) — start recruiting testers in week 1, not week 4. **Second biggest:** targetSdk compliance (A2) — check it before you build assets around a listing that might be rejected.
