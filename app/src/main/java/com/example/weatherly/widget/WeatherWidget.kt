package com.example.weatherly.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.weatherly.MainActivity
import com.example.weatherly.data.model.AlertSeverity
import com.example.weatherly.data.model.HourEntry
import com.example.weatherly.data.model.WeatherAlert
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.prefs.ForecastCache
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.data.repository.WeatherRepository
import com.example.weatherly.location.LocationProvider
import com.example.weatherly.util.skyColor
import com.example.weatherly.util.wmoText
import java.util.Calendar

// ── Size breakpoints ──────────────────────────────────────────────────────────
// Glance picks the largest size that fits the widget's actual dimensions, but in Responsive
// mode LocalSize.current still reports the *breakpoint's* nominal size, not the widget's true
// on-screen size — so a real host frame bigger than the largest declared breakpoint stretches
// the background (it's on the outer RemoteViews container) while content inside stays laid out
// for the smaller nominal size, leaving blank space around it (see B24 in IMPROVEMENTS.md).
// XLARGE plus centering content within the actual frame (see WidgetContent below) narrows that
// gap rather than eliminating it outright — Glance has no way to report the true host size back
// to Responsive-mode content.
//
// MARGIN exists because Responsive matching turned out to need real headroom, not just an exact
// or greater-or-equal fit: confirmed via the widget QA harness (see IMPROVEMENTS.md) that a real
// host frame sized to a breakpoint's exact nominal dp value — even a fraction of a dp *larger*
// after ruling out simple px-rounding — still failed to select that breakpoint and silently
// rendered the next one down (MEDIUM/TALL/WIDE/LARGE all fell back to SMALL's bare icon+temp
// layout; XLARGE fell back to LARGE's). This points to Glance/AppWidgetHostView subtracting some
// system-reserved inset (corner radius / launcher margin) from the reported size before comparing
// it against declared breakpoints. Shrinking each declared breakpoint below its true target size
// gives that headroom back. Binary-searched empirically via the QA harness: 8dp was NOT enough
// (MEDIUM still downgraded to SMALL at a real 110x110dp host); 16dp fixed selection for every
// tier. SMALL is left unshrunk — it's the floor of the declared set (Responsive always falls back
// to it), so there's nothing for it to downgrade to.
//
// KNOWN TRADE-OFF, not yet resolved: shrinking the declared breakpoint also shrinks the layout
// canvas Glance composes that tier's content for (Glance has no way to decouple "match against
// this size" from "lay out content for this size"). XLARGE had slack in its original design and
// renders cleanly at the smaller canvas. MEDIUM/TALL/WIDE/LARGE did not — their content, tuned to
// just fit the original (larger) declared size, now visibly clips at MARGIN=16 (confirmed via the
// QA harness: cut-off degree symbols, truncated location text, a clipped last hourly row/column).
// Fixing that needs actual layout tightening inside `MediumWidget`/`TallWidget`/`WideWidget`/
// `LargeWidget` (smaller fonts and/or tighter spacing) sized for a real MARGIN-dp-smaller canvas,
// not another constant tweak — deliberately not attempted yet, flagged as the next step instead.
private const val MARGIN = 16
private val SMALL  = DpSize(110.dp,        50.dp)        // 2×1: temp + emoji only
private val MEDIUM = DpSize((110 - MARGIN).dp, (110 - MARGIN).dp)  // 2×2: chrono-dynamic vertical stack
private val TALL   = DpSize((110 - MARGIN).dp, (220 - MARGIN).dp)  // 2×4-ish: MEDIUM content + a vertical mini hourly list
private val WIDE   = DpSize((250 - MARGIN).dp,  (50 - MARGIN).dp)  // 4×1: current + upcoming hourly strip
private val LARGE  = DpSize((250 - MARGIN).dp, (110 - MARGIN).dp)  // 4×2: header + full hourly strip
private val XLARGE = DpSize((300 - MARGIN).dp, (250 - MARGIN).dp)  // 5×5-ish: header + alert + 7-hour strip

private enum class TimeOfDay { MORNING, DAYTIME, NIGHT }

