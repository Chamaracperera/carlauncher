package com.example.carlauncher

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/* ================= STYLE DEFINITION ================= */

object AuroraStyle : DashboardStyle {
    override val id = "aurora"
    override val label = "Aurora"
    override val description = "Sidebar & neon glow"
    override val icon: ImageVector = Icons.Filled.AutoAwesome

    override fun background(theme: CarTheme): Modifier = auroraBackground(theme)

    @Composable
    override fun Content(theme: CarTheme, env: DashboardEnv) = AuroraHome(theme, env)
}

/* ================= BACKGROUND ================= */
/* Everything uses theme.accent, so it follows the vehicle colour. */

private fun auroraBackground(theme: CarTheme): Modifier = Modifier.drawWithCache {
    val night = theme.name == "NIGHT"
    val k = if (night) 1f else 0.45f
    val tint = if (night) Color.White else Color.Black
    val accent = theme.accent
    val w = size.width
    val h = size.height
    val full = Size(w, h)

    val glowTop = Brush.radialGradient(
        listOf(accent.copy(alpha = 0.30f * k), Color.Transparent),
        center = Offset(w * 0.02f, h * 0.02f), radius = w * 0.40f
    )
    val glowRight = Brush.radialGradient(
        listOf(accent.copy(alpha = 0.26f * k), Color.Transparent),
        center = Offset(w * 1.0f, h * 0.55f), radius = w * 0.38f
    )
    val glowBottom = Brush.radialGradient(
        listOf(accent.copy(alpha = 0.30f * k), Color.Transparent),
        center = Offset(w * 0.5f, h * 1.02f), radius = w * 0.60f
    )
    val vignette = Brush.radialGradient(
        listOf(Color.Transparent, Color.Black.copy(alpha = if (night) 0.45f else 0.08f)),
        center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.75f
    )

    // diagonal light streaks (top-left and bottom-right corners)
    val streak1 = Brush.linearGradient(
        listOf(accent.copy(alpha = 0.9f * k), Color.Transparent),
        start = Offset(0f, h * 0.14f), end = Offset(w * 0.12f, 0f)
    )
    val streak2 = Brush.linearGradient(
        listOf(accent.copy(alpha = 0.7f * k), Color.Transparent),
        start = Offset(w, h * 0.60f), end = Offset(w * 0.88f, h)
    )

    // curved neon line on the right side
    val curve = Path().apply {
        moveTo(w * 0.83f, h * 0.62f)
        quadraticBezierTo(w * 0.95f, h * 0.55f, w * 0.985f, h * 0.30f)
    }
    val curveBrush = Brush.linearGradient(
        listOf(Color.Transparent, accent.copy(alpha = 0.9f * k)),
        start = Offset(w * 0.83f, h * 0.62f), end = Offset(w * 0.985f, h * 0.30f)
    )

    // floor line under the car
    val floorY = h * 0.70f
    val floorBrush = Brush.horizontalGradient(
        listOf(Color.Transparent, accent.copy(alpha = 0.85f * k), Color.Transparent),
        startX = w * 0.18f, endX = w * 0.98f
    )

    onDrawBehind {
        drawRect(glowTop, size = full)
        drawRect(glowRight, size = full)
        drawRect(glowBottom, size = full)

        drawLine(streak1, Offset(0f, h * 0.14f), Offset(w * 0.12f, 0f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(streak2, Offset(w, h * 0.60f), Offset(w * 0.88f, h), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)

        drawPath(curve, curveBrush, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round), alpha = 0.16f)
        drawPath(curve, curveBrush, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

        drawLine(floorBrush, Offset(w * 0.18f, floorY), Offset(w * 0.98f, floorY), strokeWidth = 8.dp.toPx(), alpha = 0.18f)
        drawLine(floorBrush, Offset(w * 0.18f, floorY), Offset(w * 0.98f, floorY), strokeWidth = 2.dp.toPx())

        drawRect(vignette, size = full)
    }
}

/* ================= PANEL ================= */

@Composable
private fun AuroraPanel(
    theme: CarTheme,
    modifier: Modifier,
    radius: androidx.compose.ui.unit.Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val tint = if (theme.name == "NIGHT") Color.White else Color.Black
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.10f), tint.copy(alpha = 0.03f))))
            .border(
                1.2.dp,
                Brush.linearGradient(listOf(theme.accent.copy(alpha = 0.75f), theme.accent.copy(alpha = 0.18f), theme.accent.copy(alpha = 0.60f))),
                shape
            ),
        content = content
    )
}

