# Experiment Backlog & Testing Framework

## Testing Priority Framework

Based on Superwall's internal methodology from 2,000+ paywall designs and 25M+ monthly impressions.

### The Testing Pyramid

Run experiments in this order. Higher levels have more leverage.

```
Level 1 — PLACEMENT & VIEW RATE
"Are users even seeing my paywall?"
Biggest lever. Fix this before anything else.

Level 2 — PRICING & TRIAL LENGTH
"What's the right price/trial combo for LTV?"
Run while Level 1 data matures. Takes 3–6 weeks for LTV signal.

Level 3 — COPY & MESSAGING
"Does outcome-focused copy outperform feature-focused?"
Only meaningful once placement and price are dialed in.

Level 4 — DESIGN & LAYOUT
"Does a carousel beat a feature list?"
Incremental gains. Worth pursuing after higher levels.

Level 5 — ONBOARDING SEQUENCE
"Does adding 5 more screens increase trial starts?"
Test last. Changes here affect your entire funnel.
```

Why this order: If your paywall view rate is 40%, doubling it to 80% might deliver 2x revenue. No copy test will achieve that. Fix the foundation first.

---

## Full Experiment Backlog

### Placement Experiments

| Experiment | Hypothesis | Primary Metric |
|---|---|---|
| Add pre-onboarding paywall | Earlier placement captures users who drop in onboarding | Install-to-trial rate |
| Move paywall from post-onboarding to mid-onboarding | Higher-intent moment mid-flow | Trial start rate + LTV |
| Add paywall on first app open | Maximize paywall view rate | Paywall view rate |
| Remove paywall from onboarding entirely | Post-activation paywall converts higher-quality users | Trial-to-paid rate + LTV |
| Add contextual paywall on feature unlock | Higher relevance = higher conversion | Feature-gate conversion rate |
| Add abandoned transaction paywall | Recover high-intent users who started but didn't complete | Incremental subscriptions |
| Add exit intent paywall (X button) | Capture last-chance conversions with discount | Additional revenue % |
| Test paywall at end of first session vs. beginning of second session | Return visitors may have higher intent | LTV per cohort |

### Pricing Experiments

| Experiment | Hypothesis | Primary Metric |
|---|---|---|
| Annual default-selected vs. monthly | Annual default increases LTV | LTV per install |
| 3-day trial vs. 7-day vs. 14-day trial | Shorter = urgency; longer = better activation | Trial-to-paid × LTV |
| 2 pricing options vs. 3 options | Fewer choices reduce decision paralysis | Paywall conversion rate |
| Weekly plan shown as price anchor | Makes monthly look cheap by comparison | Annual+monthly mix |
| Discount on exit offer: 20% vs. 40% | Bigger discount recovers more but lowers LTV | Revenue per dismissed user |
| Introductory price for Day 5–7 users | Time-limited offer increases urgency for fence-sitters | Conversion rate Day 5–7 |
| Lifetime offer to Day 30+ power users | High-engagement free users have high LTV | Revenue per power user |
| Price increase: +20% on annual | Users may be less price-sensitive than assumed | Revenue per trial + LTV |
| Show savings as "X months free" vs. "Save X%" | Different framing, same math | Paywall conversion rate |
| Monthly price as anchor with per-day breakdown | "$0.33/day" vs. "$9.99/month" | Conversion rate |

### Copy Experiments

| Experiment | Hypothesis | Primary Metric |
|---|---|---|
| Outcome headline vs. feature headline | "Sleep better" vs. "Advanced sleep tracking" | Trial start rate |
| "Start Free Trial" vs. "Try Premium Free" vs. "Get Started" | Different urgency and framing | CTA tap rate |
| Add "No payment required now" text above CTA | Reduces payment anxiety | Conversion rate |
| Add personalized outcome in headline | "You told us X — here's how to achieve it" | Trial start rate |
| Number of testimonials: 0 vs. 1 vs. 3 | More social proof may overwhelm or reassure | Conversion rate |
| Testimonial type: star rating vs. user count vs. named review | Different credibility signals | Conversion rate |
| Feature list: benefit-framed vs. feature-framed | "Track every workout" vs. "Unlimited workout logging" | Trial start rate |
| Loss aversion framing: "Don't lose your plan" | Stronger emotional pull than "Unlock your plan" | Trial start rate |
| Add "Cancel anytime" near CTA vs. at bottom | Trust signals near decision point | Conversion rate |

### Design Experiments

