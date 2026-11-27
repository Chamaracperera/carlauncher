package com.example.carlauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun LauncherApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE) }

    var carId by remember { mutableStateOf(prefs.getString("car", "orange") ?: "orange") }
    var ownerName by remember { mutableStateOf(prefs.getString("owner_name", "Demo") ?: "Demo") }
    var night by remember { mutableStateOf(prefs.getBoolean("night", true)) }
    var style by remember { mutableStateOf(prefs.getString("style", "demo") ?: "demo") }
    var showSettings by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }
    var pickRadio by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(prefs.getBoolean("animating", true)) }

    val car = CarColors.firstOrNull { it.id == carId } ?: CarColors.first()
    val theme = remember(carId, night) { buildTheme(car, night) }

    val telemetry = remember { Telemetry(ctx) }
    val media = remember { MediaState(ctx) }
    var weather by remember { mutableStateOf<WeatherState>(WeatherState.Loading) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) telemetry.start() }

    LaunchedEffect(Unit) {
        permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        media.start()
    }
    DisposableEffect(Unit) {
        onDispose { telemetry.stop(); media.stop() }
    }
    LaunchedEffect(Unit) {
        while (true) { media.tick(); telemetry.checkStale(); delay(1000) }
    }

    // Weather: retry every 15 s until it works, then refresh every 30 min.
    // If a later refresh fails, the last good values stay on screen.
    LaunchedEffect(Unit) {
        while (true) {
            val result = fetchWeather(WEATHER_LAT, WEATHER_LON)
            if (result is WeatherState.Ok || weather !is WeatherState.Ok) weather = result
            delay(if (result is WeatherState.Ok) 30 * 60 * 1000L else 15_000L)
        }
    }

    BackHandler(enabled = showApps || showSettings) {
        showApps = false
        showSettings = false
        pickRadio = false
    }

    // Opens the radio app. If none is found, lets the user choose one.
    val openRadio: () -> Unit = {
        if (!launchRadio(ctx, prefs)) {
            Toast.makeText(ctx, "Radio app not found. Please choose it from the list.", Toast.LENGTH_LONG).show()
            pickRadio = true
            showApps = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = theme.glow, spotColor = theme.glow)
                .clip(RoundedCornerShape(28.dp))
                .background(theme.background)
                .dashboardBackground(theme, night)
                .border(BorderStroke(2.dp, theme.glow), RoundedCornerShape(28.dp))
                .padding(18.dp)
        ) {
            if (showApps) {
                AppDrawer(
                    theme = theme,
                    title = if (pickRadio) "Choose your radio app" else "All apps",
                    onPick = if (pickRadio) { pkg ->
                        prefs.edit().putString("radio_pkg", pkg).apply()
                        pickRadio = false
                        showApps = false
                        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startSafely(it) }
                    } else null,
                    onClose = { showApps = false; pickRadio = false }
                )
            } else {
                if (style == "drive") {
                    DriveHome(
                        theme = theme,
                        telemetry = telemetry,
                        weather = weather,
                        animating = animating,
                        onAnimating = {
                            animating = !animating
                            prefs.edit().putBoolean("animating", animating).apply()
                        },
                        onRadio = openRadio,
                        onRadioLong = { pickRadio = true; showApps = true },
                        onTheme = {
                            night = !night
                            prefs.edit().putBoolean("night", night).apply()
                        },
                        onSettings = { showSettings = !showSettings },
                        onApps = { pickRadio = false; showApps = true }
                    )
                } else {
                    Column(Modifier.fillMaxSize()) {
                        TopBar(
                            theme,
                            animating = animating,
                            ownerName = ownerName,
                            onAnimating = {
                                animating = !animating
                                prefs.edit().putBoolean("animating", animating).apply()
                            },
                            onTheme = {
                                night = !night
                                prefs.edit().putBoolean("night", night).apply()
                            },
                            onSettings = { showSettings = !showSettings }
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            SpeedCard(theme, telemetry, Modifier.weight(0.9f).fillMaxHeight())
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(2.2f).fillMaxHeight()) {
                                Row(Modifier.fillMaxWidth().height(110.dp)) {
                                    MusicCard(theme, media, Modifier.weight(1.5f).fillMaxHeight())
                                    Spacer(Modifier.width(14.dp))
                                    WeatherCard(theme, weather, Modifier.weight(1f).fillMaxHeight())
                                }
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                    CarImage(
                                        theme,
                                        Modifier.fillMaxWidth().fillMaxHeight(0.75f),   // 75% of the old size
                                        lightsOn = night,
                                        animating = animating,
                                        gpsSpeed = telemetry.speed
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Dock(
                            theme,
                            onApps = { pickRadio = false; showApps = true },
                            onRadio = openRadio,
                            onRadioLong = { pickRadio = true; showApps = true }
                        )
                    }
                }

                if (showSettings) {
                    SettingsPopup(
                        theme = theme,
                        selectedCarId = carId,
                        ownerName = ownerName,
                        style = style,
                        onStyleSelected = { s ->
                            style = s
                            prefs.edit().putString("style", s).apply()
                        },
                        onCarSelected = { id ->
                            carId = id
                            prefs.edit().putString("car", id).apply()
                        },
                        onOwnerNameChange = { name ->
                            ownerName = name
                            prefs.edit().putString("owner_name", name).apply()
                        },
                        onClose = { showSettings = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 10.dp)
                    )
                }
            }
        }
    }
}