/* ================= HOME LAYOUT ================= */

@Composable
private fun AuroraHome(theme: CarTheme, env: DashboardEnv) {
    val state = env.state
    val telemetry = env.telemetry

    Row(Modifier.fillMaxSize()) {
        AuroraSidebar(theme, env, Modifier.width(66.dp).fillMaxHeight())
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            AuroraTopBar(theme, env)
            Spacer(Modifier.height(6.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                // stats row, top right
                AuroraStats(
                    theme, telemetry,
                    Modifier.align(Alignment.TopEnd).fillMaxWidth(0.62f).padding(top = 4.dp)
                )
                // car, right: fixed wide aspect ratio so it is never cropped
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.64f)
                        .aspectRatio(2.4f)
                        .padding(bottom = 56.dp)
                ) {
                    CarImage(
                        theme,
                        Modifier.fillMaxSize(),
                        lightsOn = state.night,
                        animating = state.animating,
                        gpsSpeed = { telemetry.speed }
                    )
                }
                // speed + weather, left
                Column(
                    Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(0.36f),
                    verticalArrangement = Arrangement.Center
                ) {
                    AuroraSpeed(theme, telemetry)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.9f)
                            .height(2.dp)
                            .background(Brush.horizontalGradient(listOf(theme.accent, Color.Transparent)))
                    )
                    Spacer(Modifier.height(22.dp))
                    AuroraWeather(theme, env)
                }
            }

            Spacer(Modifier.height(10.dp))
            AuroraBottomRow(theme, env, Modifier.fillMaxWidth().height(84.dp))
        }
    }
}

/* ================= SIDEBAR ================= */

private class AuroraNav(
    val label: String,
    val icon: ImageVector,
    val badge: Boolean = false
)

private val AURORA_NAV = listOf(
    AuroraNav("Home", Icons.Filled.Home),
    AuroraNav("Navigation", Icons.Filled.Navigation, badge = true),
    AuroraNav("Music", Icons.Filled.MusicNote),
    AuroraNav("Video", Icons.Filled.OndemandVideo, badge = true),
    AuroraNav("Phone", Icons.Filled.Phone),
    AuroraNav("Radio", Icons.Filled.Radio),
    AuroraNav("Connectivity", Icons.Filled.Public),
    AuroraNav("Settings", Icons.Filled.Settings),
    AuroraNav("Apps", Icons.Filled.Apps)
)

@Composable
private fun AuroraSidebar(theme: CarTheme, env: DashboardEnv, modifier: Modifier) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf("Home") }
    val itemShape = RoundedCornerShape(14.dp)

    AuroraPanel(theme, modifier, radius = 20.dp) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 8.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AURORA_NAV.forEach { item ->
                val active = selected == item.label
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(itemShape)
                        .then(
                            if (active) Modifier
                                .background(theme.accent.copy(alpha = 0.28f))
                                .border(1.dp, theme.accent.copy(alpha = 0.7f), itemShape)
                            else Modifier
                        )
                        .pointerInput(item.label) {
                            detectTapGestures(
                                onLongPress = { if (item.label == "Radio") env.onRadioLong() },
                                onTap = {
                                    selected = item.label
                                    when (item.label) {
                                        "Navigation" -> ctx.startSafely(Intent(Intent.ACTION_VIEW, "geo:0,0?q=".toUri()))
                                        "Music" -> launchByCategory(ctx, Intent.CATEGORY_APP_MUSIC)
                                        "Video" -> launchByCategory(ctx, Intent.CATEGORY_APP_GALLERY)
                                        "Phone" -> ctx.startSafely(Intent(Intent.ACTION_DIAL))
                                        "Radio" -> env.onRadio()
                                        "Connectivity" -> ctx.startSafely(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                        "Settings" -> ctx.startSafely(Intent(Settings.ACTION_SETTINGS))
                                        "Apps" -> env.onApps()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon, null,
                        tint = if (active) theme.accent else theme.text.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                    if (item.badge) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(theme.accent)
                        )
                    }
                }
            }
        }
    }
}

