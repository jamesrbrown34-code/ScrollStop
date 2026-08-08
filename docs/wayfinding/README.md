# Wayfinder map — Launch ScrollStop on Google Play

## Destination

ScrollStop published to Google Play production: a signed release build in the Production track, with store listing, hosted privacy policy, data-safety form, content rating, accessibility declaration, and both subscriptions configured — passing Play review. "Done" = a new user can install the app from the public store listing.

## Notes

- Domain: Android accessibility-service doomscroll app. On-device only — no accounts, no servers (a standing non-goal).
- Terminology per CONTEXT.md (Premium, reminder limit, reminder tone, etc.). Use it in copy and decisions.
- Skills to consult: grilling, domain-modeling, research.
- Tracker: local-markdown. The map is this file; tickets are `docs/wayfinding/000N-*.md`; blocking uses the `blocked-by` frontmatter field (titles). A ticket is unblocked when every blocker is closed.
- Already settled before this map (do not re-litigate): applicationId = `com.scrollstop.android`; release signing wired and keystore gitignored (`bundleRelease` works); store listing copy, privacy policy draft, accessibility declaration draft, data-safety and content-rating answers drafted (see the repo-root `.md` files); pricing $1.99/mo + $14.99/yr with 1-week free trials, product IDs `scrollstop_monthly` / `scrollstop_yearly`; the Full-screen reminder style is best-effort and degrades to heads-up; the debug premium override is debug-only and ignored in release.

## Decisions so far

<!-- The index — one line per closed ticket: gist + link. Grows as tickets close. -->

- [Assess Play review risk: accessibility service and full-screen permission](0002-accessibility-fsi-review-risk.md) — Accessibility declaration is mandatory; scroll-counting is a permitted "App functionality" use but needs a prominent in-app disclosure + consent and listing documentation. `USE_FULL_SCREEN_INTENT` isn't auto-rejected but reviewers push non-alarm apps to drop it; "declare but never use" is disallowed.
- [Confirm the closed-testing requirement for this account](0006-closed-test-requirement.md) — New personal accounts (post Nov 2023) need 12+ testers opted in for 14 consecutive days before production; business accounts are exempt (need D-U-N-S + identity verification); new personal accounts also need device verification.

## Not yet specified

- Play's manual feedback on the accessibility declaration and disclosure video — only knowable after submission (0011).
- Whether the 1-week trial subscription offer draws extra review.

## Out of scope

- Post-launch iteration, ads, monetisation experiments — a fresh effort after launch.
- New Premium features (custom accent, Alternative apps, Break prompts) — a separate feature effort.
- iOS/web, accounts, cloud sync.