private fun currentTimeOfDay(): TimeOfDay {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h in 5..10  -> TimeOfDay.MORNING   // 5 AM – 10 AM: plan the day
        h in 11..17 -> TimeOfDay.DAYTIME   // 11 AM – 5 PM: current conditions
        else        -> TimeOfDay.NIGHT     // 6 PM – 4 AM: prep for tomorrow
    }
}

// ── Colors ────────────────────────────────────────────────────────────────────

private data class WColors(
    val bg: ColorProvider,
    val bgColor: Color,
    val textPrimary: ColorProvider,
    val textSecondary: ColorProvider,
    val isDark: Boolean,
)

// Convert an Android ARGB color int to a Compose Color.
private fun Int.toComposeColor() = Color(
    alpha = (this ushr 24 and 0xFF) / 255f,
    red   = (this ushr 16 and 0xFF) / 255f,
    green = (this ushr  8 and 0xFF) / 255f,
    blue  = (this          and 0xFF) / 255f,
)

private fun ctx2color(context: Context, @ColorRes id: Int) =
    ContextCompat.getColor(context, id).toComposeColor()

// Resolve widget colors outside the composable so there's no theme dependency.
// On API 31+, reads system dynamic accent colors derived from the user's wallpaper
// (Material You). Reads dark mode state from the context so the right palette
// shade is selected at each widget update.
private fun resolveWidgetColors(context: Context): WColors {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // system_accent1 palette: 100 = near-white tint, 900 = near-black tint.
        // Light mode: light container (100) + dark text (900).
        // Dark mode: dark container (700) + light text (100).
        val isDark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val bg   = if (isDark) ctx2color(context, android.R.color.system_accent1_700)
                   else        ctx2color(context, android.R.color.system_accent1_100)
        val text = if (isDark) ctx2color(context, android.R.color.system_accent1_100)
                   else        ctx2color(context, android.R.color.system_accent1_900)
        return WColors(
            bg            = ColorProvider(bg),
            bgColor       = bg,
            textPrimary   = ColorProvider(text),
            textSecondary = ColorProvider(text.copy(alpha = 0.65f)),
            isDark        = isDark,
        )
    }
    // Pre-API 31: static dusty-blue palette matching the app's primary color.
    val isDarkPre31 = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val bgPre31 = Color(0xFF6B86A3)
    return WColors(
        bg            = ColorProvider(bgPre31),
        bgColor       = bgPre31,
        textPrimary   = ColorProvider(Color.White),
        textSecondary = ColorProvider(Color.White.copy(alpha = 0.75f)),
        isDark        = isDarkPre31,
    )
}

// ── Widget ────────────────────────────────────────────────────────────────────

/** [data] is null only when there's truly nothing to show yet (never opened the app). */
private data class LoadedWeather(val data: WeatherData?, val cachedAt: Long?)

class WeatherWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, TALL, WIDE, LARGE, XLARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loaded = loadWeather(context)
        val colors = resolveWidgetColors(context)
        val transparent = PreferencesStore(context).getWidgetTransparent()
        provideContent { WidgetContent(loaded, colors, transparent) }
    }

    private suspend fun loadWeather(context: Context): LoadedWeather {
        val cached = ForecastCache(context).load()
        return try {
            val prefs = PreferencesStore(context)
            val units = prefs.getUnitSystem()
            val repo = sharedRepository(context)
            // Deliberately ignores PreferencesStore.getSelected() — the in-app "selected place"
            // reflects whatever city the user is currently *browsing* in the app (e.g. checking
            // a different city's forecast), which isn't the same thing as "where I actually am
            // right now." A home-screen widget is a glance surface, not a navigation state, so it
            // always resolves to the device's real current location instead of silently mirroring
            // the app's browsing selection (confirmed as a real, user-reported point of confusion
            // — the widget kept showing whatever place had last been searched in-app).
            val fresh = run {
                val latLon = LocationProvider(context).currentLatLon()
                if (latLon != null) repo.getWeather(latLon.first, latLon.second, units).getOrNull()
                else null
            }
            // Fall back to the app's last cached forecast so the widget always
            // shows real data once the user has opened the app at least once.
            if (fresh != null) LoadedWeather(fresh, cachedAt = null)
            else LoadedWeather(cached?.first, cachedAt = cached?.second)
        } catch (e: Exception) {
            LoadedWeather(cached?.first, cachedAt = cached?.second)
        }
    }
}