/* ================= TOP BAR ================= */

@Composable
private fun AuroraTopBar(theme: CarTheme, env: DashboardEnv) {
    val ctx = LocalContext.current
    val state = env.state
    val now = rememberNow()
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH) }
    val ownerName = state.ownerName
    var locked by remember { mutableStateOf(true) }

    val greeting by remember(ownerName) {
        derivedStateOf {
            val hour = Calendar.getInstance().apply { timeInMillis = now.value }.get(Calendar.HOUR_OF_DAY)
            val base = when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..20 -> "Good evening"
                else -> "Good night"
            }
            if (ownerName.isNotBlank()) "$base, $ownerName" else base
        }
    }
    val date by remember { derivedStateOf { dateFmt.format(Date(now.value)) } }

    Box(Modifier.fillMaxWidth().height(52.dp)) {
        // left: accent bar + greeting + quick icons
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(theme.accent))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(greeting, color = theme.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(date, color = theme.subText, fontSize = 11.sp, maxLines = 1)
            }
            Spacer(Modifier.width(26.dp))
            AuroraTopIcon(Icons.Filled.Search, theme.text) {
                ctx.startSafely(Intent(Intent.ACTION_VIEW, "geo:0,0?q=".toUri()))
            }
        }

        // centre: clock
        AuroraClock(theme, now, Modifier.align(Alignment.Center))

        // right: status icons + play/pause + night pill
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            AuroraTopIcon(
                if (state.animating) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (state.animating) theme.accent else theme.subText,
                state::toggleAnimating
            )
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Filled.Wifi, null, tint = theme.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.SignalCellularAlt, null, tint = theme.subText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(14.dp))

            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier
                    .height(38.dp)
                    .widthIn(min = 92.dp)
                    .clip(shape)
                    .background(theme.card)
                    .border(1.dp, theme.track, shape)
                    .clickable(onClick = state::toggleNight)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.NightsStay, null, tint = theme.text, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(theme.name, color = theme.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            AuroraTopIcon(Icons.Filled.Settings, theme.accent, env.onSettings)
        }
    }
}

@Composable
private fun AuroraTopIcon(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Icon(
        icon, null, tint = tint,
        modifier = Modifier.size(24.dp).clip(CircleShape).clickable(onClick = onClick)
    )
}

/** Only this small composable recomposes every second. */
@Composable
private fun AuroraClock(theme: CarTheme, now: State<Long>, modifier: Modifier) {
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.ENGLISH) }
    val ampmFmt = remember { SimpleDateFormat("a", Locale.ENGLISH) }
    val d = Date(now.value)
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(timeFmt.format(d), color = theme.text, fontSize = 34.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(5.dp))
        Text(ampmFmt.format(d), color = theme.subText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 7.dp))
    }
}

/* ================= STATS ================= */

@Composable
private fun AuroraStats(theme: CarTheme, t: Telemetry, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        AuroraStat(theme, Icons.Filled.Route, "Trip", "${"%.0f".format(t.tripKm)} km", Modifier.weight(1f))
        AuroraDivider(theme)
        AuroraStat(theme, Icons.Filled.Schedule, "Drive", "${t.driveMin} min", Modifier.weight(1f))
        AuroraDivider(theme)
        AuroraStat(theme, Icons.Filled.Explore, "Heading", t.heading, Modifier.weight(1f))
    }
}

@Composable
private fun AuroraStat(theme: CarTheme, icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = theme.text, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = theme.subText, fontSize = 10.sp)
            Text(value, color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun AuroraDivider(theme: CarTheme) {
    Box(Modifier.width(1.dp).height(34.dp).background(theme.track))
}

/* ================= SPEED ================= */

@Composable
private fun AuroraSpeed(theme: CarTheme, t: Telemetry) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("${t.speed}", color = theme.text, fontSize = 72.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text("km/h", color = theme.subText, fontSize = 18.sp, modifier = Modifier.padding(bottom = 14.dp))
    }
}

/* ================= WEATHER ================= */

