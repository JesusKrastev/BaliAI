---
name: mobile-onboarding-subscription
description: >
  Comprehensive playbook for designing, writing, and optimizing mobile app onboarding flows and paywalls for subscription apps, based on battle-tested recommendations from RevenueCat and Superwall. Use this skill whenever a user asks about:
  - Building or improving an app onboarding flow or sequence
  - Designing, placing, or A/B testing paywalls
  - Increasing trial start rate, paywall conversion, or subscription revenue
  - Reducing early churn or improving user activation
  - Personalizing onboarding or paywall experiences
  - Monetization strategy for iOS/Android subscription apps
  - Writing copy, structuring screens, or choosing pricing for a subscription app
  - Any mention of RevenueCat, Superwall, free trials, in-app purchases, or subscription funnels

  This skill is a must-use whenever the user's app involves subscriptions, monetization, or onboarding — even if they only ask about one element. It encodes the most important lessons from RevenueCat's State of Subscription Apps reports, Sub Club podcast, and Superwall's internal data from 25M+ monthly paywall impressions.
---

# Mobile Onboarding & Paywall Mastery

## Quick Reference: The Core Mental Model

```
INSTALL → [ONBOARDING] → [PAYWALL(S)] → TRIAL → PAID → RETAINED
              ↑                ↑
         Activate value    Convert intent
         Build desire      Capture revenue
```

**The #1 insight from both RevenueCat and Superwall:**
Most subscription revenue is captured in the first 24 hours. The onboarding + paywall pair is the most important surface in your entire app. Everything else is optimization at the margin.

---

## Phase 1: Before You Build — Strategy & Architecture

### 1.1 Define Your "Activation Event"

Before designing a single screen, define what *activation* means for your app. Activation = the moment the user performs the app's core action and experiences real value.

| App Type | Activation Event |
|---|---|
| Fitness | First workout tracked |
| Journaling | First entry written |
| Language learning | First lesson completed |
| Sleep tracker | First night analyzed |
| Finance | First budget or transaction added |

**Why it matters:** Every onboarding screen should be building a logical path toward this activation event. Screens that don't contribute to activation or purchase intent are dead weight — or opportunities for personalization.

### 1.2 Choose Your Monetization Model

Before building, decide the model — this determines your entire paywall strategy:

**Hard Paywall (pay-to-play):** All features locked behind subscription.
- Best for: High-value, focused apps with clear ROI for user
- Required: Free trial to reduce friction
- Onboarding goal: Demonstrate value quickly, get to paywall fast

**Soft Paywall (freemium):** Core app is free, premium features gated.
- Best for: Apps where usage drives virality or network effects
- Onboarding goal: Activate on core free value, upsell contextually
- Risk: "Inattentional blindness" — users become blind to paywall prompts

**Hybrid:** Free tier + trial of premium
- Most flexible model
- Requires more complex paywall segmentation (see Phase 3)

### 1.3 Set Your Target Metrics Before Launch

Based on RevenueCat benchmarks and Superwall's top-25 app data:

| Metric | Minimum Target | Good | Great |
|---|---|---|---|
| Install → Paywall View Rate | 60% | 80% | 85%+ |
| Install → Trial Start Rate | 2% | 5–8% | 10%+ |
| Paywall View → Trial Rate | varies by category | 10–20% | 20%+ |
| Trial → Paid Conversion | varies | 40–60% | 60%+ |
| Day-1 Retention | 30% | 40% | 50%+ |

> **Superwall key insight:** There is a *direct* linear correlation between paywall view rate and transaction rate across their 25 biggest apps. Every app with a transaction rate above 5% had a paywall views-per-user of at least 1. You cannot convert users who never see your paywall.

---

## Phase 2: Onboarding Flow Design

### 2.1 The Three Jobs of Onboarding

Every screen in your onboarding must serve at least one of three jobs:

1. **EDUCATE** — Teach the user how to use the app and what value it provides
2. **ENGAGE** — Capture attention, build desire, create emotional investment
3. **CONVERT** — Move the user closer to trial start or first payment

If a screen doesn't serve one of these jobs, cut it or redesign it.