// GlanceAppWidget instances are created fresh per update (e.g. every WeatherWidget() call in
// WeatherViewModel), so WeatherRepository's 30-minute in-memory cache — an instance field, see
// WeatherRepository.kt's `memoryCache` — only helps across calls if the same repository instance
// survives them. Held at file scope (not tied to any single GlanceAppWidget instance) so scheduled
// updates, every onAppWidgetOptionsChanged (i.e. every resize), and RefreshAction's manual refresh
// (below) all share one cache instead of each starting a brand-new forecast/air-quality/NWS fetch
// from scratch — confirmed via the widget QA harness that this was masking two other bugs: a
// 25-50s blank-spinner load on every single update, and options changes racing/restarting an
// in-flight fetch.
private val sharedRepoLock = Any()
@Volatile
private var sharedRepo: WeatherRepository? = null

private fun sharedRepository(context: Context): WeatherRepository =
    sharedRepo ?: synchronized(sharedRepoLock) {
        sharedRepo ?: WeatherRepository(context.applicationContext).also { sharedRepo = it }
    }

// ── Root composable ───────────────────────────────────────────────────────────

// User-controlled via Settings → Widget Background (PreferencesStore.getWidgetTransparent()).
// 0.55 keeps the wallpaper visibly showing through while staying readable against most
// wallpapers — text contrast was calibrated against a fully opaque background, so going much
// lower risks real readability problems on busy wallpapers, which is why this is an opt-in
// setting rather than the default.
private const val TRANSPARENT_ALPHA = 0.55f

@Composable
private fun WidgetContent(loaded: LoadedWeather, c: WColors, transparent: Boolean) {
    val data = loaded.data
    val time = currentTimeOfDay()
    val open = actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))
    val alpha = if (transparent) TRANSPARENT_ALPHA else 1f

    // Condition-aware gradient background — sky tone (skyColor(), the same shared source of
    // truth as the in-app hero, see util/ConditionColors.kt) fading into the widget's existing
    // Material You accent color (c.bgColor), rather than replacing that personalization outright.
    // A small bitmap is enough (not pixel-exact to the real widget size, which Glance can't
    // report anyway in Responsive mode) since a vertical gradient scales losslessly with
    // FillBounds. remember() so this isn't re-rendered on every recomposition, only when the
    // inputs actually change.
    val backgroundModifier = if (data != null) {
        val bitmap = remember(data.currentIcon, data.isDay, c.isDark, c.bgColor, alpha) {
            renderGradientBitmap(
                topColor = skyColor(data.currentIcon, data.isDay, c.isDark).copy(alpha = alpha),
                bottomColor = c.bgColor.copy(alpha = alpha),
                widthPx = 4,
                heightPx = 200,
            )
        }
        GlanceModifier.background(ImageProvider(bitmap), ContentScale.FillBounds)
    } else {
        GlanceModifier.background(ColorProvider(c.bgColor.copy(alpha = alpha)))
    }

    val base = GlanceModifier
        .fillMaxSize()
        .cornerRadius(20.dp)
        .then(backgroundModifier)
        .clickable(open)

    // Responsive mode lays content out for the matched breakpoint's nominal size, not the
    // widget's true on-screen size (see the XLARGE comment above) — centering here means any
    // leftover space around a smaller-than-actual layout reads as balanced padding instead of
    // content stuck in one corner with a blank void elsewhere (B24 in IMPROVEMENTS.md).
    //
    // The manual-refresh icon (see RefreshButton below) is NOT placed here as a corner overlay —
    // an Image (any Image, confirmed even with the already-proven WidgetGlyph) silently fails to
    // render as a second sibling of a fillMaxSize() child inside a Box, even though a plain
    // Box+background renders fine in that exact position. Root cause not fully understood (a
    // RemoteViews/Glance ImageView measurement quirk in that specific nesting shape, most likely)
    // — worked around by placing RefreshButton inline within each tier's own header row instead,
    // the same proven pattern every other icon in this file already uses successfully.
    Box(modifier = base, contentAlignment = Alignment.Center) {
        when (LocalSize.current) {
            // MEDIUM/TALL/WIDE/LARGE's padding is tighter than XLARGE's — those four tiers'
            // declared canvas is MARGIN dp smaller than the size their content was originally
            // designed to fill (see the MARGIN comment above), so every dp of padding reclaimed
            // here is a dp back for content that was otherwise clipping. XLARGE keeps its
            // original padding — its declared size has slack the others don't.
            XLARGE -> XLargeWidget(GlanceModifier.padding(14.dp), data, loaded.cachedAt, time, c)
            LARGE  -> LargeWidget(GlanceModifier.padding(8.dp), data, loaded.cachedAt, time, c)
            WIDE   -> WideWidget(GlanceModifier.padding(horizontal = 8.dp, vertical = 2.dp), data, c)
            TALL   -> TallWidget(GlanceModifier.padding(8.dp), data, loaded.cachedAt, time, c)
            MEDIUM -> MediumWidget(GlanceModifier.padding(8.dp), data, time, c)
            else   -> SmallWidget(GlanceModifier.padding(8.dp), data, c)
        }
    }
}

