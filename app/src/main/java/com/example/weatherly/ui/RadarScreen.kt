package com.example.weatherly.ui

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.weatherly.ui.components.AppBackground
import com.example.weatherly.ui.components.Cyan
import com.example.weatherly.ui.components.TextPrimary
import com.example.weatherly.ui.components.TextSecondary
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── RainViewer API models ─────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
private data class RainViewerResponse(
    val version: String = "",
    val generated: Long = 0,
    val host: String = "",
    val radar: RadarFrames = RadarFrames()
)

@JsonClass(generateAdapter = true)
private data class RadarFrames(
    val past: List<RadarFrame> = emptyList(),
    val nowcast: List<RadarFrame> = emptyList()
)

@JsonClass(generateAdapter = true)
private data class RadarFrame(
    val time: Long = 0,
    val path: String = ""
)

// ── Radar tile source ─────────────────────────────────────────────────────────

private fun radarTileSource(host: String, path: String): OnlineTileSourceBase =
    object : OnlineTileSourceBase("RainViewer", 1, 12, 256, ".png", arrayOf(host)) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$host$path/256/$z/$x/$y/2/1_1.png"
        }
    }

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun RadarScreen(
    lat: Double,
    lon: Double,
    locationName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // RainViewer state
    var frames by remember { mutableStateOf<List<RadarFrame>>(emptyList()) }
    var host by remember { mutableStateOf("https://tilecache.rainviewer.com") }
    var frameIndex by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    // Map reference for overlay swapping
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentOverlay by remember { mutableStateOf<TilesOverlay?>(null) }

    // OSMDroid needs an app context for its config
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Fetch radar frames from RainViewer
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val req = Request.Builder()
                    .url("https://api.rainviewer.com/public/weather-maps.json")
                    .build()
                val body = client.newCall(req).execute().use { it.body?.string() }
                if (body != null) {
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val rv = moshi.adapter(RainViewerResponse::class.java).fromJson(body)
                    if (rv != null) {
                        host = rv.host
                        frames = rv.radar.past.takeLast(10)
                        frameIndex = frames.lastIndex.coerceAtLeast(0)
                    }
                }
            } catch (_: Exception) { }
            loading = false
        }
    }

    // Swap radar overlay when frame changes
    LaunchedEffect(frameIndex, frames, mapView) {
        val mv = mapView ?: return@LaunchedEffect
        val allFrames = frames
        if (allFrames.isEmpty()) return@LaunchedEffect
        val frame = allFrames.getOrNull(frameIndex) ?: return@LaunchedEffect
        val src = radarTileSource(host, frame.path)
        val overlay = TilesOverlay(
            org.osmdroid.tileprovider.MapTileProviderBasic(context, src), context
        ).apply { loadingBackgroundColor = android.graphics.Color.TRANSPARENT }
        withContext(Dispatchers.Main) {
            currentOverlay?.let { mv.overlays.remove(it) }
            mv.overlays.add(overlay)
            currentOverlay = overlay
            mv.invalidate()
        }
    }

    // Auto-play loop
    LaunchedEffect(playing, frames.size) {
        if (playing && frames.isNotEmpty()) {
            while (playing) {
                delay(600)
                frameIndex = (frameIndex + 1) % frames.size
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Radar", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(locationName, color = TextSecondary, fontSize = 12.sp)
            }
        }

        // Map
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(8.0)
                        controller.setCenter(GeoPoint(lat, lon))
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (loading) {
                CircularProgressIndicator(
                    color = Cyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Frame timestamp badge
            if (frames.isNotEmpty()) {
                val ts = frames.getOrNull(frameIndex)?.time ?: 0L
                val label = remember(ts) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts * 1000))
                }
                val badgeShape = RoundedCornerShape(14.dp)
                val badgeStroke = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(badgeShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, badgeStroke, badgeShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Playback controls
        if (frames.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { playing = !playing },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Cyan)
                    ) {
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        value = frameIndex.toFloat(),
                        onValueChange = { playing = false; frameIndex = it.toInt() },
                        valueRange = 0f..(frames.lastIndex.toFloat().coerceAtLeast(0f)),
                        steps = (frames.size - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = Cyan,
                            activeTrackColor = Cyan,
                            inactiveTrackColor = TextSecondary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        frames.firstOrNull()?.let {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.time * 1000))
                        } ?: "",
                        color = TextSecondary, fontSize = 10.sp
                    )
                    Text("past 10 frames · RainViewer", color = TextSecondary, fontSize = 10.sp)
                    Text(
                        frames.lastOrNull()?.let {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.time * 1000))
                        } ?: "",
                        color = TextSecondary, fontSize = 10.sp
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView?.onDetach() }
    }
}
