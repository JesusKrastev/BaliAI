# Paywall Design Patterns

## Table of Contents
1. Single-Page Paywall Anatomy
2. Multi-Page Paywall Framework
3. Pricing Design Patterns
4. Feature-Based (Contextual) Paywalls
5. Exit & Abandoned Transaction Paywalls
6. Paywall Copy Library
7. Visual Design Rules

---

## 1. Single-Page Paywall Anatomy

The standard single-page paywall. Use this as your baseline control.

```
┌─────────────────────────────────────────┐
│  [Hero: App screenshot, lifestyle image, │
│   or short video loop — 40% of screen]  │
│                                         │
│  [Outcome headline — large, bold]       │
│  [Subheadline — 1 sentence benefit]     │
│                                         │
│  ★★★★★  4.8 · 50,000 Reviews           │
│  "Short testimonial quote"              │
│  — Name, City                           │
│                                         │
│  ✓ Benefit framed as outcome            │
│  ✓ Benefit framed as outcome            │
│  ✓ Benefit framed as outcome            │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │  ◉ Annual  $49.99/year           │   │ ← Default selected
│  │     Best value — save 58%        │   │
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │  ○ Monthly  $9.99/month          │   │
│  └──────────────────────────────────┘   │
│                                         │
│  "No payment required now"              │ ← Never remove this
│                                         │
│  ┌──────────────────────────────────┐   │
│  │  START MY FREE 7-DAY TRIAL      │   │
│  └──────────────────────────────────┘   │
│                                         │
│  ✓ Cancel anytime  ✓ Secure payment     │
│  ✓ 7 days free, then $49.99/year        │
│                                         │
│  [View All Plans]  [Restore Purchase]   │
└─────────────────────────────────────────┘
```

---

## 2. Multi-Page Paywall Framework

Best for: Health, education, wellness, high-commitment apps. Pioneered by Duolingo.

### Page 1: Personalized Promise

```
┌─────────────────────────────────────────┐
│                                         │
│  Based on your answers,                 │
│  here's what Premium unlocks for you:   │
│                                         │
│  [Personalized projection visual]       │
│  "Reach [goal] by [specific date]"      │
│                                         │
│  [Continue →]                           │
└─────────────────────────────────────────┘
```

### Page 2: Feature Highlights (outcome-framed)

```
┌─────────────────────────────────────────┐
│                                         │
│  Everything you need to [achieve goal]: │
│                                         │
│  [Icon] Feature name                    │
│         "Outcome this enables..."       │
│                                         │
│  [Icon] Feature name                    │
│         "Outcome this enables..."       │
│                                         │
│  [Icon] Feature name                    │
│         "Outcome this enables..."       │
│                                         │
│  [Continue →]                           │
└─────────────────────────────────────────┘
```

### Page 3: Social Proof

```
┌─────────────────────────────────────────┐
│                                         │
│  Join 2.4 million people who already    │
│  reached their goal                     │
│                                         │
│  [Testimonial 1 — matches user's goal]  │
│  [Testimonial 2 — matches user's pain]  │
│  [Testimonial 3 — matches user's demo]  │
│                                         │
│  ★★★★★  App Store Editors' Choice       │
│                                         │
│  [Continue →]                           │
└─────────────────────────────────────────┘
```

### Page 4: Pricing + CTA

Standard single-page paywall layout from above.

---

## 3. Pricing Design Patterns

### Pattern A: Two-Tier (Recommended Default)

```
┌─────────────────────────────────────────────┐
│                                             │
│   ┌───────────────────────────────────┐     │
│   │ ● ANNUAL         $4.16/month     │ ← Recommended
│   │   Billed as $49.99/year          │
│   │   [BEST VALUE badge]              │    │
│   └───────────────────────────────────┘    │
│                                             │
│   ┌───────────────────────────────────┐     │
│   │ ○ MONTHLY        $9.99/month     │     │
│   │   Billed monthly                 │     │
│   └───────────────────────────────────┘    │
│                                             │
└─────────────────────────────────────────────┘
```

