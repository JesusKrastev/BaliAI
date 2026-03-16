# Onboarding Architecture Deep Dive

## Table of Contents
1. Screen-by-Screen Blueprint
2. Category-Specific Patterns
3. The Long Onboarding Framework
4. Measurement & Optimization

---

## 1. Screen-by-Screen Blueprint

### Screens 1–2: The Hook
**Goal:** Stop the user from second-guessing the install. Make them feel they've found the right app.

Design:
- Full-bleed hero image or video (app in use, or outcome achieved)
- One strong outcome headline — max 6 words
- Subheadline that names the problem you solve
- CTA: "Get Started" or "Let's Go"

Copy examples:
- Fitness: "Build the body you want. / Track workouts, measure progress, reach goals — faster."
- Sleep: "Sleep like you did at 20. / Wake up knowing exactly how to feel your best."

---

### Screens 3–15: The Personalization Survey
**Goal:** Collect data, build investment, create the implicit promise of a personalized plan.

**Question order principle:** Start with easy motivational questions (goals). Progress to more personal/sensitive questions. End with data that enables your most impactful personalized projection.

**Recommended question sequence:**

1. **Goal question** (most important — sets the frame for everything)
   - "What's your main goal?" with 3–5 illustrated options
   - Used in: paywall copy, personalized projections, notification copy

2. **Experience/baseline question**
   - "How would you describe your current level/situation?"
   - Used in: plan difficulty calibration, first content recommendations

3. **Context question**
   - "How much time can you commit each day/week?"
   - Used in: schedule personalization, expectation setting

4. **Pain point question** ← High conversion value
   - "What has made it hard to achieve your goal in the past?"
   - Users who answer this feel understood → more likely to convert
   - Used in: objection handling in paywall copy

5. **Demographic/profiling question** (if relevant)
   - Age, weight, language level, etc.
   - Always add "Why we ask: [explanation]" below sensitive fields

6. **Motivation deepening question**
   - "What would reaching your goal mean to you?"
   - Users who answer this are significantly more likely to convert
   - Used in: personalized future-pacing on paywall

7. **How did you hear about us?** ← Never skip this
   - Priceless attribution data
   - Simple multiple choice: Friend/Family | App Store search | Social media | Ad | Other

8. **Commitment question** (optional but powerful)
   - "How committed are you to achieving your goal?"
   - Options: Exploring | Fairly committed | 100% committed
   - Users who select "100% committed" convert at 2–3x rate
   - Use to segment into different paywall experiences

---

### Screens 16–18: Social Proof Interlude
**Goal:** Handle objections before the paywall. Build trust while maintaining momentum.

Pattern 1 — Rating + Review:
```
★★★★★  4.8 · 50,000 ratings

"I lost 12 kg in 3 months. I've tried everything else 
and nothing worked until this app."
— Sarah M., verified subscriber
```

Pattern 2 — Scientific Credibility:
```
[Partner logo or research badge]
"[App Name]'s approach is based on [specific research method]"
[Citation or expert name and credentials]
```

Pattern 3 — User Count + Outcome:
```
2.4 million people have already reached their goal with [App Name].
```

---

### Screens 19–20: The Plan Reveal
**Goal:** Transition from "survey" to "your personalized product" framing. Create the paywall as access to something already built for them.

**The Loading Screen (do not skip this):**
- 2–3 second pause with animated loading indicators
- Text cycling through:
  1. "Analyzing your goal..." ✓
  2. "Building your schedule..." ✓
  3. "Calculating your timeline..." ✓
- Even if nothing is actually computed, this screen increases trial starts substantially
- It makes users feel the product is personalized, not generic

**The Projection Screen:**
```
Based on your profile:

Your Plan
─────────────────────────────
Start date: Today
Your goal: [Goal from survey]
Estimated milestone: [Date 4 weeks out]
Estimated goal completion: [Date based on inputs]
─────────────────────────────

[Continue to Your Plan →]
```

Loss aversion trigger: users see their timeline and immediately want it to be real. The paywall is what makes it real.