@Composable
private fun AuroraWeather(theme: CarTheme, env: DashboardEnv) {
    val state = env.weather
    val ok = (state as? WeatherState.Ok)?.data
    val sunny = ok == null || ok.condition == "Clear"
    val temps = ok?.let { "${it.high}° / ${it.low}°" } ?: "--° / --°"
    val info = when (state) {
        is WeatherState.Ok -> "Now ${state.data.now}° · ${state.data.condition} · Sunset ${state.data.sunset}"
        is WeatherState.Error -> "${state.message} · retrying…"
        WeatherState.Loading -> "Loading…"
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (sunny) Icons.Filled.WbSunny else Icons.Filled.Cloud, null,
                tint = if (sunny) Color(0xFFFFB300) else theme.text.copy(alpha = 0.9f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(WEATHER_CITY, color = theme.text, fontSize = 12.sp, maxLines = 1)
                Text(temps, color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(info, color = theme.subText, fontSize = 10.sp, maxLines = 1)
    }
}

/* ================= BOTTOM ROW (3 PANELS) ================= */

@Composable
private fun AuroraBottomRow(theme: CarTheme, env: DashboardEnv, modifier: Modifier) {
    Row(modifier) {
        AuroraMusicPanel(theme, env.media, Modifier.weight(1.5f).fillMaxHeight())
        Spacer(Modifier.width(12.dp))
        AuroraRadioPanel(theme, env, Modifier.weight(1.05f).fillMaxHeight())
        Spacer(Modifier.width(12.dp))
        AuroraVolumePanel(theme, Modifier.weight(0.9f).fillMaxHeight())
    }
}

/* ---------- 1. MUSIC ---------- */

@Composable
private fun AuroraMusicPanel(theme: CarTheme, m: MediaState, modifier: Modifier) {
    val ctx = LocalContext.current

    AuroraPanel(theme, modifier, radius = 24.dp) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // album art
            Box(
                Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF6B4FA0), Color(0xFFFF9A3C)))),
                contentAlignment = Alignment.Center
            ) {
                val art = m.art
                if (art != null) {
                    Image(art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(12.dp))

            // title + progress
            Column(
                Modifier
                    .weight(1f)
                    .clickable(enabled = !m.access) {
                        ctx.startSafely(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
            ) {
                Text("NOW PLAYING", color = theme.subText, fontSize = 9.sp, letterSpacing = 1.2.sp)
                Text(m.title, color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(m.artist, color = theme.subText, fontSize = 11.sp, maxLines = 1)
                AuroraProgress(theme, m)
            }

            Spacer(Modifier.width(10.dp))

            // controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SkipPrevious, null, tint = theme.text,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { m.previous() })
                Spacer(Modifier.width(8.dp))
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
                        null, tint = theme.text, modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.SkipNext, null, tint = theme.text,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { m.next() })
            }
        }
    }
}

/** Position changes every second, so only this part recomposes. */
@Composable
private fun AuroraProgress(theme: CarTheme, m: MediaState) {
    val frac = if (m.duration > 0) (m.position.toFloat() / m.duration).coerceIn(0f, 1f) else 0f
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(auroraTime(m.position), color = theme.subText, fontSize = 10.sp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(theme.track)) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(theme.accent.copy(alpha = 0.5f), theme.accent)))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(auroraTime(m.duration), color = theme.subText, fontSize = 10.sp)
    }
}

private fun auroraTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/* ---------- 2. RADIO CONTROL ---------- */

private const val AURORA_FM_MIN = 87.5f
private const val AURORA_FM_MAX = 108.0f

// Stations used by the scan buttons. Replace with your real saved or scanned list.
private val AURORA_PRESETS = listOf(
    87.5f, 90.3f, 92.3f, 94.7f, 97.1f, 99.9f, 101.1f, 103.5f, 105.7f, 107.9f
)

