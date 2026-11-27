package com.example.carlauncher

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.delay

/* ---------------- STATE ---------------- */

@Stable
class LauncherState(val prefs: SharedPreferences) {
    var carId by mutableStateOf(prefs.getString("car", "orange") ?: "orange"); private set
    var ownerName by mutableStateOf(prefs.getString("owner_name", "Demo") ?: "Demo"); private set
    var night by mutableStateOf(prefs.getBoolean("night", true)); private set
    var styleId by mutableStateOf(prefs.getString("style", "demo") ?: "demo"); private set
    var animating by mutableStateOf(prefs.getBoolean("animating", true)); private set

    var showSettings by mutableStateOf(false); private set
    var showApps by mutableStateOf(false); private set
    var pickRadio by mutableStateOf(false); private set

    fun toggleNight() { night = !night; prefs.edit { putBoolean("night", night) } }
    fun toggleAnimating() { animating = !animating; prefs.edit { putBoolean("animating", animating) } }
    fun setCar(id: String) { carId = id; prefs.edit { putString("car", id) } }
    fun setStyle(id: String) { styleId = id; prefs.edit { putString("style", id) } }
    fun updateOwnerName(name: String) { ownerName = name; prefs.edit { putString("owner_name", name) } }
    fun saveRadioPackage(pkg: String) { prefs.edit { putString("radio_pkg", pkg) } }

    fun toggleSettings() { showSettings = !showSettings }
    fun closeSettings() { showSettings = false }
    fun openApps(pickRadio: Boolean = false) { this.pickRadio = pickRadio; showApps = true }
    fun closeOverlays() { showApps = false; showSettings = false; pickRadio = false }
}

@Composable
fun rememberLauncherState(): LauncherState {
    val ctx = LocalContext.current
    return remember { LauncherState(ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)) }
}

/* ---------------- HOST ---------------- */

private val FrameShape = RoundedCornerShape(28.dp)

@Composable
fun LauncherApp() {
    val ctx = LocalContext.current
    val state = rememberLauncherState()

    val style = DashboardStyles.byId(state.styleId)
    val theme = remember(state.carId, state.night) {
        buildTheme(CarColors.firstOrNull { it.id == state.carId } ?: CarColors.first(), state.night)
    }
    // Background modifier is rebuilt only when the style or colours change.
    val background = remember(style, theme) { style.background(theme) }

    val telemetry = remember { Telemetry(ctx) }
    val media = remember { MediaState(ctx) }
    val env = remember {
        DashboardEnv(
            state = state,
            telemetry = telemetry,
            media = media,
            onRadio = {
                if (!launchRadio(ctx, state.prefs)) {
                    Toast.makeText(ctx, "Radio app not found. Please choose it from the list.", Toast.LENGTH_LONG).show()
                    state.openApps(pickRadio = true)
                }
            },
            onRadioLong = { state.openApps(pickRadio = true) },
            onApps = { state.openApps() },
            onSettings = state::toggleSettings
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) telemetry.start() }

    LaunchedEffect(Unit) {
        permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        media.start()
    }
    DisposableEffect(Unit) { onDispose { telemetry.stop(); media.stop() } }
    LaunchedEffect(Unit) {
        while (true) { media.tick(); telemetry.checkStale(); delay(1000) }
    }

    // Weather: retry every 15 s until it works, then every 30 min. Last good values stay on failure.
    LaunchedEffect(Unit) {
        while (true) {
            val result = fetchWeather(WEATHER_LAT, WEATHER_LON)
            if (result is WeatherState.Ok || env.weather !is WeatherState.Ok) env.weather = result
            delay(if (result is WeatherState.Ok) 30 * 60 * 1000L else 15_000L)
        }
    }

    BackHandler(enabled = state.showApps || state.showSettings) { state.closeOverlays() }

    Box(Modifier.fillMaxSize().background(Color.Black).padding(10.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .shadow(24.dp, FrameShape, ambientColor = theme.glow, spotColor = theme.glow)
                .clip(FrameShape)
                .background(theme.background)
                .then(background)                       // <- style specific background
                .border(BorderStroke(2.dp, theme.glow), FrameShape)
                .padding(18.dp)
        ) {
            if (state.showApps) {
                AppDrawer(
                    theme = theme,
                    title = if (state.pickRadio) "Choose your radio app" else "All apps",
                    onPick = if (state.pickRadio) { pkg ->
                        state.saveRadioPackage(pkg)
                        state.closeOverlays()
                        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startSafely(it) }
                    } else null,
                    onClose = state::closeOverlays
                )
            } else {
                style.Content(theme, env)               // <- style specific layout

                if (state.showSettings) {
                    SettingsPopup(
                        theme = theme,
                        state = state,
                        onClose = state::closeSettings,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 10.dp)
                    )
                }
            }
        }
    }
}