/* ---------------- TOP BAR ---------------- */

@Composable
fun TopBar(
    theme: CarTheme,
    animating: Boolean,
    ownerName: String,
    onAnimating: () -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }

    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH) }
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.ENGLISH) }
    val meridiemFmt = remember { SimpleDateFormat("a", Locale.ENGLISH) }

    val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    val baseGreeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val greeting = if (ownerName.isNotBlank()) "$baseGreeting, $ownerName" else baseGreeting
    val date = dateFmt.format(Date(now))
    val time = timeFmt.format(Date(now))
    val meridiem = meridiemFmt.format(Date(now))

    Box(Modifier.fillMaxWidth().height(56.dp)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Home, null, tint = theme.subText, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(greeting, color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(date, color = theme.subText, fontSize = 12.sp)
            }
        }
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.Bottom) {
            Text(time, color = theme.text, fontSize = 40.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(4.dp))
            Text(meridiem, color = theme.subText, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            PillButton(theme, onClick = onAnimating) {
                Icon(
                    if (animating) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null,
                    tint = if (animating) theme.accent else theme.subText,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            PillButton(theme, onClick = onSettings) {
                Icon(Icons.Filled.Settings, null, tint = theme.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            PillButton(theme, wide = true, onClick = onTheme) {
                Icon(Icons.Filled.NightsStay, null, tint = theme.text, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(theme.name, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PillButton(
    theme: CarTheme,
    wide: Boolean = false,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier
            .height(46.dp)
            .then(if (wide) Modifier.widthIn(min = 96.dp) else Modifier.width(54.dp))
            .clip(RoundedCornerShape(23.dp))
            .background(theme.card)
            .border(1.dp, theme.track, RoundedCornerShape(23.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

/* ---------------- SETTINGS POPUP ---------------- */

@Composable
fun SettingsPopup(
    theme: CarTheme,
    selectedCarId: String,
    ownerName: String,
    style: String,
    onStyleSelected: (String) -> Unit,
    onCarSelected: (String) -> Unit,
    onOwnerNameChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(theme.card)
            .border(1.dp, theme.track, RoundedCornerShape(24.dp))
            .width(340.dp)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Settings, null, tint = theme.accent, modifier = Modifier.size(20.dp))
                    Text("Settings", color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = theme.subText,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                )
            }

            // Section 1: Pick a theme (horizontal list)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("1. Pick a Theme", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple("demo", "Default", "Classic dashboard"),
                        Triple("drive", "Cockpit", "Big speedo & map")
                    ).forEach { (id, name, desc) ->
                        val sel = style == id
                        val icon = if (id == "demo") Icons.Filled.Dashboard else Icons.Filled.Speed
                        Column(
                            Modifier
                                .width(140.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) theme.accent.copy(alpha = 0.12f) else theme.background)
                                .border(
                                    if (sel) 2.dp else 1.dp,
                                    if (sel) theme.accent else theme.track,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onStyleSelected(id) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null, tint = if (sel) theme.accent else theme.text, modifier = Modifier.size(22.dp))
                                if (sel) Icon(Icons.Filled.CheckCircle, null, tint = theme.accent, modifier = Modifier.size(16.dp))
                            }
                            Text(name, color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = theme.subText, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Section 2: Vehicle colour
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("2. Vehicle Colour", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CarColors.forEach { c ->
                        val selected = c.id == selectedCarId
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onCarSelected(c.id) }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c.accent)
                                    .border(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) theme.text else theme.track,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                c.label,
                                color = if (selected) theme.text else theme.subText,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Section 3: Owner Name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("3. Owner Name", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.OutlinedTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Enter owner name", color = theme.subText) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = theme.text, fontSize = 14.sp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.track,
                        focusedTextColor = theme.text,
                        unfocusedTextColor = theme.text,
                        cursorColor = theme.accent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}


/* ---------------- SPEED CARD ---------------- */

@Composable
fun CardBox(theme: CarTheme, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(theme.card)
            .border(1.dp, theme.track.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun SpeedCard(theme: CarTheme, t: Telemetry, modifier: Modifier) {
    CardBox(theme, modifier) {
        Column(Modifier.fillMaxSize()) {
            Text("SPEED", color = theme.subText, fontSize = 12.sp, letterSpacing = 1.5.sp)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Gauge(theme, t.speed, Modifier.fillMaxHeight().aspectRatio(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${t.speed}", color = theme.text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    Text("KM/H", color = theme.subText, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(theme, "TRIP", "${"%.0f".format(t.tripKm)} km")
                Stat(theme, "DRIVE", "${t.driveMin} min")
                Stat(theme, "HEADING", t.heading)
            }
        }
    }
}

@Composable
fun Stat(theme: CarTheme, label: String, value: String) {
    Column {
        Text(label, color = theme.subText, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = theme.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Gauge(theme: CarTheme, speed: Int, modifier: Modifier, max: Int = 180) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val segments = 40
            val start = 135f
            val sweep = 270f
            val per = sweep / segments
            val active = ((speed.coerceIn(0, max).toFloat() / max) * segments).toInt()
            for (i in 0 until segments) {
                val grow = 0.55f + 0.45f * (i / segments.toFloat())
                val w = stroke * grow
                val inset = stroke / 2 + (stroke - w) / 2
                drawArc(
                    color = if (i < active) theme.accent else theme.track,
                    startAngle = start + i * per,
                    sweepAngle = per - 2.2f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(width = w)
                )
            }
        }
        val lbl = theme.subText
        Text("0", color = lbl, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 22.dp))
        Text("60", color = lbl, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 34.dp))
        Text("120", color = lbl, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 22.dp, top = 34.dp))
        Text("180", color = lbl, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 22.dp))
    }
}

/* ---------------- MUSIC CARD ---------------- */

@Composable
fun MusicCard(theme: CarTheme, m: MediaState, modifier: Modifier) {
    val ctx = LocalContext.current
    CardBox(
        theme,
        modifier.background(
            Brush.horizontalGradient(listOf(theme.accent.copy(alpha = 0.2f), Color.Transparent)),
            RoundedCornerShape(18.dp)
        )
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF6B4FA0), Color(0xFFFF9A3C)))),
                contentAlignment = Alignment.Center
            ) {
                val art = m.art
                if (art != null) {
                    Image(art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(
                Modifier
                    .weight(1f)
                    .clickable(enabled = !m.access) { askNotificationAccess(ctx) }
            ) {
                Text("MUSIC", color = theme.accent, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text(m.title, color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(m.artist, color = theme.subText, fontSize = 12.sp, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                val frac = if (m.duration > 0) (m.position.toFloat() / m.duration).coerceIn(0f, 1f) else 0f
                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(theme.track)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(theme.accent))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt(m.position), color = theme.subText, fontSize = 10.sp)
                    Text(fmt(m.duration), color = theme.subText, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SkipPrevious, null, tint = theme.text,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { m.previous() })
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(52.dp)
                        .border(2.dp, theme.accent, CircleShape)
                        .clip(CircleShape)
                        .clickable { m.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (m.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = theme.accent, modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Filled.SkipNext, null, tint = theme.text,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { m.next() })
            }
        }
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun askNotificationAccess(ctx: Context) {
    ctx.startSafely(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
}

/* ---------------- WEATHER CARD ---------------- */

@Composable
fun WeatherCard(theme: CarTheme, state: WeatherState, modifier: Modifier) {
    val temps = (state as? WeatherState.Ok)?.data?.let { "${it.high}° / ${it.low}°" } ?: "--° / --°"
    val info = when (state) {
        is WeatherState.Ok ->
            "Now ${state.data.now}°  •  ${state.data.condition}  •  Sunset ${state.data.sunset}"
        is WeatherState.Error -> "${state.message}  •  retrying…"
        WeatherState.Loading -> "Loading…"
    }
    CardBox(theme, modifier) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WEATHER", color = theme.subText, fontSize = 11.sp, letterSpacing = 1.5.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = theme.text, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(WEATHER_CITY, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Text(temps, color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(info, color = theme.subText, fontSize = 10.sp, maxLines = 1)
            }
            Icon(Icons.Filled.WbSunny, null, tint = Color(0xFFFFB300), modifier = Modifier.size(40.dp))
        }
    }
}

/* ---------------- CAR IMAGE ---------------- */

// Light positions as a fraction of the car image (0.0 to 1.0). Tune to your PNG.
private const val HEAD_X = 0.94f
private const val HEAD_Y = 0.50f
private const val TAIL_X = 0.06f
private const val TAIL_Y = 0.48f

// Rear wheel position, where the dust comes from
private const val WHEEL_X = 0.22f
private const val GROUND_Y = 0.92f

private data class Streak(
    val y: Float, val len: Float, val speed: Float,
    val phase: Float, val thick: Float, val alpha: Float
)

@Composable
fun CarImage(
    theme: CarTheme,
    modifier: Modifier,
    lightsOn: Boolean = true,
    animating: Boolean = true,
    gpsSpeed: Int = 0
) {
    // -1.05 = just off-screen left, 0 = middle, +1.05 = just off-screen right
    val offset = remember { Animatable(-1.05f) }
    var shownImage by remember { mutableIntStateOf(theme.carImage) }
    var wasOff by remember { mutableStateOf(false) }
    var wasPaused by remember { mutableStateOf(false) }

    val accelerate = FastOutSlowInEasing

    LaunchedEffect(theme.carImage, animating) {
        if (!animating) {
            wasPaused = true
            wasOff = false
            shownImage = theme.carImage
            offset.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
            return@LaunchedEffect
        }
        if (wasPaused) {
            offset.snapTo(-1.05f)
            wasPaused = false
            shownImage = theme.carImage
            offset.animateTo(0f, tween(1900, easing = FastOutSlowInEasing))
            return@LaunchedEffect
        }
        if (wasOff) {
            offset.snapTo(-1.05f)
            wasOff = false
        } else if (shownImage != theme.carImage) {
            offset.animateTo(1.05f, tween(900, easing = accelerate))
            offset.snapTo(-1.05f)
        }
        shownImage = theme.carImage
        offset.animateTo(0f, tween(1900, easing = FastOutSlowInEasing))
    }

    // Smoothed speed: follows the speedometer, but eases up and down
    // so the wind builds up and dies away gradually.
    val smoothSpeed = animateFloatAsState(
        targetValue = if (animating) gpsSpeed.toFloat() else 0f,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "windSpeed"
    )

    // 0..1 wind strength from speed (140 km/h and above = maximum)
    val windRaw: () -> Float = {
        if (!animating) 0f else {
            val s = smoothSpeed.value
            if (s < 0.5f) 0f else (s / 140f).coerceIn(0f, 1f)
        }
    }
    // Same value on a curve that makes low speeds easier to see
    val windVisual: () -> Float = { sqrt(windRaw()) }

    // Animation clock: its speed is proportional to the real speed.
    // Speed 0 means the clock stops and everything freezes.
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    phase += dt * 2.6f * windRaw()
                }
                last = now
            }
        }
    }

    val streaks = remember {
        val r = Random(7)
        List(28) {
            Streak(
                y = r.nextFloat(),
                len = 0.08f + r.nextFloat() * 0.22f,
                speed = 0.6f + r.nextFloat() * 0.9f,
                phase = r.nextFloat(),
                thick = 1f + r.nextFloat() * 1.8f,
                alpha = 0.25f + r.nextFloat() * 0.5f
            )
        }
    }

    val lightsLevel by animateFloatAsState(
        targetValue = if (lightsOn) 1f else 0f,
        animationSpec = tween(700),
        label = "lightsLevel"
    )

    val pulse by rememberInfiniteTransition(label = "lights").animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val painter = painterResource(shownImage)
    val ratio = painter.intrinsicSize.let {
        if (it.width > 0f && it.height > 0f) it.width / it.height else 2.2f
    }
    val windColor = if (theme.name == "NIGHT") Color.White else theme.subText

    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.BottomCenter) {
        val parentPx = constraints.maxWidth.toFloat()

        /* ---------- Wind streaks (behind the car) ---------- */
        Canvas(Modifier.fillMaxSize()) {
            val k = windVisual()
            if (k <= 0.02f) return@Canvas
            val w = size.width
            val h = size.height
            streaks.forEach { s ->
                val len = s.len * w * (0.6f + k)
                val p = (((s.phase - phase * s.speed) % 1f) + 1f) % 1f
                val x = p * (w + len) - len
                val y = h * (0.08f + 0.78f * s.y)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            windColor.copy(alpha = s.alpha * k * 0.8f),
                            Color.Transparent
                        ),
                        startX = x,
                        endX = x + len
                    ),
                    start = Offset(x, y),
                    end = Offset(x + len, y),
                    strokeWidth = s.thick,
                    cap = StrokeCap.Round
                )
            }
        }

        /* ---------- Floor glow ---------- */
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .height(3.dp)
                .graphicsLayer { alpha = (1f - abs(offset.value)).coerceIn(0f, 1f) }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, theme.accent, Color.Transparent)
                    )
                )
        )

        /* ---------- The car (moves as one unit) ---------- */
        Box(
            Modifier
                .padding(bottom = 6.dp)
                .fillMaxHeight()
                .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                .graphicsLayer {
                    translationX = offset.value * (parentPx / 2f + size.width / 2f)
                    // road bob only while the car is really moving
                    translationY = sin(phase * 22f) * windRaw() * 2.5.dp.toPx()
                }
        ) {
            // ground shadow and dust
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(w * 0.08f, h * 0.88f),
                    size = Size(w * 0.84f, h * 0.10f)
                )
                val k = windVisual()
                if (k > 0.05f) {
                    for (i in 0 until 7) {
                        val life = (phase * 1.4f + i / 7f) % 1f
                        drawCircle(
                            color = Color(0xFFC9C2B8).copy(alpha = (1f - life) * 0.30f * k),
                            radius = h * (0.025f + 0.07f * life),
                            center = Offset(
                                w * WHEEL_X - life * w * 0.22f,
                                h * GROUND_Y - life * h * 0.10f
                            )
                        )
                    }
                }
            }

            Image(
                painter = painter,
                contentDescription = "Vehicle",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // headlight beam and tail glow
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = lightsLevel * (1f - abs(offset.value)).coerceIn(0f, 1f)
                    }
            ) {
                val w = size.width
                val h = size.height

                val hx = w * HEAD_X
                val hy = h * HEAD_Y
                val beamLen = w * 0.38f
                val spread = h * 0.24f
                val beam = Path().apply {
                    moveTo(hx, hy - h * 0.025f)
                    lineTo(hx + beamLen, hy - spread)
                    lineTo(hx + beamLen, hy + spread)
                    lineTo(hx, hy + h * 0.025f)
                    close()
                }
                drawPath(
                    path = beam,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFF3D0).copy(alpha = 0.55f * pulse),
                            Color(0xFFFFF3D0).copy(alpha = 0.15f * pulse),
                            Color.Transparent
                        ),
                        startX = hx,
                        endX = hx + beamLen
                    )
                )
                val headR = h * 0.12f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFFFFE9A8).copy(alpha = 0.45f * pulse),
                            Color.Transparent
                        ),
                        center = Offset(hx, hy),
                        radius = headR
                    ),
                    radius = headR,
                    center = Offset(hx, hy)
                )

                val tx = w * TAIL_X
                val ty = h * TAIL_Y
                val tailLen = w * 0.10f
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF1E1E).copy(alpha = 0.55f * pulse),
                            Color.Transparent
                        ),
                        startX = tx,
                        endX = tx - tailLen
                    ),
                    topLeft = Offset(tx - tailLen, ty - h * 0.05f),
                    size = Size(tailLen, h * 0.10f)
                )
                val tailR = h * 0.14f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF3B30).copy(alpha = 0.9f),
                            Color(0xFFFF1E1E).copy(alpha = 0.35f * pulse),
                            Color.Transparent
                        ),
                        center = Offset(tx, ty),
                        radius = tailR
                    ),
                    radius = tailR,
                    center = Offset(tx, ty)
                )
            }
        }
    }
}