**Key rules:**
- Annual is default-selected with visual emphasis (border, badge, checkmark)
- Show annual as per-month cost to make it look cheaper: "$4.16/mo" not "$49.99/yr"
- Show monthly at full monthly price as the anchor: "$9.99/mo"
- Savings callout: "Save 58%" or "2 months free" (test which performs better)
- Keep monthly visible — it captures users who won't commit to annual

### Pattern B: Horizontal Row (alternate design)

```
┌──────────────────┐  ┌──────────────────┐
│    MONTHLY       │  │    ANNUAL        │
│    $9.99/mo      │  │    $4.16/mo      │
│                  │  │   BEST VALUE ★   │
│ [Select]         │  │ [Selected ✓]     │
└──────────────────┘  └──────────────────┘
```

Worth testing against vertical stack — some categories perform better with this layout.

### Pattern C: Three-Tier with Hidden Options

Show two options prominently. Hide edge SKUs behind "View All Plans" link.

```
Visible:
- Monthly: $9.99/mo
- Annual: $4.16/mo [BEST VALUE]

Behind "View All Plans":
- Weekly: $3.99/week (highest price, anchors everything)
- Lifetime: $149.99 (test carefully — can hurt LTV)
- Quarterly: $19.99/quarter
```

**Use weekly plan as a price anchor:** Showing "$3.99/week" makes $9.99/month feel cheap. Even if nobody buys weekly, it frames your prices as reasonable.

### Pricing psychology rules

1. **Never show annual price as a total first.** Lead with per-month or per-week.
2. **50% discount for annual is the sweet spot.** Based on Superwall/RevenueCat data.
3. **Avoid too many pricing options.** 3 visible max. Decision paralysis is real.
4. **Trial length affects perceived value.** "7 days free, then $49.99/year" reads differently than "start free today."
5. **Price anchoring always helps.** If you only offer one price, users compare it to nothing. Show multiple to let them anchor on what feels like a deal.

---

## 4. Feature-Based (Contextual) Paywalls

When a free user attempts to access a premium feature, show a paywall that specifically highlights *that* feature. This is the highest-relevance paywall type.

### Standard contextual paywall structure:
```
┌─────────────────────────────────────────┐
│                                         │
│  [Visual of the specific feature]       │
│                                         │
│  "[Feature name] is a Premium feature"  │
│                                         │
│  Unlock [Feature Name] and:             │
│  ✓ [What this feature enables — 1 line] │
│  ✓ [Secondary benefit]                  │
│  ✓ [All other Premium features]         │
│                                         │
│  [Pricing options]                      │
│  [Start Free Trial CTA]                 │
│                                         │
└─────────────────────────────────────────┘
```

**Implementation tip (Superwall pattern):**
Use placement parameters to pass the feature name into the paywall. One paywall template can dynamically change its headline and image based on which feature was tapped. No need to build a separate paywall per feature.

**The intent signal advantage:**
A user tapping on a premium feature has just signaled exactly what they want. Showing them a generic paywall ignores that signal. Showing them a paywall that says "you tried to use X — here's what X can do for you" converts significantly better.

---

## 5. Exit & Abandoned Transaction Paywalls

### Exit Intent Paywall (user taps X to close paywall)

Only show after at least one full paywall dismissal. Revenue impact: 15–20% additional revenue per Superwall customer data.

```
┌─────────────────────────────────────────┐
│                                         │
│  Wait — here's a special offer          │
│  just for you                           │
│                                         │
│  [Reduced price or extended trial]      │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │ Annual — $34.99/year             │   │
│  │ (was $49.99 — you save 30%)      │   │
│  └──────────────────────────────────┘   │
│                                         │
│  This offer expires in: [10:00 timer]   │
│                                         │
│  [Accept Offer]                         │
│  [No thanks, I don't want to save]      │
│                                         │
└─────────────────────────────────────────┘
```

**Rules:**
- Only show once per session (not on every dismissal)
- Discount should be meaningful: 20–40% off standard price
- Countdown timer increases urgency but use honestly — reset it = bad practice
- "No thanks" copy can use mild loss framing: "No thanks, I'll pay full price later"
- Don't be deceptive — Apple/Google review teams flag manipulative patterns

