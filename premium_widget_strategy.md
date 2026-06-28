# Weatherly — Premium Widget & Monetization Strategy

> Brainstorm doc. Covers widget design, tiered API architecture, and micro-subscription model.

---

## 1. Widget Design Strategy — Material You & Dynamic Layouts

Jetpack Glance (already in the project) supports Compose-like syntax and is the right foundation. The goal is to make widgets feel premium by adapting to three distinct states rather than just scaling a static layout.

### 1a. Material You State

Map widget colors dynamically to the user's system wallpaper palette using Android's dynamic color tokens:

```kotlin
// In Glance widget — use system dynamic color
GlanceTheme(
    colors = if (Build.VERSION.SDK_INT >= 31)
        dynamicThemeColorProviders()
    else
        GlanceTheme.colors
) { ... }
```

Elements that should recolor: background tint, text accents, weather icon stroke colors. When the user changes their wallpaper, the widget seamlessly picks up the new palette — no user action needed.

### 1b. Chrono-Dynamic State

Shift layout content based on time of day — not just appearance, but information hierarchy:

| Time of day | Layout focus |
|---|---|
| **Morning** (5 AM–11 AM) | Minimalist: day's high temp + rain probability. Helps the user decide what to wear. |
| **Daytime** (11 AM–6 PM) | Standard: current conditions, hourly strip, UV index. |
| **Night** (6 PM–5 AM) | Dark ambient: tomorrow's morning temperature + any overnight events (frost, rain). |

Implementation: read `Calendar.getInstance().get(Calendar.HOUR_OF_DAY)` in the Glance `update()` call and branch layout accordingly.

### 1c. Layout Shift by Widget Size

Instead of scaling a fixed grid, change the content hierarchy entirely based on the size the user drags out:

| Size | Content |
|---|---|
| **2×1** | Current temp + condition emoji only |
| **2×2** | Current temp + condition + H/L + single tip line |
| **4×1** | Horizontal hourly strip (next 5 hours) |
| **4×2** | Full layout: header + hourly strip + rain chance bar |

In Glance, use `LocalSize.current` to branch:

```kotlin
val size = LocalSize.current
when {
    size.width >= 250.dp -> FullWidget()
    size.width >= 130.dp -> CompactWidget()
    else -> MinimalWidget()
}
```

---

## 2. Multi-Tier API & Caching Architecture

Separating free and premium data pipelines prevents API costs from eating into margins and keeps the free tier genuinely usable.

### Architecture Overview

```
                  ┌────────────────────────────────────────┐
                  │          Weatherly Core App            │
                  └──────────────────┬─────────────────────┘
                                     │
                    Is User Subscribed via Play Billing?
                                    ╱ ╲
                                   ╱   ╲
                                 YES    NO
                                 ╱       ╲
                                ▼         ▼
      ┌───────────────────────────┐     ┌───────────────────────────┐
      │     Premium Pipeline      │     │       Free Pipeline       │
      ├───────────────────────────┤     ├───────────────────────────┤
      │ • Tomorrow.io / WeatherKit│     │ • Open-Meteo (current)    │
      │ • Real-time refreshes     │     │ • Strict 3-hour caching   │
      │ • Minute-by-minute precip │     │ • Standard hourly data    │
      └───────────────────────────┘     └───────────────────────────┘
```

### Free Tier — "Eco-Friendly" Cache

- **Data source:** Open-Meteo (already integrated, CC BY 4.0, free for non-commercial)
- **Fetch strategy:** WorkManager periodic task. On widget update, check if cached timestamp is < 180 minutes old. If fresh, render cache instantly without hitting the network.
- **UX:** Widget stays responsive. A small timestamp reads "Updated 2h ago" in `TextSecondary` at 10sp.

```kotlin
// In WeatherCheckWorker
val cacheAge = System.currentTimeMillis() - forecastCache.loadTimestamp()
if (cacheAge < 3 * 60 * 60 * 1000L) {
    renderFromCache()
    return Result.success()
}
fetchAndCache()
```

### Premium Tier — Hyperlocal Stream

- **Data source:** Tomorrow.io (generous free tier with commercial license) or Apple WeatherKit (requires Apple developer account, $99/yr — less ideal for solo dev)
- **Fetch strategy:** Real-time on-demand. Trigger a background fetch whenever the widget receives a lifecycle visible broadcast.
- **UX:** Zero perceived lag. Optional manual refresh via a small glyph (↻) in the widget corner.
- **Cost check:** Tomorrow.io free tier = 25 calls/hour, 500/day. For a widget refreshing every 15 min per user, this limits you to ~33 concurrent users on the free tier. At scale, their paid plans start at ~$0/month up to 1,000 calls/day.

---

## 3. Play Store Micro-Subscriptions

### Pricing

| Plan | Price | Target user |
|---|---|---|
| Monthly | $0.49/month | Low commitment, impulse buy |
| Annual | $4.99/year | ~15% discount vs monthly, higher LTV |

At $4.99/year and Google's 15% cut (first $1M revenue), you net ~$4.24/user/year. You need roughly 7 subscribers/month to cover the Tomorrow.io paid tier entry cost.

### Integration — Google Play Billing Library

Add the dependency (free to use):
```kotlin
implementation("com.android.billingclient:billing-ktx:7.0.0")
```

Key flow:
1. On app launch, call `BillingClient.queryPurchasesAsync(SUBS)` to check subscription status.
2. Store the entitlement state in `PreferencesStore` (already exists) — `isPremium: Boolean`.
3. `WeatherRepository` reads `isPremium` to decide which data pipeline to use.
4. No custom backend needed — Google manages renewal, cancellation, and receipts entirely.

### The In-App Upsell

When a free user taps a locked premium feature (e.g., "Real-time widget updates" toggle in widget settings), show a Material 3 `ModalBottomSheet`:

```
┌────────────────────────────────────────┐
│  ✦  Weatherly Premium                 │
│                                        │
│  Minute-by-minute forecasts            │
│  Real-time widget refresh              │
│  Material You widget themes            │
│                                        │
│  Less than a cup of coffee per year.   │
│                                        │
│  [  Try free for 7 days  ]             │
│  [ $4.99/year · $0.49/month ]         │
└────────────────────────────────────────┘
```

A 7-day free trial converts significantly better than a direct paywall for weather apps. Google Play Billing supports free trial periods natively — configure in the Play Console product setup, zero code change needed.

---

## Implementation Priority (if pursuing this path)

| Step | What | Effort | Blocks |
|---|---|---|---|
| 1 | Widget size-aware layouts (Glance `LocalSize`) | 1 day | Nothing |
| 2 | Chrono-dynamic content (time-of-day branching) | half-day | Nothing |
| 3 | Material You dynamic colors in widget | half-day | Android 12+ only |
| 4 | Play Billing integration + `isPremium` flag | 1 day | Play Console account ($25) |
| 5 | Premium pipeline — Tomorrow.io API client | 1–2 days | Tomorrow.io API key (free tier available) |
| 6 | Upsell bottom sheet UI | half-day | Step 4 |

Steps 1–3 can be done now for free and make the widget feel premium even before the subscription system exists.

---

## Open-Meteo Licensing Note

If the app becomes paid or subscription-based, Open-Meteo's free tier terms no longer apply. Either:
- Switch the free tier to a commercially-licensed free API (Tomorrow.io free plan allows commercial use)
- License Open-Meteo commercially ($29/month) for the free tier users

See `playstore_claude_agy.md` for full licensing breakdown.
