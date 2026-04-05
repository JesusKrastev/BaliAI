# Metrics & Analytics Guide

## Table of Contents
1. The Complete Metric Stack
2. RevenueCat Charts Reference
3. Superwall Analytics Reference
4. Diagnosing Problems by Metric
5. Involuntary Churn Playbook

---

## 1. The Complete Metric Stack

### Install-to-Revenue Funnel Metrics

```
METRIC 1: Install Count
Definition: Total app installs
Where to track: App Store Connect / Google Play Console / MMP (Adjust, AppsFlyer)
Target: N/A (depends on UA budget)
Action if low: UA strategy problem, not monetization problem

METRIC 2: Onboarding Completion Rate
Definition: % of users who complete all onboarding screens
Where to track: Your analytics tool (Mixpanel, Amplitude, Firebase)
Target: 85%+
Action if low: Find drop-off screen, fix or move paywall before completion

METRIC 3: Install → Paywall View Rate
Definition: % of installs that result in at least one paywall view
Where to track: Superwall, RevenueCat, or custom
Target: 85%+  (Superwall benchmark for top-performing apps)
Action if low: Move paywall earlier in funnel

METRIC 4: Paywall Conversion Rate
Definition: % of paywall views that result in trial start or purchase
Where to track: Superwall, RevenueCat
Target: 10–25% (varies heavily by category and timing)
Action if low: Test copy, pricing, design (in that order after placement is fixed)

METRIC 5: Install → Trial Start Rate
Definition: % of installs that result in trial start
Where to track: RevenueCat (Trial Conversion Chart)
Benchmark: Median across categories is ~3.7% (RevenueCat SOSA 2024)
Target: 5–10% is good; 10%+ is great
Note: This metric combines view rate × conversion rate — always diagnose which is the problem

METRIC 6: Trial → Paid Conversion Rate
Definition: % of trials that convert to paid subscription
Where to track: RevenueCat (Trial Conversion Chart)
Target: 40–60% (varies by trial length and category)
Action if low: Improve in-trial activation, add trial reminder notifications

METRIC 7: Realized LTV per User (ARPU)
Definition: Total revenue ÷ total users over a time window
Where to track: RevenueCat (Realized LTV per Customer Chart)
This is the north star metric — optimize everything to improve this
Timeframe: Measure at 30, 90, 180, 365 days

METRIC 8: 30-Day Subscriber Retention
Definition: % of subscribers still subscribed on Day 30
Where to track: RevenueCat (Subscription Retention Chart)
Target: 70%+ (voluntary churn below 30%)
Action if low: Improve in-app experience, add engagement hooks, segment churners

METRIC 9: Involuntary Churn Rate
Definition: % of subscription cancellations due to failed payment
Where to track: RevenueCat (Churn Chart, filter by reason)
Target: Below 20% of total churn
Action if above: Implement dunning, payment retry, and proactive notification
```

### Supporting Metrics

```
Average Paywall Views per User
Definition: Total paywall views ÷ total users
Superwall finding: Top-converting apps see ~2x views per user before conversion
Action if 1.0: Users aren't getting a second chance; test re-exposure strategy

7-Day Cancellation Rate per Product
Definition: % of subscribers who cancel within 7 days of trial start
Proxy for: Product-market fit and trial quality
Action if high: Trial users aren't experiencing value; improve activation during trial

Onboarding Screen Drop-off Rate
Definition: % of users who leave at each onboarding screen
Track: Every single screen separately
Flag: Any screen with >15% drop-off is a problem to investigate

Trial Reminder Push Notification Open Rate
Definition: % of users who open your trial expiry reminder
Why it matters: Push notifications before trial end significantly improve trial-to-paid
Target: 20%+ open rate
```

---

## 2. RevenueCat Charts Reference

Key charts and what to do with them:

**Trial Conversion Chart**
- Shows cohort of trial starters and % that converted to paid
- Use this to compare A/B test cohorts: which paywall variant produces higher-quality trials?
- Do NOT just compare trial start rate — compare this downstream metric

**Realized LTV per Customer (ARPU)**
- Most important chart for long-term monetization decisions
- Use for A/B test comparison: which variant produces more revenue per install?
- Compare 90-day ARPU between cohorts to validate pricing changes