### Abandoned Transaction Paywall (user started payment but cancelled)

User opened the payment sheet but tapped X before completing purchase.

This user showed very high intent. They are the most valuable segment to recover.

```
┌─────────────────────────────────────────┐
│                                         │
│  Looks like something went wrong        │
│                                         │
│  Your plan is still waiting for you.   │
│  Complete your trial activation and     │
│  get [specific benefit].               │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │ Continue with Annual — $49.99/yr │   │
│  └──────────────────────────────────┘   │
│                                         │
│  [Or try monthly — $9.99/month]         │
│                                         │
└─────────────────────────────────────────┘
```

Note: Don't immediately show a discount here — this user was willing to pay full price. Offer the discount only if they dismiss this recovery paywall too.

---

## 6. Paywall Copy Library

### Headline formulas (proven patterns)

**Outcome + timeframe:**
- "Lose your first 5 kg in 30 days"
- "Sleep 45 minutes more — starting tonight"
- "Speak fluently in 6 months"
- "Save 5 hours a week on [task]"

**Personalized plan framing:**
- "Your personalized [fitness/sleep/language] plan is ready"
- "Based on your goals, we built something just for you"
- "Your [app name] plan starts today"

**Social proof headline:**
- "Join 2.4 million people who already [achieved outcome]"
- "Rated #1 [category] app for 3 years running"
- "The app that helped [N] people [achieve goal]"

**Trial-first framing:**
- "Try everything free for 7 days"
- "Start free — cancel anytime in seconds"
- "[N] days to experience everything Premium has to offer"

### Feature-to-benefit translation guide

Always translate features into outcomes:

| Feature copy (bad) | Benefit copy (good) |
|---|---|
| "AI-powered recommendations" | "A plan that adapts to how you actually perform" |
| "Unlimited workout logs" | "See exactly how far you've come, any time" |
| "Advanced analytics dashboard" | "Know what's working and what to change" |
| "Sleep stage tracking" | "Wake up knowing exactly why you feel tired" |
| "Cloud sync across devices" | "Your progress is always with you, on any device" |
| "Offline mode" | "Train anywhere — no wifi needed" |
| "Customizable notifications" | "Stay on track without being annoyed" |

### Trust signal copy (place near CTA)
```
✓ Cancel anytime — no questions asked
✓ No charge for [X] days
✓ [N] million subscribers worldwide
✓ Rated [X.X]★ on the App Store
✓ Secure payment via Apple Pay / Google Pay
```

---

## 7. Visual Design Rules

### Hero image/video selection

Order of performance (test from top):
1. Short looping video showing app in use with real results
2. "Transformation" or outcome visual (before/after, goal achieved)
3. Lifestyle image matching user's stated goal
4. App screenshots (lowest emotional impact, highest clarity)

### Color and visual hierarchy

- CTA button: highest-contrast color in the design. Should be impossible to miss.
- Annual plan selection: visual emphasis (border, background, badge)
- "Best value" badge: use a color that pops — orange, green, or gold typically work
- Trust signals: smaller type, neutral color — don't compete with CTA
- Feature list: icons help scan speed; bullets without icons lose attention

### Typography rules

- Headline: large, bold, max 2 lines on smallest device (iPhone SE)
- Subheadline: medium weight, max 2 lines
- Feature list items: 1 line each when possible (scan, don't read)
- Price: bold for the promoted option, regular weight for the anchor
- "No payment required now": small but visible, above CTA button

### What NOT to do

- Don't put the price at the very top of the paywall (user sees cost before value)
- Don't use fine print that obscures the total billing amount (Apple/Google policy + user trust)
- Don't use fake countdown timers that reset
- Don't hide the restore purchase button (required by both stores)
- Don't use confusing pricing math that requires calculation (e.g., "$0.14/day")
- Don't stack more than 3 pricing options without a "View All" button
- Don't remove the ability to close the paywall (hard paywalls require careful framing)