/* ---------------- DOCK ---------------- */

private data class DockItem(
    val label: String,
    val icon: ImageVector,
    val badge: Boolean = false,
    val dim: Boolean = false
)

@Composable
fun Dock(
    theme: CarTheme,
    onApps: () -> Unit,
    onRadio: () -> Unit,
    onRadioLong: () -> Unit
) {
    val ctx = LocalContext.current
    val items = listOf(
        DockItem("Navigation", Icons.Filled.Navigation, badge = true),
        DockItem("Music", Icons.Filled.MusicNote),
        DockItem("Video", Icons.Filled.OndemandVideo, badge = true, dim = true),
        DockItem("Phone", Icons.Filled.Phone),
        DockItem("Radio", Icons.Filled.Radio),
        DockItem("Connectivity", Icons.Filled.Public),
        DockItem("Settings", Icons.Filled.Settings),
        DockItem("Apps", Icons.Filled.Apps)
    )
    var selected by remember { mutableStateOf("Navigation") }

    Row(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(theme.card)
            .border(1.dp, theme.track.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val active = selected == item.label
            val color = when {
                active -> theme.accent
                item.dim -> theme.subText.copy(alpha = 0.5f)
                else -> theme.text
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(item.label) {
                        detectTapGestures(
                            onLongPress = { if (item.label == "Radio") onRadioLong() },
                            onTap = {
                                selected = item.label
                                when (item.label) {
                                    "Navigation" -> ctx.startSafely(Intent(Intent.ACTION_VIEW, "geo:0,0?q=".toUri()))
                                    "Music" -> launchByCategory(ctx, Intent.CATEGORY_APP_MUSIC)
                                    "Video" -> launchByCategory(ctx, Intent.CATEGORY_APP_GALLERY)
                                    "Phone" -> ctx.startSafely(Intent(Intent.ACTION_DIAL))
                                    "Radio" -> onRadio()
                                    "Connectivity" -> ctx.startSafely(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                    "Settings" -> ctx.startSafely(Intent(Settings.ACTION_SETTINGS))
                                    "Apps" -> onApps()
                                }
                            }
                        )
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Icon(item.icon, null, tint = color, modifier = Modifier.size(24.dp))
                    if (item.badge) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(theme.accent)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(item.label, color = color, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/* ---------------- APP DRAWER ---------------- */

@Composable
fun AppDrawer(
    theme: CarTheme,
    title: String = "All apps",
    onPick: ((String) -> Unit)? = null,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val apps = remember {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .map {
                Triple(
                    it.loadLabel(pm).toString(),
                    it.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                    it.activityInfo.packageName
                )
            }
    }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.text,
                modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClose)
            )
            Spacer(Modifier.width(12.dp))
            Text(title, color = theme.text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(96.dp), modifier = Modifier.fillMaxSize()) {
            items(apps) { (label, icon, pkg) ->
                Column(
                    Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (onPick != null) onPick(pkg)
                            else ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startSafely(it) }
                        }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(icon, null, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(label, color = theme.text, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
/* ---------------- BACKGROUND LINES ---------------- */
/* ---------------- BACKGROUND: shapes + lines + gradients ---------------- */

private fun hexPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0..5) {
        val a = Math.toRadians(60.0 * i - 30.0)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

fun Modifier.dashboardBackground(theme: CarTheme, night: Boolean): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    val k = if (night) 1f else 0.55f
    val tint = if (night) Color.White else Color.Black
    val full = Size(w, h)

    // 1) Large soft gradient glows
    drawRect(
        Brush.radialGradient(
            listOf(theme.accent.copy(alpha = 0.30f * k), Color.Transparent),
            center = Offset(w * 0.88f, h * 0.12f), radius = w * 0.55f
        ), size = full
    )
    drawRect(
        Brush.radialGradient(
            listOf(theme.accent.copy(alpha = 0.20f * k), Color.Transparent),
            center = Offset(w * 0.08f, h * 0.92f), radius = w * 0.5f
        ), size = full
    )
    drawRect(
        Brush.radialGradient(
            listOf(tint.copy(alpha = 0.05f * k), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.7f
        ), size = full
    )

    // 2) Slanted translucent bands
    val band1 = Path().apply {
        moveTo(w * 0.58f, 0f); lineTo(w * 0.80f, 0f)
        lineTo(w * 0.38f, h); lineTo(w * 0.16f, h); close()
    }
    drawPath(
        band1,
        Brush.linearGradient(
            listOf(theme.accent.copy(alpha = 0.12f * k), Color.Transparent),
            start = Offset(w * 0.7f, 0f), end = Offset(w * 0.25f, h)
        )
    )
    val band2 = Path().apply {
        moveTo(w * 0.86f, 0f); lineTo(w * 0.92f, 0f)
        lineTo(w * 0.50f, h); lineTo(w * 0.44f, h); close()
    }
    drawPath(
        band2,
        Brush.linearGradient(
            listOf(tint.copy(alpha = 0.06f * k), Color.Transparent),
            start = Offset(w * 0.9f, 0f), end = Offset(w * 0.47f, h)
        )
    )

    // 3) Concentric rings (bottom right corner)
    for (i in 1..6) {
        drawCircle(
            color = theme.accent.copy(alpha = 0.22f * k * (1f - i / 7f)),
            radius = h * (0.14f + 0.13f * i),
            center = Offset(w * 0.95f, h * 1.0f),
            style = Stroke(1.2.dp.toPx())
        )
    }

    // 4) Flowing gradient curves
    // startY, control1Y, control2Y, endY, alpha, stroke(dp)
    val lines = listOf(
        floatArrayOf(0.20f, 0.05f, 0.55f, 0.35f, 0.35f, 1.5f),
        floatArrayOf(0.35f, 0.15f, 0.75f, 0.50f, 0.22f, 1.2f),
        floatArrayOf(0.55f, 0.40f, 0.95f, 0.70f, 0.30f, 1.8f),
        floatArrayOf(0.75f, 0.60f, 1.10f, 0.88f, 0.18f, 1.0f),
        floatArrayOf(0.95f, 0.80f, 1.25f, 1.00f, 0.25f, 1.4f)
    )
    lines.forEach { l ->
        val path = Path().apply {
            moveTo(0f, h * l[0])
            cubicTo(w * 0.30f, h * l[1], w * 0.65f, h * l[2], w, h * l[3])
        }
        val brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                theme.accent.copy(alpha = l[4] * k),
                tint.copy(alpha = l[4] * 0.35f * k),
                Color.Transparent
            ),
            startX = 0f, endX = w
        )
        drawPath(path, brush, style = Stroke(l[5].dp.toPx() * 6f, cap = StrokeCap.Round), alpha = 0.22f)
        drawPath(path, brush, style = Stroke(l[5].dp.toPx(), cap = StrokeCap.Round))
    }

    // 5) Dot grid (top left, fades out)
    val gap = 22.dp.toPx()
    val dotR = 1.6.dp.toPx()
    val gridW = w * 0.26f
    val gridH = h * 0.30f
    var gy = gap
    while (gy < gridH) {
        var gx = gap
        while (gx < gridW) {
            val fade = (1f - (gx / gridW) * 0.6f - (gy / gridH) * 0.6f).coerceIn(0f, 1f)
            drawCircle(tint.copy(alpha = 0.28f * k * fade), dotR, Offset(gx, gy))
            gx += gap
        }
        gy += gap
    }

    // 6) Outlined hexagons
    drawPath(
        hexPath(w * 0.50f, h * 0.10f, h * 0.10f),
        theme.accent.copy(alpha = 0.28f * k),
        style = Stroke(1.2.dp.toPx())
    )
    drawPath(
        hexPath(w * 0.545f, h * 0.18f, h * 0.06f),
        tint.copy(alpha = 0.14f * k),
        style = Stroke(1.dp.toPx())
    )
    drawPath(
        hexPath(w * 0.06f, h * 0.55f, h * 0.08f),
        theme.accent.copy(alpha = 0.22f * k),
        style = Stroke(1.dp.toPx())
    )

    // 7) Short accent dashes (tech look)
    for (i in 0 until 5) {
        val y = h * (0.30f + 0.045f * i)
        val len = w * (0.05f + 0.02f * (i % 3))
        drawLine(
            Brush.horizontalGradient(
                listOf(theme.accent.copy(alpha = 0.35f * k), Color.Transparent),
                startX = w * 0.02f, endX = w * 0.02f + len
            ),
            start = Offset(w * 0.02f, y), end = Offset(w * 0.02f + len, y),
            strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
        )
    }
}

/* ---------------- HELPERS ---------------- */

fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { }
}

fun launchByCategory(ctx: Context, category: String) {
    ctx.startSafely(Intent(Intent.ACTION_MAIN).addCategory(category))
}