**Subscription Retention Chart**
- Cohort retention curves for subscribers
- Healthy shape: fast initial drop, then flattening curve
- Problematic shape: steep continuous decline (product/value problem)
- Compare retention curves between acquisition sources to identify quality

**Churn Chart**
- Breakdown of voluntary vs. involuntary churn
- If involuntary > 25% of total churn, prioritize billing recovery
- Voluntary churn reasons (from cancellation surveys): segment and address each

**Monthly Recurring Revenue (MRR)**
- Leading indicator of business health
- Watch for: MRR growth slowing while install growth continues (monetization problem)
- Watch for: MRR growth while installs decline (great — monetization improving)

---

## 3. Superwall Analytics Reference

**Paywalled Rate (Install → Paywall View Rate)**
- The most important Superwall metric
- Strong correlation to transaction rate
- Target: 85%
- If below 60%: emergency — paywall placement is broken

**Transaction Rate**
- % of all users who complete a trial or purchase
- Includes all paywall views across all campaigns
- Superwall top-25 benchmark: 5%+ = good, 10%+ = excellent

**Paywall Views per User**
- Average number of times a user sees any paywall
- Top-converting apps: ~2x before conversion
- Low (below 1.0): users aren't getting enough exposure
- High (above 5.0): may be causing annoyance; check retention correlation

**Conversion by Placement**
- Which paywall placement is driving the most conversions?
- Compare: onboarding paywall vs. feature gate vs. re-engagement
- Use this to optimize paywall sequence and budget placement

---

## 4. Diagnosing Problems by Metric

Use this as a diagnostic flowchart:

```
Revenue is low
    ↓
Is Install → Paywall View Rate below 80%?
    YES → Fix paywall placement (Level 1 problem)
    NO  ↓
Is Paywall Conversion Rate below 10%?
    YES → Test pricing, copy, design (Level 2–4 problem)
    NO  ↓
Is Trial → Paid Conversion below 40%?
    YES → Improve in-trial activation; add trial reminder notifications
    NO  ↓
Is 30-day Subscriber Retention below 70%?
    YES → Product quality or value delivery problem; check involuntary churn
    NO  ↓
Is Realized LTV per User below category benchmark?
    YES → May be pricing problem (undercharging) or annual vs. monthly mix issue
```

---

## 5. Involuntary Churn Playbook

Involuntary churn (failed payments) is often 15–30% of total subscriber loss. This is recoverable with the right systems.

### Step 1: Measure it
In RevenueCat, look at your churn breakdown. What % is involuntary? If it's above 20%, this is a priority.

### Step 2: Enable dunning (payment retry)
Both Apple and Google automatically retry failed payments, but on their own schedule. For Android (Google Play Billing), you can configure dunning behavior. For iOS, Apple handles retries — focus on your own notification strategy.

### Step 3: Pre-expiry notifications
Send push + email 3 days before subscription renewal IF the user's payment method has had issues in the past. Many users have expired cards they haven't updated.

Notification copy:
```
Subject: Action needed — your [App Name] subscription

Your [App Name] subscription renews in 3 days and we noticed your 
payment method may need an update. Tap here to ensure uninterrupted access.
```

### Step 4: Post-failure grace period
Don't immediately revoke access when a payment fails. Give users a 7–14 day grace period. Send notifications at Day 1, Day 3, Day 7:

Day 1 (gentle):
"Your payment didn't go through — update your payment method to keep access."

Day 3 (more urgent):
"You have 4 days to update your payment before losing access to [App Name] Premium."

Day 7 (final):
"This is your last chance — update payment to avoid losing your [App Name] plan."

### Step 5: Re-subscription offer to lapsed involuntary churners
Users who lapsed due to payment failure (not by choice) are highly likely to re-subscribe if prompted. They didn't intend to cancel.

Re-engagement offer: "Welcome back — here's 1 month free to reactivate your plan."

### Step 6: Alternative payment methods
Add Apple Pay and Google Pay wherever possible. One-tap payment dramatically reduces failed payment rates (no manual card entry, always up to date).