@Composable
private fun AuroraRadioPanel(theme: CarTheme, env: DashboardEnv, modifier: Modifier) {
    var freq by remember { mutableFloatStateOf(87.5f) }

    // Fine tuning: 0.1 MHz per step, wraps at the band ends
    fun step(delta: Float) {
        var f = Math.round((freq + delta) * 10f) / 10f
        if (f > AURORA_FM_MAX) f = AURORA_FM_MIN
        if (f < AURORA_FM_MIN) f = AURORA_FM_MAX
        freq = f
    }

    // Scan: jump to the next or previous preset, wraps around
    fun seek(forward: Boolean) {
        val eps = 0.05f
        freq = if (forward) {
            AURORA_PRESETS.firstOrNull { it > freq + eps } ?: AURORA_PRESETS.first()
        } else {
            AURORA_PRESETS.lastOrNull { it < freq - eps } ?: AURORA_PRESETS.last()
        }
    }

    AuroraPanel(theme, modifier, radius = 24.dp) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                AuroraRadioRound(theme, Icons.Filled.SkipPrevious) { seek(false) }
                Spacer(Modifier.width(4.dp))
                AuroraRadioStep(theme, Icons.Filled.ChevronLeft,
                    onStep = { step(-0.1f) }, onBigStep = { step(-1f) })

                // Frequency: tap to open the radio app, long press for radio settings
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { env.onRadio() }, onLongPress = { env.onRadioLong() })
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("RADIO · FM", color = theme.subText, fontSize = 9.sp, letterSpacing = 1.2.sp, maxLines = 1)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%.1f".format(freq), color = theme.text, fontSize = 24.sp,
                            fontWeight = FontWeight.Medium, maxLines = 1)
                        Spacer(Modifier.width(4.dp))
                        Text("MHz", color = theme.subText, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }

                AuroraRadioStep(theme, Icons.Filled.ChevronRight,
                    onStep = { step(0.1f) }, onBigStep = { step(1f) })
                Spacer(Modifier.width(4.dp))
                AuroraRadioRound(theme, Icons.Filled.SkipNext) { seek(true) }
            }

            // tuner position bar
            val frac = ((freq - AURORA_FM_MIN) / (AURORA_FM_MAX - AURORA_FM_MIN)).coerceIn(0f, 1f)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(theme.track)) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(theme.accent.copy(alpha = 0.4f), theme.accent)))
                )
            }
        }
    }
}

/** Round button: scan to previous / next station. */
@Composable
private fun AuroraRadioRound(theme: CarTheme, icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(1.dp, theme.accent.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = theme.text, modifier = Modifier.size(22.dp))
    }
}

/** Square button: fine step. Tap = 0.1 MHz, long press = 1 MHz. */
@Composable
private fun AuroraRadioStep(theme: CarTheme, icon: ImageVector, onStep: () -> Unit, onBigStep: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .size(width = 28.dp, height = 38.dp)
            .clip(shape)
            .background(theme.accent.copy(alpha = 0.22f))
            .border(1.dp, theme.accent.copy(alpha = 0.45f), shape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onStep() }, onLongPress = { onBigStep() })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = theme.text, modifier = Modifier.size(22.dp))
    }
}

/* ---------- 3. VOLUME ---------- */

@Composable
private fun AuroraVolumePanel(theme: CarTheme, modifier: Modifier) {
    AuroraPanel(theme, modifier, radius = 24.dp) {
        AuroraVolume(
            theme,
            Modifier.fillMaxSize().padding(horizontal = 14.dp)
        )
    }
}
/* ================= VOLUME ================= */

@Composable
private fun AuroraVolume(theme: CarTheme, modifier: Modifier) {
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember {
        mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol)
    }

    fun setVol(f: Float) {
        vol = f.coerceIn(0f, 1f)
        try {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (vol * maxVol).toInt(), 0)
        } catch (_: SecurityException) {}
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.VolumeUp, null, tint = theme.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.Remove, null, tint = theme.text,
            modifier = Modifier.size(22.dp).clip(CircleShape).clickable { setVol(vol - 1f / maxVol) })
        Spacer(Modifier.width(6.dp))

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(30.dp)
                .pointerInput(Unit) { detectTapGestures { setVol(it.x / size.width) } }
                .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> setVol(change.position.x / size.width) } },
            contentAlignment = Alignment.CenterStart
        ) {
            val trackW = maxWidth
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(theme.track))
            Box(
                Modifier
                    .width(trackW * vol)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(theme.accent.copy(alpha = 0.5f), theme.accent)))
            )
            Box(
                Modifier
                    .offset(x = (trackW * vol) - 7.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(theme.accent)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
        }

        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.Add, null, tint = theme.text,
            modifier = Modifier.size(22.dp).clip(CircleShape).clickable { setVol(vol + 1f / maxVol) })
    }
}