// ── Manual refresh (shared by every tier except SMALL) ───────────────────────

// System-scheduled updates (android:updatePeriodMillis) run at most every 30 minutes — that's
// the OS-enforced floor, not something this app controls, so it can't be sped up from the
// manifest side. This gives an explicit, immediate alternative: tapping the icon forces a real
// network re-fetch (forceRefresh = true, bypassing WeatherRepository's 30-minute cache) using the
// same sharedRepository(context) instance loadWeather() reads from, so the result is immediately
// visible to the normal provideGlance() call that follows.
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val units = PreferencesStore(context).getUnitSystem()
        val latLon = LocationProvider(context).currentLatLon()
        if (latLon != null) {
            sharedRepository(context).getWeather(latLon.first, latLon.second, units, forceRefresh = true)
        }
        WeatherWidget().update(context, glanceId)
    }
}

// Deliberately the smallest tappable element in the widget — a plain circular-arrow glyph with no
// background chip or label, so it reads as a subtle utility affordance rather than competing with
// the actual weather content for attention. padding() around the small icon widens the real tap
// target without growing what's visually drawn. Placed inline within each tier's own header row
// (never as a corner overlay — see the WidgetContent comment above for why that doesn't render).
@Composable
private fun RefreshButton(c: WColors) {
    Text(
        "↻",
        style = TextStyle(color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        modifier = GlanceModifier
            .padding(horizontal = 4.dp)
            .clickable(actionRunCallback<RefreshAction>()),
    )
}

// ── Alert indicator (shared by MEDIUM/WIDE/LARGE/XLARGE) ─────────────────────

// Mirrors the accent tones from WeatherComponents.kt's alertColors() for visual consistency
// with the in-app alert strip — Glance has no MaterialTheme/@Composable dependency, so this is
// a plain, non-Composable copy of just the values, not a reuse of that function directly.
private fun widgetAlertColor(severity: AlertSeverity, isDark: Boolean): Color = when (severity) {
    AlertSeverity.EXTREME, AlertSeverity.SEVERE ->
        if (isDark) Color(0xFFE8927A) else Color(0xFF9C3B26)
    AlertSeverity.MODERATE ->
        if (isDark) Color(0xFFE0B15C) else Color(0xFF8A5A1E)
    AlertSeverity.MINOR, AlertSeverity.UNKNOWN ->
        if (isDark) Color(0xFF7FA8C9) else Color(0xFF3F5670)
}

@Composable
private fun AlertIndicator(alerts: List<WeatherAlert>, isDark: Boolean, c: WColors) {
    val top = alerts.firstOrNull() ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(7.dp)
                .cornerRadius(4.dp)
                .background(widgetAlertColor(top.severity, isDark)),
            content = {},
        )
        Spacer(GlanceModifier.width(4.dp))
        val label = if (alerts.size > 1) "${top.event} +${alerts.size - 1}" else top.event
        Text(
            label,
            style = TextStyle(
                color = ColorProvider(widgetAlertColor(top.severity, isDark)),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

// ── Location name (shortened for the widget's tight width) ───────────────────

// WeatherRepository.reverseGeocode() formats data.locationName as "City, State" (e.g. "Mountain
// View, California", or a full township + state name that's meaningfully longer). Glance's Text
// composable has no ellipsize/overflow control at all (confirmed against the library source —
// maxLines only calls TextView.setMaxLines(), never setEllipsize()), so a real device showed this
// hard-clipped mid-character rather than nicely truncated with "…" — a real, user-reported bug,
// not just a cosmetic nicety. Dropping the state/region and showing only the city name is the
// direct fix given Glance can't ellipsize for us; it keeps the common case short enough to fit
// every tier that displays it, at the cost of losing the region for same-named cities in
// different states (an acceptable trade for a glanceable widget over the more detailed in-app
// screen, which still shows the full name).
private fun widgetLocationName(name: String): String = name.substringBefore(",").trim().ifBlank { name }

// ── Staleness label (shown when serving cached, not fresh, data) ─────────────

private fun stalenessLabel(cachedAt: Long?): String? {
    if (cachedAt == null) return null
    val minutes = ((System.currentTimeMillis() - cachedAt) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1  -> "Updated just now"
        minutes < 60 -> "Updated ${minutes}m ago"
        else         -> "Updated ${minutes / 60}h ago"
    }
}

// ── Weather icon (real vector glyph, not system emoji) ────────────────────────

// Renders the app's own WeatherGlyph vector icon (see WidgetRendering.kt) to a bitmap and shows
// it via Image — replaces the raw system emoji this widget used before, which render
// inconsistently (and often cheaply) across OEM launchers. remember() so a given (code, isDay,
// pixel size) combination is only rendered once per composition, not on every recomposition.
@Composable
private fun WidgetGlyph(code: Int, isDay: Boolean, size: Dp, modifier: GlanceModifier = GlanceModifier) {
    val density = LocalContext.current.resources.displayMetrics.density
    val sizePx = (size.value * density).toInt().coerceAtLeast(1)
    val bitmap = remember(code, isDay, sizePx) { renderGlyphBitmap(code, isDay, sizePx) }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = wmoText(code),
        modifier = modifier.size(size),
    )
}

// ── Small (2×1): temp + emoji only ───────────────────────────────────────────

@Composable
private fun SmallWidget(mod: GlanceModifier, data: WeatherData?, c: WColors) {
    Column(
        modifier = mod,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (data == null) {
            Text("—", style = TextStyle(color = c.textPrimary, fontSize = 18.sp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetGlyph(data.currentIcon, data.isDay, 20.dp)
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    "${data.currentTempC}°",
                    style = TextStyle(color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

// ── Medium (2×2): chrono-dynamic stack ───────────────────────────────────────

@Composable
private fun MediumWidget(mod: GlanceModifier, data: WeatherData?, time: TimeOfDay, c: WColors) {
    // fillMaxSize() + a defaultWeight() Spacer on both ends means real extra vertical space
    // (a host bigger than MEDIUM's 110x110 nominal size, but not tall enough to earn TALL) gets
    // distributed as flexible top/bottom breathing room instead of the content block sitting at
    // a fixed intrinsic size wherever the outer Box's centering happens to place it.
    Column(modifier = mod.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        if (data == null) {
            Text(
                "Open SkySpeak to set up",
                style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
            )
        } else {
            Spacer(GlanceModifier.defaultWeight())
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    widgetLocationName(data.locationName),
                    style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                RefreshButton(c)
            }
            if (data.alerts.isNotEmpty()) {
                Spacer(GlanceModifier.height(2.dp))
                AlertIndicator(data.alerts, c.isDark, c)
            }
            Spacer(GlanceModifier.height(3.dp))
            when (time) {
                TimeOfDay.MORNING -> MorningFocus(data, c)
                TimeOfDay.DAYTIME -> DaytimeFocus(data, c)
                TimeOfDay.NIGHT   -> NightFocus(data, c)
            }
            Spacer(GlanceModifier.defaultWeight())
        }
    }
}

// Shared by MediumWidget and TallWidget — tuned to fit MEDIUM's shrunk declared canvas (see
// MARGIN), the tighter of the two, so both stay clipping-free.

// Morning: today's high + rain chance — plan what to wear
@Composable
private fun MorningFocus(data: WeatherData, c: WColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WidgetGlyph(data.currentIcon, data.isDay, 13.dp)
        Spacer(GlanceModifier.width(3.dp))
        Text(
            data.condition,
            style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
            maxLines = 1,
        )
    }
    Spacer(GlanceModifier.height(2.dp))
    Text(
        "High ${data.highTodayC}°",
        style = TextStyle(color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold),
    )
    val rain = data.daily.firstOrNull()?.precipProbMax ?: 0
    if (rain > 0) {
        Spacer(GlanceModifier.height(2.dp))
        Text(
            "💧 $rain% chance of rain",
            style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
        )
    }
}

// Daytime: current temperature is the hero
@Composable
private fun DaytimeFocus(data: WeatherData, c: WColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WidgetGlyph(data.currentIcon, data.isDay, 22.dp)
        Spacer(GlanceModifier.width(3.dp))
        Text(
            "${data.currentTempC}°",
            style = TextStyle(color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold),
        )
    }
    Spacer(GlanceModifier.height(1.dp))
    Text(
        data.condition,
        style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
        maxLines = 1,
    )
    Spacer(GlanceModifier.height(2.dp))
    Text(
        "H:${data.highTodayC}°  L:${data.lowTodayC}°",
        style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
    )
}

// Night: tomorrow's forecast — wake up informed
@Composable
private fun NightFocus(data: WeatherData, c: WColors) {
    Text("Tomorrow", style = TextStyle(color = c.textSecondary, fontSize = 10.sp))
    Spacer(GlanceModifier.height(3.dp))
    val tomorrow = data.daily.getOrNull(1)
    if (tomorrow != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WidgetGlyph(tomorrow.icon, true, 13.dp)
            Spacer(GlanceModifier.width(3.dp))
            Text(
                wmoText(tomorrow.icon),
                style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(
            "H:${tomorrow.highC}°  L:${tomorrow.lowC}°",
            style = TextStyle(color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
    }
}

// ── Tall (2×4-ish): MEDIUM content + a vertical mini hourly list ─────────────
// Narrow placements (width < WIDE/LARGE's 250dp threshold) that are much taller than MEDIUM's
// 110dp would otherwise fall back to MEDIUM and leave a lot of unused vertical space (the
// screenshot-reported "spacing doesn't look right" case) — TALL keeps MEDIUM's width-appropriate
// content but adds a vertical hourly list to use the extra height instead of leaving it blank.

@Composable
private fun TallWidget(
    mod: GlanceModifier,
    data: WeatherData?,
    cachedAt: Long?,
    time: TimeOfDay,
    c: WColors,
) {
    // fillMaxSize() + defaultWeight() Spacers around the hourly list means real extra height
    // (beyond TALL's 220dp nominal) distributes as flexible gaps rather than sitting unused.
    Column(modifier = mod.fillMaxSize()) {
        if (data == null) {
            Text(
                "Open SkySpeak to set up",
                style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
            )
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    widgetLocationName(data.locationName),
                    style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                RefreshButton(c)
            }
            if (data.alerts.isNotEmpty()) {
                Spacer(GlanceModifier.height(2.dp))
                AlertIndicator(data.alerts, c.isDark, c)
            }
            Spacer(GlanceModifier.height(3.dp))
            when (time) {
                TimeOfDay.MORNING -> MorningFocus(data, c)
                TimeOfDay.DAYTIME -> DaytimeFocus(data, c)
                TimeOfDay.NIGHT   -> NightFocus(data, c)
            }
            val upcoming = data.hourly.drop(1).take(4)
            if (upcoming.isNotEmpty()) {
                Spacer(GlanceModifier.defaultWeight())
                Column(verticalAlignment = Alignment.CenterVertically) {
                    upcoming.forEach { entry ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                entry.hourLabel,
                                style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                                modifier = GlanceModifier.defaultWeight(),
                            )
                            WidgetGlyph(entry.icon, entry.isDay, 13.dp)
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                "${entry.tempC}°",
                                style = TextStyle(
                                    color = c.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            stalenessLabel(cachedAt)?.let {
                Text(it, style = TextStyle(color = c.textSecondary, fontSize = 8.sp))
            }
        }
    }
}

// ── Wide (4×1): current + compact hourly text ─────────────────────────────────

@Composable
private fun WideWidget(mod: GlanceModifier, data: WeatherData?, c: WColors) {
    // fillMaxSize() makes verticalAlignment actually do something — without it the Column was
    // wrap-content sized, so any extra height in a real host taller than WIDE's 50dp nominal
    // size was only ever handled by the outer Box's centering, not this Column's own alignment.
    Column(
        modifier = mod.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (data == null) {
            Text(
                "Open SkySpeak to set up",
                style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
            )
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetGlyph(data.currentIcon, data.isDay, 14.dp)
                Spacer(GlanceModifier.width(3.dp))
                Text(
                    "${data.currentTempC}°  ·  ${widgetLocationName(data.locationName)}",
                    style = TextStyle(color = c.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                RefreshButton(c)
            }
            Spacer(GlanceModifier.height(1.dp))
            // WIDE is too cramped for both an alert and the hourly line — an active alert is
            // more urgent than routine hourly conditions, so it wins the second line.
            if (data.alerts.isNotEmpty()) {
                AlertIndicator(data.alerts, c.isDark, c)
            } else {
                val upcoming = data.hourly.drop(1).take(4)
                if (upcoming.isNotEmpty()) {
                    // Same fix as HourlyStrip: fillMaxWidth() + defaultWeight() per entry so the
                    // row always divides the actual available width, instead of a fixed-spacer
                    // sequence that overflows the card and clips the last entry (confirmed via a
                    // real device screenshot — "7 PM" showing its icon but no temperature).
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        upcoming.forEach { entry ->
                            Row(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    entry.hourLabel,
                                    style = TextStyle(color = c.textSecondary, fontSize = 9.sp),
                                )
                                Spacer(GlanceModifier.width(2.dp))
                                WidgetGlyph(entry.icon, entry.isDay, 10.dp)
                                Spacer(GlanceModifier.width(2.dp))
                                Text(
                                    "${entry.tempC}°",
                                    style = TextStyle(color = c.textSecondary, fontSize = 9.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Large (4×2): chrono header + hourly strip ─────────────────────────────────

@Composable
private fun LargeWidget(
    mod: GlanceModifier,
    data: WeatherData?,
    cachedAt: Long?,
    time: TimeOfDay,
    c: WColors,
) {
    // fillMaxSize() + defaultWeight() Spacers around the hourly strip means real extra height
    // (beyond LARGE's 110dp nominal) distributes as flexible gaps instead of sitting unused
    // below a fixed-size block.
    Column(modifier = mod.fillMaxSize()) {
        if (data == null) {
            Text(
                "Open SkySpeak to set up",
                style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
            )
        } else {
            if (data.alerts.isNotEmpty()) {
                AlertIndicator(data.alerts, c.isDark, c)
                Spacer(GlanceModifier.height(2.dp))
            }
            LargeHeader(data, time, c)
            Spacer(GlanceModifier.defaultWeight())
            HourlyStrip(data.hourly.take(5), c)
            Spacer(GlanceModifier.defaultWeight())
            stalenessLabel(cachedAt)?.let {
                Text(it, style = TextStyle(color = c.textSecondary, fontSize = 8.sp))
            }
        }
    }
}

// ── XLarge (5×5-ish): header + alert + 7-hour strip ───────────────────────────

@Composable
private fun XLargeWidget(
    mod: GlanceModifier,
    data: WeatherData?,
    cachedAt: Long?,
    time: TimeOfDay,
    c: WColors,
) {
    // fillMaxSize() + defaultWeight() Spacers, same reasoning as LargeWidget — XLARGE's real
    // host is frequently much bigger than its 300x250dp nominal size.
    Column(modifier = mod.fillMaxSize()) {
        if (data == null) {
            Text(
                "Open SkySpeak to set up",
                style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
            )
        } else {
            if (data.alerts.isNotEmpty()) {
                AlertIndicator(data.alerts, c.isDark, c)
                Spacer(GlanceModifier.height(8.dp))
            }
            LargeHeader(data, time, c)
            Spacer(GlanceModifier.defaultWeight())
            // XLARGE is roomy enough for full detail rows (time · icon · temp · feels like ·
            // precip%) instead of LARGE's compact column strip — referencing another weather
            // app's widget design, user-requested, using this app's own wording ("Feels like",
            // not that app's trademarked term).
            Column {
                data.hourly.take(6).forEach { entry -> HourlyRow(entry, c) }
            }
            Spacer(GlanceModifier.defaultWeight())
            stalenessLabel(cachedAt)?.let {
                Text(it, style = TextStyle(color = c.textSecondary, fontSize = 9.sp))
            }
        }
    }
}

@Composable
private fun HourlyRow(entry: HourEntry, c: WColors) {
    // Fixed widths for the short, bounded-length fields (hour label, temp) plus a
    // defaultWeight() spacer to absorb whatever real width XLARGE's actual host has — same
    // overflow-safety reasoning as HourlyStrip's per-column weighting, just horizontal here.
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry.hourLabel,
            style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
            modifier = GlanceModifier.width(42.dp),
        )
        WidgetGlyph(entry.icon, entry.isDay, 16.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(
            "${entry.tempC}°",
            style = TextStyle(color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.width(38.dp),
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(
            "Feels like ${entry.feelsLikeC}°",
            style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
            maxLines = 1,
        )
        val precip = entry.precipChance
        if (precip != null && precip >= 10) {
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "💧$precip%",
                style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LargeHeader(data: WeatherData, time: TimeOfDay, c: WColors) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Left: location + current condition (always shown)
        Column(modifier = GlanceModifier.width(116.dp)) {
            Text(
                widgetLocationName(data.locationName),
                style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetGlyph(data.currentIcon, data.isDay, 12.dp)
                Spacer(GlanceModifier.width(3.dp))
                Text(
                    data.condition,
                    style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        // Right: chrono-dynamic key metric, plus the manual-refresh icon pinned to the far right
        // via this Row's own defaultWeight() + the inner Column's matching defaultWeight().
        Row(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Top) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                when (time) {
                    TimeOfDay.MORNING -> {
                        Text(
                            "High ${data.highTodayC}°",
                            style = TextStyle(color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        )
                        val rain = data.daily.firstOrNull()?.precipProbMax ?: 0
                        if (rain > 0) {
                            Text(
                                "💧 $rain% rain",
                                style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                            )
                        }
                    }
                    TimeOfDay.DAYTIME -> {
                        Text(
                            "${data.currentTempC}°",
                            style = TextStyle(color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        )
                        Text(
                            "H:${data.highTodayC}°  L:${data.lowTodayC}°",
                            style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                        )
                    }
                    TimeOfDay.NIGHT -> {
                        val tomorrow = data.daily.getOrNull(1)
                        Text("Tomorrow", style = TextStyle(color = c.textSecondary, fontSize = 10.sp))
                        if (tomorrow != null) {
                            Text(
                                "H:${tomorrow.highC}°  L:${tomorrow.lowC}°",
                                style = TextStyle(color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
            RefreshButton(c)
        }
    }
}

// ── Shared hourly strip ───────────────────────────────────────────────────────

@Composable
private fun HourlyStrip(hours: List<HourEntry>, c: WColors) {
    // Only used by LARGE (XLARGE has its own full-detail HourlyRow list instead). Columns use
    // defaultWeight() rather than a fixed width so the row always divides the actual available
    // width evenly regardless of column count — a fixed-width-per-column approach overflowed and
    // clipped the last column(s) against the widget's edge for any wider column count.
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        hours.forEach { entry ->
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    entry.hourLabel,
                    style = TextStyle(color = c.textSecondary, fontSize = 9.sp),
                    maxLines = 1,
                )
                WidgetGlyph(entry.icon, entry.isDay, 12.dp)
                Text(
                    "${entry.tempC}°",
                    style = TextStyle(color = c.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                )
                val precip = entry.precipChance
                if (precip != null && precip >= 20) {
                    Text(
                        "$precip%",
                        style = TextStyle(color = c.textSecondary, fontSize = 9.sp),
                    )
                }
            }
        }
    }
}
