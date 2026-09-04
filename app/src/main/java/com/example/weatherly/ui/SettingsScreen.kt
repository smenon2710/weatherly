package com.example.weatherly.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherly.BuildConfig
import com.example.weatherly.data.model.ThemePreference
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.ui.components.AppBackground
import com.example.weatherly.ui.components.Coral
import com.example.weatherly.ui.components.Cyan
import com.example.weatherly.ui.components.GlassCard
import com.example.weatherly.ui.components.TextPrimary
import com.example.weatherly.ui.components.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    units: UnitSystem,
    onUnitsChange: (UnitSystem) -> Unit,
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val hasOwnKey by settingsViewModel.hasOwnOpenRouterKey.collectAsStateWithLifecycle()
    val storedModel by settingsViewModel.openRouterModel.collectAsStateWithLifecycle()
    val widgetTransparent by settingsViewModel.widgetTransparent.collectAsStateWithLifecycle()
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsStateWithLifecycle()

    // The key field never holds the stored secret — only whatever new value the
    // user is about to save. See SettingsViewModel for why.
    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember(storedModel) { mutableStateOf(storedModel) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (saved) {
            delay(1500)
            saved = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
    ) {
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
            Text("Settings", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsSectionLabel("Appearance")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionPill(
                            label = "Light",
                            icon = Icons.Filled.LightMode,
                            selected = themePreference == ThemePreference.LIGHT,
                            onClick = { onThemePreferenceChange(ThemePreference.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        OptionPill(
                            label = "Dark",
                            icon = Icons.Filled.DarkMode,
                            selected = themePreference == ThemePreference.DARK,
                            onClick = { onThemePreferenceChange(ThemePreference.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        OptionPill(
                            label = "System",
                            icon = Icons.Filled.SettingsBrightness,
                            selected = themePreference == ThemePreference.SYSTEM,
                            onClick = { onThemePreferenceChange(ThemePreference.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsSectionLabel("Units")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionPill(
                            label = "°C · km/h",
                            icon = null,
                            selected = units == UnitSystem.METRIC,
                            onClick = { onUnitsChange(UnitSystem.METRIC) },
                            modifier = Modifier.weight(1f)
                        )
                        OptionPill(
                            label = "°F · mph",
                            icon = null,
                            selected = units == UnitSystem.IMPERIAL,
                            onClick = { onUnitsChange(UnitSystem.IMPERIAL) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsSectionLabel("Widget Background")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionPill(
                            label = "Opaque",
                            icon = null,
                            selected = !widgetTransparent,
                            onClick = { settingsViewModel.setWidgetTransparent(false) },
                            modifier = Modifier.weight(1f)
                        )
                        OptionPill(
                            label = "Transparent",
                            icon = null,
                            selected = widgetTransparent,
                            onClick = { settingsViewModel.setWidgetTransparent(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Transparent lets your wallpaper show through the widget. Text may be " +
                            "harder to read against busy wallpapers.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsSectionLabel("Haptic Feedback")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionPill(
                            label = "On",
                            icon = null,
                            selected = hapticsEnabled,
                            onClick = { settingsViewModel.setHapticsEnabled(true) },
                            modifier = Modifier.weight(1f)
                        )
                        OptionPill(
                            label = "Off",
                            icon = null,
                            selected = !hapticsEnabled,
                            onClick = { settingsViewModel.setHapticsEnabled(false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "A brief vibration when the forecast loads for a notable condition — an " +
                            "active severe alert, a thunderstorm, or heavy rain/snow. Ordinary " +
                            "weather stays silent.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsSectionLabel("AI Assistant")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("New OpenRouter API key") },
                        placeholder = { Text(if (hasOwnKey) "•••• (saved)" else "Not set") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (hasOwnKey) "A key is saved on this device. It can't be viewed here — enter a new one above to replace it."
                        else "Using the app's built-in key — AI chat works without one. Add your own above to pick a different model or use your own OpenRouter account.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (hasOwnKey) {
                        TextButton(onClick = { settingsViewModel.removeOpenRouterKey() }) {
                            Text("Remove saved key", color = Coral, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("Model (optional override)") },
                        singleLine = true,
                        enabled = hasOwnKey,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!hasOwnKey) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Add your own key above to change the model — this stays on the shared free model until then.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (keyInput.isNotBlank()) {
                                settingsViewModel.saveOpenRouterKey(keyInput)
                                keyInput = ""
                            }
                            if (hasOwnKey) settingsViewModel.saveOpenRouterModel(modelInput)
                            saved = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan)
                    ) {
                        Text(if (saved) "Saved" else "Save")
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsSectionLabel("About")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openPlayStoreListing(context) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Cyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Rate SkySpeak on Google Play", color = TextPrimary, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Version ${BuildConfig.VERSION_NAME}", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun OptionPill(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Cyan.copy(alpha = 0.16f) else Color.Transparent
    val fg = if (selected) Cyan else TextSecondary
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) Cyan.copy(alpha = 0.4f) else TextSecondary.copy(alpha = 0.15f), shape)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
        }
        Text(label, color = fg, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/**
 * Deep-links directly to the Play Store app (bypassing implicit-intent
 * resolution) and falls back to the browser listing when Play Store isn't
 * installed (e.g. some emulators) — Google's own guidance for a manual
 * "rate us" entry point is a direct Store listing link, not the In-App
 * Review API, which is reserved for auto-triggered post-interaction prompts.
 */
private fun openPlayStoreListing(context: Context) {
    val packageName = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                setPackage("com.android.vending")
            }
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        )
    }
}
