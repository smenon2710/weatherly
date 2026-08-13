package com.example.weatherly.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Debug-only QA harness: hosts [WeatherWidget] itself (bypassing the launcher's drag/drop widget
 * picker, which has proven unreliable to drive via adb in this environment) and renders each
 * declared size breakpoint — plus a couple of "real host bigger than the declared breakpoint"
 * cases the widget's own code comments flag as unverified — to PNG files under
 * getExternalFilesDir/widget_qa for a real pixel-level visual check. Launch with:
 *   adb shell am start -n com.example.weatherly/.widget.WidgetQaActivity
 * Never included in release builds (this whole source set is debug-only).
 */
class WidgetQaActivity : Activity() {

    private lateinit var host: AppWidgetHost
    private lateinit var root: FrameLayout
    private lateinit var hostView: AppWidgetHostView
    private var appWidgetId: Int = -1

    private data class Tier(
        val name: String,
        val optW: Int, val optH: Int, // reported via AppWidgetManager options (dp)
        val realW: Int, val realH: Int, // actual on-screen host frame (dp)
    )

    // Real host frames fixed at each breakpoint's ORIGINAL (pre-margin-fix) target dp size — the
    // exact sizes that reproduced the downgrade bug before WeatherWidget.kt's MARGIN constant was
    // added. If the margin fix works, each of these should now render its own intended layout
    // instead of falling back to a smaller one.
    private val tiers = listOf(
        Tier("1_small", 110, 50, 110, 50),
        Tier("2_medium", 110, 110, 110, 110),
        Tier("3_tall", 110, 220, 110, 220),
        Tier("4_wide", 250, 50, 250, 50),
        Tier("5_large", 250, 110, 250, 110),
        Tier("6_xlarge", 300, 250, 300, 250),
        // Real host frame bigger than the matched breakpoint's nominal size.
        Tier("7_xlarge_oversized_host", 300, 250, 400, 340),
        Tier("8_tall_oversized_host", 110, 220, 140, 340),
        Tier("9_medium_oversized_host", 110, 110, 160, 170),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        setContentView(root)

        host = AppWidgetHost(this, 20260813)
        host.startListening()

        val mgr = AppWidgetManager.getInstance(this)
        appWidgetId = host.allocateAppWidgetId()
        val provider = ComponentName(this, WeatherWidgetReceiver::class.java)
        val bound = mgr.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        Log.i(TAG, "bindAppWidgetIdIfAllowed=$bound appWidgetId=$appWidgetId")
        if (!bound) {
            Log.e(TAG, "QA_ABORT bind failed, needs user consent")
            finish()
            return
        }

        val info = mgr.getAppWidgetInfo(appWidgetId)
        hostView = host.createView(this, appWidgetId, info)
        root.addView(hostView)

        CoroutineScope(Dispatchers.Main).launch {
            tiers.forEachIndexed { i, tier -> captureTier(mgr, tier, first = i == 0) }
            Log.i(TAG, "QA_DONE")
            finish()
        }
    }

    private suspend fun captureTier(mgr: AppWidgetManager, tier: Tier, first: Boolean) {
        val density = resources.displayMetrics.density
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, tier.optW)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, tier.optH)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, tier.optW)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, tier.optH)
        }
        mgr.updateAppWidgetOptions(appWidgetId, options)

        // ceil, not toInt() (which truncates) — a floored px size converts back to a hair under
        // the requested dp value, which was enough to make Glance's Responsive matching miss an
        // exact-size breakpoint and silently fall back to the next one down.
        val realWpx = kotlin.math.ceil(tier.realW * density).toInt()
        val realHpx = kotlin.math.ceil(tier.realH * density).toInt()
        hostView.layoutParams = FrameLayout.LayoutParams(realWpx, realHpx)
        hostView.requestLayout()

        // Only the first (cold-cache) tier needs the long wait now that WeatherWidget shares one
        // WeatherRepository across provideGlance() calls — later tiers should hit that repo's
        // 30-minute cache and resolve quickly. A short delay here is deliberate: it's the test
        // for whether the cache-reuse fix actually worked.
        delay(if (first) 40000 else 6000)

        root.measure(
            View.MeasureSpec.makeMeasureSpec(realWpx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(realHpx, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, realWpx, realHpx)

        val w = realWpx.coerceAtLeast(1)
        val h = realHpx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        hostView.draw(canvas)

        val dir = File(getExternalFilesDir(null), "widget_qa").apply { mkdirs() }
        val file = File(dir, "${tier.name}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        Log.i(TAG, "QA_SAVED ${file.absolutePath} opt=${tier.optW}x${tier.optH} real=${tier.realW}x${tier.realH}dp (${w}x${h}px)")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::host.isInitialized) {
            host.stopListening()
            if (appWidgetId != -1) host.deleteAppWidgetId(appWidgetId)
        }
    }

    companion object {
        private const val TAG = "WidgetQa"
    }
}