| Experiment | Hypothesis | Primary Metric |
|---|---|---|
| Feature comparison table vs. horizontal carousel | Interactive element increases engagement | Time on paywall + conversion |
| Hero: lifestyle image vs. app screenshot vs. video | Video shows product quality and outcome | Conversion rate |
| Single-page paywall vs. multi-page paywall | Multi-page builds more value before price | Trial start rate + LTV |
| Pricing: vertical stack vs. horizontal row | Different visual hierarchy | Conversion rate |
| Annual plan: highlight with border vs. badge vs. both | Stronger visual cue increases selection | Annual vs. monthly mix |
| Add video to paywall | Video shows quality and results | Conversion rate (usually wins) |
| Dark vs. light paywall theme | Category-dependent; test for your users | Conversion rate |
| Animated hero vs. static image | Animation captures attention | Conversion rate |

### Onboarding Experiments

| Experiment | Hypothesis | Primary Metric |
|---|---|---|
| Add 5 more survey screens | More questions = stronger personalization promise | Trial start rate |
| Add loading/analysis screen before paywall | Makes product feel personalized | Trial start rate |
| Add personalized projection screen before paywall | Timeline creates desire and urgency | Trial start rate |
| Add loss aversion screen before paywall | "You said X; don't lose access" | Trial start rate |
| Move "How did you hear about us?" earlier | Earlier placement = more data collected | Attribution data quality |
| Add commitment question | Segment high-intent users into different paywall | LTV by commitment level |
| Add scientific credibility screen | Increases trust in health/wellness categories | Trial start rate |
| Test onboarding with vs. without video screens | Video engagement vs. completion rate | Completion + trial rate |
| Add "Your plan is being calculated" interstitial | Investment and personalization effect | Trial start rate |

---

## How to Run a Valid A/B Test

### Setup requirements

1. **One variable only.** Change one element per test. If you change headline AND image, you don't know which caused the result.

2. **50/50 split.** Equal split between control and variant. For three variants: 34/33/33. Don't do 80/20 — you'll lack statistical power on the small variant.

3. **Minimum run time: 7 days.** Weekly behavior cycles mean Monday conversions differ from Friday. Shorter tests have misleading results.

4. **Minimum sample size.** For pricing tests: 500+ conversions per variant. For design tests: 300+ paywall views per variant. For copy tests: 200+ paywall views per variant.

5. **Measure the right metric.** Do NOT stop a test because trial start rate improved. Measure LTV or realized ARPU per user. A paywall that gets more low-intent users to trial may have the same or worse LTV.

6. **QA all variants.** Ensure the test variant works correctly across all device sizes, languages, and regions included in the test.

### The Superwall testing cadence

Large apps: Run one experiment every 7–14 days.

Process:
- Week 1–2: Run price test
- Week 3–4: Price test is still running (collecting LTV data). Run design test in parallel on a different segment.
- Week 5: Declare winner on price test. Combine winning price with winning design.
- Week 6+: Move to copy tests.

### What to do when a test loses

A losing variant is still valuable data. Document:
- What you tested
- The hypothesis
- The result
- Why you think it lost
- What you'd test next based on this learning

Build this into an experiment log. The best growth teams treat losing tests as evidence, not failures.

---

## Common Testing Mistakes

**Mistake 1: Stopping tests too early**
Seeing a 30% lift on Day 3 and declaring victory. Early data is noisy. Wait for statistical significance.

**Mistake 2: Testing only on the paywall**
Your onboarding is part of the conversion funnel. A paywall that converts 15% of users who completed a 40-screen onboarding may be worse than a paywall that converts 10% of users who completed a 5-screen onboarding — because far more users completed the shorter flow.

**Mistake 3: Optimizing trial start rate instead of LTV**
The "best" paywall for trial start rate is often the most aggressive one — showing to everyone, immediately. But it may attract low-quality subscribers who churn quickly. Always measure downstream.

**Mistake 4: Not testing placement before design**
Redesigning your paywall when your paywall view rate is 35% is like polishing a sign nobody can see. View rate first.

**Mistake 5: Ignoring seasonality**
January fitness app conversions are 3–5x higher than August. December has high gift subscription behavior. Don't compare December test results to March baselines.

**Mistake 6: Running too many tests at once**
If you have three tests running across overlapping user segments, you can't interpret any of them. Max one test per placement at a time.

---

## Seasonality & External Factors to Track

| Period | Effect on Subscription Apps | Action |
|---|---|---|
| January 1–15 | Massive fitness/wellness/productivity spike | Increase paywall aggression, test price increases |
| February (Valentine's) | Dating + relationship apps spike | Category-specific — test social proof |
| September (Back to School) | Education + productivity spike | Test student pricing, academic framing |
| November–December | Gift subscriptions, holiday generosity | Test gift subscription option, holiday messaging |
| App Store feature | Major spike in new installs (lower intent) | Increase onboarding length, soften paywall |
| iOS major release | User behavior shifts, test new OS features | Monitor conversion rate changes |
| Price increase | Users churn; also opportunities for new anchoring | Re-run pricing tests after any price change |
