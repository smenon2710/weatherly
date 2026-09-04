package com.example.weatherly.widget

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.weatherly.ui.components.VividGlyphColors
import com.example.weatherly.ui.components.drawWeather
import com.example.weatherly.ui.components.glyphFor
import com.example.weatherly.ui.components.hasHail
import com.example.weatherly.ui.components.intensityFor
import android.graphics.Canvas as AndroidCanvas

/**
 * Renders the app's real [com.example.weatherly.ui.components.WeatherGlyph] vector icon set to a
 * plain [Bitmap], for use in the Glance widget (RemoteViews-based — it can't embed a live Compose
 * UI composable the way the in-app screens do). `drawWeather`/`glyphFor` are the same pure
 * DrawScope-extension functions the in-app icon uses (marked `internal` for this, not
 * duplicated), so the widget's icons are pixel-for-pixel the same shapes as the rest of the app
 * rather than a second, drifting icon set — this replaces the raw system emoji the widget used
 * before, which render inconsistently (and often cheaply) across OEM launchers.
 */
fun renderGlyphBitmap(code: Int, isDay: Boolean, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val composeCanvas = Canvas(AndroidCanvas(bitmap))
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(sizePx.toFloat(), sizePx.toFloat()),
    ) {
        val center = Offset(sizePx / 2f, sizePx / 2f)
        // VividGlyphColors, not the in-app muted palette — a deliberate widget-only choice
        // (user-requested, referencing another weather app's more saturated widget icons).
        drawWeather(
            glyphFor(code, isDay), center, sizePx.toFloat(), VividGlyphColors,
            intensity = intensityFor(code), hasHail = hasHail(code)
        )
    }
    return bitmap
}

/**
 * Renders a small vertical 2-stop gradient bitmap — [topColor] (the condition's sky tone, from
 * the shared [com.example.weatherly.util.skyColor], same source of truth as the in-app hero) to
 * [bottomColor] (the app's own light/dark background, since Glance has no MaterialTheme to read
 * from) — set as the widget's background via `ImageProvider(bitmap)` with `ContentScale.FillBounds`
 * so it stretches to whatever the actual widget frame turns out to be. A small bitmap (not a
 * per-pixel-exact one matching the real widget size, which Glance can't report anyway in
 * Responsive mode) is intentional — a vertical gradient scales losslessly.
 */
fun renderGradientBitmap(topColor: Color, bottomColor: Color, widthPx: Int, heightPx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, heightPx.toFloat(),
            topColor.toArgb(), bottomColor.toArgb(),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), paint)
    return bitmap
}