### 2.2 Short vs. Long Onboarding — How to Choose

**Default assumption (wrong for many apps):** Shorter = better. Less friction = more completions.

**The RevenueCat finding:** In health & fitness and other high-commitment categories, *longer onboardings consistently increase trial start rates* — even when the added screens don't collect data that changes the product experience.

**Why longer onboarding works:**
- More questions = implicit promise of personalization
- Longer commitment = stronger investment effect ("I've spent time here")
- More screens = more opportunities to build desire before the paywall
- Loss aversion framing: "You told us X, don't lose access to Y"

**Noom / Lose It! pattern:** Ask questions that imply a scientific, personalized output. Show a "loading" screen computing the user's personalized plan. Then present the paywall as access to that tailored plan. Trial rates went up double digits.

**When to use short onboarding:**
- Utility apps with immediate obvious value (e.g., simple tools, games)
- Apps where users are already motivated and don't need convincing
- Web-to-app funnels where pre-qualification happened already

**When to use long onboarding (10–50+ screens):**
- Health, fitness, wellness, sleep apps
- Any app where the value is not immediately obvious
- Apps where personalization is a core promise
- Categories with high skepticism (weight loss, finance, mental health)

### 2.3 The Anatomy of a High-Converting Onboarding Screen

**Each survey/question screen should include:**
```
[Progress Bar — always visible]
[Short, clear question headline]
[Subtext explaining WHY you're asking]
[3–5 answer options with icons or illustrations]
[Optional: animated transition into next screen]
```

**Each value/education screen should include:**
```
[Emotional headline — outcome-focused, not feature-focused]
[Social proof OR statistic OR testimonial]
[Visual that makes the outcome tangible]
[CTA to continue — not "Next", use something meaningful]
```

**Screen copywriting rules:**
- 5th–7th grade reading level (use Hemingway App to check)
- Headlines are outcomes, not features: "Wake up refreshed" not "Sleep tracking"
- Use second person: "your plan", "your goal", not "our app"
- Keep above-the-fold: key message visible without scrolling on smallest device

### 2.4 The Essential Onboarding Screen Sequence

**Recommended flow structure:**

```
1. HOOK (1–2 screens)
   └─ Outcome-first headline
   └─ Single powerful visual or statistic
   └─ "Get Started" CTA

2. PERSONALIZATION SURVEY (5–20 screens depending on category)
   └─ Goal setting ("What do you want to achieve?")
   └─ User profiling (relevant to your app's personalization)
   └─ Baseline measurement (if applicable)
   └─ "How did you hear about us?" (required — attribution data)
   └─ Progress bar visible throughout

3. SOCIAL PROOF INTERLUDE (1–3 screens)
   └─ App store reviews / testimonials
   └─ Scientific credibility or partnerships
   └─ Success stories matching user's stated goal

4. PERSONALIZED PLAN REVEAL (1–2 screens)
   └─ "Based on your answers, here's your plan"
   └─ Loading animation (even if not actually computing)
   └─ Show personalized projection: "You could reach X by [date]"

5. PAYWALL (see Phase 3)
```

### 2.5 Critical Design Details

**Progress bar:** Always show it. Reduces perceived friction, increases completion rates. Consider resetting it partway through (e.g., "Setting up your plan...") to make longer flows feel segmented.

**Back button behavior:** Let users go back. Removing back buttons increases anxiety and abandonment. Don't trap users.

**Provide context for every question:** Add a small "Why we ask" explanation below sensitive questions (age, weight, medical history). This builds trust and reduces drop-off on invasive screens.

**Mix question types:** Don't just ask multiple choice. Add sliders, image selections, tappable cards. Variety maintains engagement over long flows.

**Loss aversion framing before paywall:** Summarize what the user has told you. "You said you want to lose 10kg by March. Premium members hit goals 3x faster. Don't lose access to your plan." This is the single highest-impact copywriting technique in the category.

---

## Phase 3: Paywall Design & Placement

### 3.1 Paywall Placement Strategy — The Most Important Decision

RevenueCat data: Most trial starts happen within the first 24 hours of install. Superwall data: Apps with 85%+ install-to-paywall view rate consistently outperform on revenue.

