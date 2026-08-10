---
title: Choose the full-screen reminder strategy
type: grilling
status: open
blocked-by:
  - 0002-accessibility-fsi-review-risk
---

## Question

Given the `USE_FULL_SCREEN_INTENT` review risk (facts from 0002), what should we ship:

- Keep the Full-screen reminder style + permission and defend it in the declaration?
- Keep the code but scope the permission out of the first release (degrade to heads-up) and re-add later?
- Something in between (e.g., keep the setting but remove the manifest permission so Play never sees it)?

Resolve with the user (grilling + domain-modeling). The answer decides what the submitted manifest contains.