---

## 2. Category-Specific Patterns

### Health & Fitness
- Use longer onboardings (20–50 screens) — consistently increases trial rates
- Include a "body assessment" style sequence (current vs. desired state)
- Always include projection screen with goal date
- Frame paywall as "your personalized program"
- Video on paywall consistently outperforms static
- "No payment required" callout is especially important — users distrust fitness apps

### Sleep & Wellness
- Use RISE Science model: educate users they have a problem before solving it
- Include a "sleep debt calculator" or "energy score" type screen
- Outcome frame: daily energy levels, not just sleep hours
- Long onboarding validated: users need to be convinced the problem is real first

### Language Learning
- Let users complete one lesson before the paywall (creates investment — Duolingo model)
- Progress bar and streak psychology work extremely well
- Multi-page paywall is worth testing (Duolingo pioneered this pattern)
- Highlight streak protection as a key premium feature

### Journaling / Mindfulness
- Shorter onboarding often works (intrinsic motivation already present at install)
- Heavy social proof: testimonials from people in similar life situations
- Daily habit framing: "Just 5 minutes a day"
- JTBD-based personalization can lift ARPU 20%+ (Five Minute Journal case study)

### Finance / Productivity
- Users are more skeptical and analytical — lead with data, not just emotion
- ROI framing works: "Save X hours per week" or "Users save an average of $Y"
- Free trial especially important — users want to verify the product before paying
- Shorter onboarding with faster time-to-value typically wins over long surveys

---

## 3. The Long Onboarding Framework

### When friction creates value (the key insight from RevenueCat/Lose It!)

The Lose It! team discovered that adding survey questions — even ones they didn't use to change the product — increased trial start rates by double digits. The questions implied personalization, which increased trust and commitment.

**The "science-backed" framing pattern:**
```
[Screen asking detailed personal question]
   ↓ (appears to run analysis)
[Screen showing the result as a scientifically derived insight]
   ↓
[Screen connecting their data to a premium feature they'd need]
```

Example:
1. "How many hours of sleep do you get most nights?" → [4–5h / 6–7h / 8+ hours]
2. Loading: "Calculating your sleep debt..."
3. "Based on your input, you have an estimated 23-hour sleep debt — here's what that means for your daily performance..."
4. "Premium unlocks your daily Energy Score and recovery plan — start free"

### Progress bar best practices
- Always visible — reduces perceived effort
- Consider resetting mid-flow with a new label ("Setting up your plan..." after initial survey)
- Do not show "you're 90% done" and then show 5 more screens — it breaks trust
- Non-linear progress is fine: it's about managing expectation, not precise counting

### Question variety to prevent fatigue
Rotate between these types across a long onboarding:
- Multiple choice (3–5 options)
- Binary choice (faster, builds momentum)
- Slider scale ("Rate your energy level 1–10")
- Image selection ("Which of these represents your current lifestyle?")
- Open text (use sparingly — high friction, but high intent signal)
- Informational screens (no input required — let user rest)

---

## 4. Measurement & Optimization

### The onboarding funnel you must instrument

Every screen = one analytics event. Track:
- Screen view
- Time spent on screen
- Answer selected (for survey screens)
- Drop-off rate (percent who don't advance to next screen)

### What high drop-off tells you

| Drop-off Location | Likely Cause | Fix |
|---|---|---|
| Screen 1–2 (hook) | Value prop unclear or weak | Rewrite headline, test different visuals |
| Early survey screens | Questions feel irrelevant or intrusive | Add "why we ask" explanations |
| Late survey screens | Fatigue from too many similar questions | Add variety, break into sections |
| Plan reveal screen | Personalization doesn't feel real | Improve loading animation, make projection more specific |
| Paywall | Price objection, trust gap | Test pricing, add trust signals, try exit offer |

### Key benchmarks
- Onboarding completion rate target: 85%+ (if below, add paywall before completion)
- Screen-by-screen drop-off: flag anything above 15% per screen for optimization
- Time-to-first-paywall-view: under 3 minutes for most categories