**The 4 Paywall Placement Options:**

| Placement | When to Use | Notes |
|---|---|---|
| **Pre-onboarding** | Cold traffic, skeptical users | Maximizes views, but lower intent. Jake Mor doubled revenue at FitnessAI this way. One developer moved paywall to start of onboarding → 5x revenue, near zero negative feedback |
| **Mid-onboarding** | After initial value screens, before full activation | Good for balancing view rate and intent |
| **Post-onboarding** | After full personalization survey | Highest intent audience; lower view rate. ~50% of Mojo's trial starts come from here |
| **Contextual (feature gate)** | When user attempts premium action | Highest relevance — show paywall tailored to feature attempted |

**Recommended default strategy (test from here):**
Show paywall at end of onboarding. Then add a pre-onboarding paywall and measure incrementality. Most apps that add the pre-onboarding paywall see higher overall revenue because they capture users who would have dropped off during onboarding.

> **Superwall rule:** If your onboarding completion rate is below 85%, you must show a paywall before onboarding ends — you're leaving money on the table.

**80% paywall view rate** is the industry benchmark to target. If you're below this, placement is your #1 fix before touching design or copy.

### 3.2 Paywall Design Principles

**The Hierarchy of a Paywall:**
```
1. Hero visual (app in use, outcome achieved, or emotion-evoking)
2. Value headline (outcome, not feature list)
3. Social proof (testimonials, ratings, user count)
4. Feature/benefit list (3–5 items max)
5. Pricing options (1–3 options, clear visual winner)
6. Primary CTA (trial or purchase)
7. Trust signals (cancel anytime, no charge now, privacy)
8. Secondary option (if applicable)
```

**Critical copy elements:**
- Add "No payment required now" text near the CTA — Superwall reports this increases conversions *every single time* with no clear explanation of why
- Use "Start Free Trial" not "Subscribe" — trial framing reduces anxiety
- Show the price *after* building value, not at the top
- Display "billed annually" clearly to avoid refund issues and Apple/Google policy violations

**Social proof placement:** Don't wait until the paywall to introduce social proof. Front-load it throughout onboarding. By paywall time, the user should already feel confident. Use star ratings, user counts, named testimonials.

### 3.3 Pricing Strategy

**Number of options:** 1–3 max. 2 is often optimal. More than 3 causes decision paralysis and reduces conversion.

**The Annual vs. Monthly anchor:**
- Always show monthly as a "price anchor" to make annual look like an obvious deal
- A 50% discount on annual vs. monthly is highly effective
- Highlight annual as the "best value" with visual emphasis
- Keep monthly available — it captures users who won't commit to annual, and can upsell to annual later
- If you only want annual subscribers, de-emphasize monthly but don't remove it

**"View All Plans" button:** Hide edge-case SKUs (lifetime, quarterly) behind this. Show only your 2 main options prominently. This reduces visual clutter while capturing full demand curve.

**Exit offer / Abandoned paywall discount:**
When user taps X to close paywall, show a one-time discounted offer. Superwall customers report 15–20% additional revenue from this pattern alone. Do not show this on first view — only after at least one dismissal.

**Free trial length:** Test 3-day, 7-day, 14-day. 7 days is most common but not always optimal. Shorter trials can increase urgency. Longer trials can improve trial-to-paid if activation takes time.

### 3.4 Paywall Copy Frameworks

**JTBD (Jobs-To-Be-Done) Framework:**
Frame every benefit in terms of the user's stated goal, not your app's features.

```
❌ "Advanced sleep tracking algorithms"
✅ "Wake up knowing exactly why you feel tired — and how to fix it"

❌ "Unlimited workout logs"  
✅ "Track every session so you can see how far you've come"

❌ "AI-powered recommendations"
✅ "A coach that learns your schedule and builds your perfect plan"
```

**The headline formula:**
`[Desired outcome] + [timeframe or ease] + [without the pain they're trying to avoid]`

Examples:
- "Reach your goal weight by summer — without counting every calorie"
- "Sleep 45 minutes more per night — starting this week"
- "Speak fluently in 6 months — no boring textbooks"

### 3.5 Multi-Page Paywalls

Trend: Multi-page paywalls (2–4 screens that walk through value before showing price) are performing well, especially in health and education. Duolingo pioneered this format.

Structure:
```
Page 1: Personalized outcome promise ("Based on your profile...")
Page 2: Feature highlights with visual demos
Page 3: Social proof / transformation stories
Page 4: Pricing + CTA
```

This is worth testing in health, wellness, fitness, and education categories. The extra screens allow continued value-building and loss aversion framing before the decision screen.

---

## Phase 4: Segmentation & Personalization

### 4.1 User Segments to Build

Segment users by:
- **Days since install:** New (<1 day), recent (1–7 days), returning (7+ days)
- **Paywall views:** Never seen, seen 1–3x, seen 4+ times
- **Subscription status:** Free, trial, paid, lapsed
- **Feature engagement:** Which features they've used or attempted
- **Onboarding answers:** Their stated goals, demographics, use case

### 4.2 Paywall Personalization Tactics

**Feature-based paywalls:** When a user tries to access a premium feature, show a paywall specifically highlighting *that feature*. "You tried to access [Feature X]. With Premium, you can..." Superwall enables this via placement parameters.

**Onboarding-informed paywalls:** Use answers from your onboarding survey to change paywall copy, images, and even pricing dynamically. A sports betting app that knows a user selected "basketball" should show a basketball-themed paywall, not a generic one. Personalized paywalls outperform generic by 15%+.

**Acquisition-source paywalls:** If you know a user came from a specific ad campaign or keyword, tailor the paywall to match the promise of that ad. Duolingo shows different paywalls based on entry point: from the shop (focus on hearts), from an ad (focus on no ads).

**Frequency control:** Show paywalls to free users no more than 1–2x per week to avoid "inattentional blindness" and app abandonment. Test this limit — it varies by category.

### 4.3 Re-engagement Sequences

**The post-install messaging sequence (for free users who didn't convert):**

```
Day 0: Welcome + app tour (push or in-app)
Day 1: Highlight the #1 value moment they haven't experienced yet
Day 3: Social proof + soft upsell CTA
Day 5: Feature unlock reminder or "limited time offer"
Day 7: Direct paywall prompt with urgency framing
Day 14: Re-engagement with survey ("What's blocking you?")
Day 30: Deep discount or alternative offer
```

**Lapsed subscriber re-engagement:**
- Segment churners: understand *why* they left (use cancellation surveys)
- Offer discounts targeted to specific reasons for leaving
- Remind them what they're missing (feature usage data)
- Don't assume all churners are price-sensitive — some left due to lack of value, and discounts won't fix that

---

## Phase 5: Experimentation Framework

### 5.1 Testing Priority Order

Based on Superwall's internal framework, run tests in this order:

1. **Paywall placement / view rate** — fix this first; nothing else matters if users don't see the paywall
2. **Pricing & trial length** — find the price/trial combo that maximizes LTV (not just conversions)
3. **Copy & headline** — test outcome-focused vs. feature-focused, JTBD framing
4. **Design & layout** — images, colors, visual hierarchy, carousels vs. lists
5. **Onboarding length & content** — add/remove screens, reorder, test question types
6. **Frequency & segmentation** — when to show, how often, to which segments

> **The Superwall rule:** Don't start all experiments at once. Run price tests first, collect 2–4 weeks of data, then layer in design tests. Large apps run experiments every 7–14 days.

### 5.2 How to Set Up A/B Tests

**Minimum viable test setup:**
- 50/50 split between control and variant (or 34/33/33 for three variants)
- One variable changed per test
- Minimum sample size: enough to reach statistical significance (~1,000+ conversions per variant for pricing tests)
- Run for at least 7 days to capture weekly behavior cycles
- Primary metric: Realized LTV per user (not just trial start rate)

**Warning: Trial start rate alone is a misleading metric.** A paywall that shows to lower-intent users may have a lower trial-to-paid conversion rate, even if the overall revenue per install is higher. Always measure downstream (LTV), not just top-of-funnel.

### 5.3 Experiment Backlog (Copy-Paste Ready Ideas)

**Placement tests:**
- Pre-onboarding paywall vs. post-onboarding
- Paywall on first app open vs. after first core action
- Add abandoned-transaction paywall (show discount when user starts payment but cancels)
- Add post-dismissal paywall (show different offer when user closes paywall)

**Pricing tests:**
- Annual vs. monthly as default-selected option
- 3-day vs. 7-day vs. 14-day free trial
- 2-tier pricing vs. 3-tier pricing
- Emphasize monthly as anchor to make annual look cheaper
- Lifetime offer to power users (test carefully — can hurt LTV)
- Introductory offer to users who've been free for 7+ days

**Copy tests:**
- Outcome headline vs. feature list headline
- "Start Free Trial" vs. "Try Premium Free" vs. "Get Started"
- "No payment required" callout below CTA (usually wins)
- Number of testimonials: 1 vs. 3 vs. none
- Social proof type: star rating vs. user count vs. named testimonial

**Design tests:**
- Feature comparison table vs. horizontal carousel
- Hero: app screenshot vs. lifestyle image vs. animated demo
- Single-page paywall vs. multi-page paywall
- Pricing: horizontal row vs. vertical stack
- Adding a video to paywall (often significantly improves conversion)

**Onboarding tests:**
- Adding 5 more survey screens to long onboarding (usually increases trial rate)
- Adding a "loading/analyzing" screen before paywall
- Adding personalized projection screen ("You could reach X by date Y")
- Moving "how did you hear about us" to earlier vs. later in flow

---

## Phase 6: Metrics & Analytics Setup

### 6.1 The Core Funnel to Instrument

```
[Install]
    ↓ measure: install count
[Onboarding Start]
    ↓ measure: step completion at every screen (use screen-level drop-off)
[Onboarding Complete]
    ↓ measure: completion rate
[Paywall View]
    ↓ measure: install-to-paywall view rate (target: 85%)
[Trial Start or Purchase]
    ↓ measure: paywall conversion rate + trial start rate
[Trial Active Day 3]
    ↓ measure: early trial retention
[Trial → Paid Conversion]
    ↓ measure: trial conversion rate (target: 40–60%)
[Subscriber Day 30]
    ↓ measure: 30-day renewal rate, voluntary churn
[Subscriber Day 90+]
    ↓ measure: realized LTV
```

### 6.2 The Metrics That Matter Most

**Leading indicators** (fast feedback, optimize first):
- Install → Paywall View Rate
- Paywall View → Trial Start Rate
- Onboarding screen drop-off (find the leak)

**Lagging indicators** (true revenue signal, use to validate changes):
- Realized LTV per user (RevenueCat ARPU Chart)
- Trial → Paid conversion rate by cohort
- 12-month subscriber retention

**The most neglected metric (per Superwall):**
- Average paywall views per user: top-converting apps see users viewing paywalls ~2x before converting. If your users only see it once, you may be showing it too infrequently or too late.

### 6.3 Involuntary Churn — The Silent Revenue Killer

RevenueCat data shows involuntary churn (failed payments) is a major source of subscriber loss that most developers ignore.

**Tactics to reduce involuntary churn:**
- Enable dunning (retry failed payments on a schedule)
- Send push + email notifications before renewal failures
- Validate payment info before processing
- Support Apple Pay and local payment methods
- Send "payment failed" notifications within 24 hours of failure

---

## Phase 7: Web-to-App Funnels

### 7.1 When to Use a Web Funnel

Web-to-app funnels are now a core growth strategy for subscription apps, not just a way to avoid App Store fees. Benefits include:
- More control over the user journey
- Better tracking and attribution
- Access to new acquisition channels (Google, Meta, email)
- Higher renewal rates than App Store subscriptions in some cases
- Ability to run longer surveys and video content before install

### 7.2 Web Funnel Best Practices

**Continuity is critical:** The web page must match the onboarding experience in tone, visuals, and messaging. Users who see a fitness-themed web ad, click through to a generic landing page, and then get a generic onboarding will churn at each disconnection.

**Web onboarding = longer and richer:** The web allows you to run a 20-question survey before the user even installs. Use this to pre-qualify, personalize deeply, and set up loss aversion before they're in the app.

**Use a survey to segment early:** Ladder's model (per RevenueCat): web survey collects goals → user is segmented into a persona → personalized onboarding and paywall inside the app follows the web persona.

**Paywall before or after install?** Increasingly, high-performing apps show the paywall on the web (before install) for high-intent cold traffic. A/B test this — some categories convert far better with a pre-install paywall.

---

## Reference: Quick-Copy Templates

### Onboarding Question Screen Template
```
[Progress: ████████░░ 80%]

What's your main goal?

We use this to build your personalized plan.

○ [Icon] Option A
○ [Icon] Option B  
○ [Icon] Option C
○ [Icon] Something else
```

### Social Proof Screen Template
```
[Large number, e.g. "2.4M"]
people have already reached their goal 
with [App Name]

★★★★★ "This app changed my life..."
— [Name], [City/Profession]

[Continue →]
```

### Personalized Plan Reveal Template
```
[Loading animation: 2–3 seconds]

"Building your personalized plan..."
Analyzing your goals ✓
Calculating your timeline ✓  
Preparing your program ✓

Your plan is ready →
```

### Paywall Headline Templates (fill in the blanks)
```
"Reach [goal] by [date] — without [the thing they hate]"
"Join [N] people who already [achieved outcome]"
"Your personalized [outcome] plan is ready — start free"
"[Time] from now, you could be [outcome]"
```

### Paywall CTA Copy Options (tested, in approximate performance order)
```
1. "Start My Free [X]-Day Trial"
2. "Try [App Name] Free"
3. "Get Started — Free for [X] Days"
4. "Continue with Free Trial"
5. "Unlock My Plan" (works well after personalized plan reveal)
```

### Trust Signals (place near CTA)
```
✓ Cancel anytime
✓ No charge for [X] days  
✓ [N] million happy subscribers
✓ Rated [X]★ on the App Store
```

---

## Appendix: Checklist Before Launch

Use this checklist before shipping your onboarding and paywall:

**Onboarding:**
- [ ] Activation event defined and every screen serves a path to it
- [ ] Progress bar visible on all survey screens
- [ ] Context explanation ("why we ask") on all sensitive questions
- [ ] "How did you hear about us?" screen included
- [ ] Social proof integrated before paywall (not just on paywall)
- [ ] Personalized plan reveal or projection screen before paywall
- [ ] Onboarding completion event tracked in analytics
- [ ] Drop-off measured at every screen

**Paywall:**
- [ ] Install-to-paywall view rate target set (aim for 85%)
- [ ] Placement tested: end of onboarding as baseline
- [ ] "No payment required now" text below CTA
- [ ] 1–3 pricing options max, clear visual winner
- [ ] Annual plan de-emphasizes monthly using price anchoring
- [ ] Exit/dismissal offer configured (15–20% revenue uplift)
- [ ] "View All Plans" for edge SKUs
- [ ] Video or animated demo present (test against static)
- [ ] Trust signals visible without scrolling

**Analytics:**
- [ ] Every onboarding step tagged as analytics event
- [ ] Install → paywall view rate dashboarded
- [ ] Trial start rate by cohort tracked
- [ ] Realized LTV chart set up in RevenueCat
- [ ] Involuntary churn / dunning configured
- [ ] A/B test framework ready (Superwall or equivalent)

---

## Reading Order for Deep Dives

→ For onboarding flow architecture: see `references/onboarding-architecture.md`
→ For paywall design patterns with examples: see `references/paywall-patterns.md`
→ For experiment ideas and prioritization: see `references/experiment-backlog.md`
→ For metrics and analytics setup: see `references/metrics-guide.md`

**Primary sources this skill is based on:**
- RevenueCat: State of Subscription Apps 2024, Sub Club podcast, blog.revenuecat.com/growth
- Superwall: Blog at superwall.com/blog, internal data from 25M+ monthly paywall impressions
- Jacob Rushfinn (Retention.Blog): 2,000+ paywall designs, weekly growth calls
- Noom, Lose It!, RISE Science, Mojo, FitnessAI: case studies from RevenueCat
