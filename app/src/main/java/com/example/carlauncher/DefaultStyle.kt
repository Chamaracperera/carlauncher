package com.example.carlauncher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import kotlin.math.cos
import kotlin.math.sin

/* ================= STYLE DEFINITION ================= */

object DefaultStyle : DashboardStyle {
    override val id = "demo"          // keep: already saved in users' prefs
    override val label = "Default"
    override val description = "Classic dashboard"
    override val icon: ImageVector = Icons.Filled.Dashboard

    override fun background(theme: CarTheme): Modifier = defaultBackground(theme)

    @Composable
    override fun Content(theme: CarTheme, env: DashboardEnv) = DefaultHome(theme, env)
}

/* ================= BACKGROUND ================= */

private fun hexPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0..5) {
        val a = Math.toRadians(60.0 * i - 30.0)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Paths and brushes are built once per size, only the draw calls run per frame. */
private fun defaultBackground(theme: CarTheme): Modifier = Modifier.drawWithCache {
    val night = theme.name == "NIGHT"
    val w = size.width
    val h = size.height
    val k = if (night) 1f else 0.55f
    val tint = if (night) Color.White else Color.Black
    val accent = theme.accent

    // 1) soft glows
    val glow1 = Brush.radialGradient(
        listOf(accent.copy(alpha = 0.30f * k), Color.Transparent),
        center = Offset(w * 0.88f, h * 0.12f), radius = w * 0.55f
    )
    val glow2 = Brush.radialGradient(
        listOf(accent.copy(alpha = 0.20f * k), Color.Transparent),
        center = Offset(w * 0.08f, h * 0.92f), radius = w * 0.5f
    )
    val glow3 = Brush.radialGradient(
        listOf(tint.copy(alpha = 0.05f * k), Color.Transparent),
        center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.7f
    )

    // 2) slanted bands
    val band1 = Path().apply {
        moveTo(w * 0.58f, 0f); lineTo(w * 0.80f, 0f)
        lineTo(w * 0.38f, h); lineTo(w * 0.16f, h); close()
    }
    val band1Brush = Brush.linearGradient(
        listOf(accent.copy(alpha = 0.12f * k), Color.Transparent),
        start = Offset(w * 0.7f, 0f), end = Offset(w * 0.25f, h)
    )
    val band2 = Path().apply {
        moveTo(w * 0.86f, 0f); lineTo(w * 0.92f, 0f)
        lineTo(w * 0.50f, h); lineTo(w * 0.44f, h); close()
    }
    val band2Brush = Brush.linearGradient(
        listOf(tint.copy(alpha = 0.06f * k), Color.Transparent),
        start = Offset(w * 0.9f, 0f), end = Offset(w * 0.47f, h)
    )

    // 4) flowing curves: startY, c1Y, c2Y, endY, alpha, stroke(dp)
    val lines = listOf(
        floatArrayOf(0.20f, 0.05f, 0.55f, 0.35f, 0.35f, 1.5f),
        floatArrayOf(0.35f, 0.15f, 0.75f, 0.50f, 0.22f, 1.2f),
        floatArrayOf(0.55f, 0.40f, 0.95f, 0.70f, 0.30f, 1.8f),
        floatArrayOf(0.75f, 0.60f, 1.10f, 0.88f, 0.18f, 1.0f),
        floatArrayOf(0.95f, 0.80f, 1.25f, 1.00f, 0.25f, 1.4f)
    )
    class Curve(val path: Path, val brush: Brush, val stroke: Float)
    val curves = lines.map { l ->
        Curve(
            Path().apply {
                moveTo(0f, h * l[0])
                cubicTo(w * 0.30f, h * l[1], w * 0.65f, h * l[2], w, h * l[3])
            },
            Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    accent.copy(alpha = l[4] * k),
                    tint.copy(alpha = l[4] * 0.35f * k),
                    Color.Transparent
                ),
                startX = 0f, endX = w
            ),
            l[5].dp.toPx()
        )
    }

    // 6) hexagons
    val hex1 = hexPath(w * 0.50f, h * 0.10f, h * 0.10f)
    val hex2 = hexPath(w * 0.545f, h * 0.18f, h * 0.06f)
    val hex3 = hexPath(w * 0.06f, h * 0.55f, h * 0.08f)

    // 7) dashes
    class Dash(val y: Float, val len: Float, val brush: Brush)
    val dashes = List(5) { i ->
        val len = w * (0.05f + 0.02f * (i % 3))
        Dash(
            h * (0.30f + 0.045f * i), len,
            Brush.horizontalGradient(
                listOf(accent.copy(alpha = 0.35f * k), Color.Transparent),
                startX = w * 0.02f, endX = w * 0.02f + len
            )
        )
    }

    onDrawBehind {
        val full = Size(w, h)
        drawRect(glow1, size = full)
        drawRect(glow2, size = full)
        drawRect(glow3, size = full)

        drawPath(band1, band1Brush)
        drawPath(band2, band2Brush)

        // 3) concentric rings (bottom right)
        for (i in 1..6) {
            drawCircle(
                color = accent.copy(alpha = 0.22f * k * (1f - i / 7f)),
                radius = h * (0.14f + 0.13f * i),
                center = Offset(w * 0.95f, h),
                style = Stroke(1.2.dp.toPx())
            )
        }

        curves.forEach { c ->
            drawPath(c.path, c.brush, style = Stroke(c.stroke * 6f, cap = StrokeCap.Round), alpha = 0.22f)
            drawPath(c.path, c.brush, style = Stroke(c.stroke, cap = StrokeCap.Round))
        }

        // 5) dot grid (top left, fades out)
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

        drawPath(hex1, accent.copy(alpha = 0.28f * k), style = Stroke(1.2.dp.toPx()))
        drawPath(hex2, tint.copy(alpha = 0.14f * k), style = Stroke(1.dp.toPx()))
        drawPath(hex3, accent.copy(alpha = 0.22f * k), style = Stroke(1.dp.toPx()))

        dashes.forEach { d ->
            drawLine(
                d.brush,
                start = Offset(w * 0.02f, d.y), end = Offset(w * 0.02f + d.len, d.y),
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
            )
        }
    }
}

/* ================= HOME LAYOUT ================= */

@Composable
private fun DefaultHome(theme: CarTheme, env: DashboardEnv) {
    val state = env.state
    val telemetry = env.telemetry

    Column(Modifier.fillMaxSize()) {
        DefaultTopBar(theme, state, env.onSettings)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.weight(1f).fillMaxWidth()) {
            SpeedCard(theme, telemetry, Modifier.weight(0.9f).fillMaxHeight())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(2.2f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().height(110.dp)) {
                    MusicCard(theme, env.media, Modifier.weight(1.5f).fillMaxHeight())
                    Spacer(Modifier.width(14.dp))
                    WeatherCard(theme, env, Modifier.weight(1f).fillMaxHeight())
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    CarImage(
                        theme,
                        Modifier.fillMaxWidth().fillMaxHeight(0.75f),
                        lightsOn = state.night,
                        animating = state.animating,
                        gpsSpeed = { telemetry.speed }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        DefaultDock(theme, env)
    }
}

/* ================= TOP BAR ================= */

@Composable
private fun DefaultTopBar(theme: CarTheme, state: LauncherState, onSettings: () -> Unit) {
    val now = rememberNow()
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH) }
    val ownerName = state.ownerName

    // derived: the bar itself only recomposes when the text really changes
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

    Box(Modifier.fillMaxWidth().height(56.dp)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Home, null, tint = theme.subText, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(greeting, color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(date, color = theme.subText, fontSize = 12.sp)
            }
        }
        BigClock(theme, now, Modifier.align(Alignment.Center))
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            PillButton(theme, onClick = state::toggleAnimating) {
                Icon(
                    if (state.animating) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null,
                    tint = if (state.animating) theme.accent else theme.subText,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            PillButton(theme, onClick = onSettings) {
                Icon(Icons.Filled.Settings, null, tint = theme.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            PillButton(theme, wide = true, onClick = state::toggleNight) {
                Icon(Icons.Filled.NightsStay, null, tint = theme.text, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(theme.name, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Only this small composable recomposes every second. */
@Composable
private fun BigClock(theme: CarTheme, now: State<Long>, modifier: Modifier) {
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.ENGLISH) }
    val meridiemFmt = remember { SimpleDateFormat("a", Locale.ENGLISH) }
    val d = Date(now.value)
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(timeFmt.format(d), color = theme.text, fontSize = 40.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.width(4.dp))
        Text(meridiemFmt.format(d), color = theme.subText, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun PillButton(
    theme: CarTheme,
    wide: Boolean = false,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(23.dp)
    Row(
        Modifier
            .height(46.dp)
            .then(if (wide) Modifier.widthIn(min = 96.dp) else Modifier.width(54.dp))
            .clip(shape)
            .background(theme.card)
            .border(1.dp, theme.track, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

/* ================= SPEED CARD ================= */

@Composable
private fun SpeedCard(theme: CarTheme, t: Telemetry, modifier: Modifier) {
    CardBox(theme, modifier) {
        Column(Modifier.fillMaxSize()) {
            Text("SPEED", color = theme.subText, fontSize = 12.sp, letterSpacing = 1.5.sp)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Gauge(theme, { t.speed }, Modifier.fillMaxHeight().aspectRatio(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpeedNumber(theme, t)
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
private fun SpeedNumber(theme: CarTheme, t: Telemetry) {
    Text("${t.speed}", color = theme.text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Stat(theme: CarTheme, label: String, value: String) {
    Column {
        Text(label, color = theme.subText, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = theme.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Gauge(theme: CarTheme, speed: () -> Int, modifier: Modifier, max: Int = 180) {
    // Animated value is read only in the draw phase: no recomposition while it moves.
    val shown = rememberSmoothValue(600) { speed().coerceIn(0, max).toFloat() }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val segments = 40
            val start = 135f
            val sweep = 270f
            val per = sweep / segments
            val active = ((shown.value / max) * segments).toInt()
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

/* ================= MUSIC CARD ================= */

@Composable
private fun MusicCard(theme: CarTheme, m: MediaState, modifier: Modifier) {
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
                MusicProgress(theme, m)
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

/** Position changes every second, so only this part recomposes. */
@Composable
private fun MusicProgress(theme: CarTheme, m: MediaState) {
    val frac = if (m.duration > 0) (m.position.toFloat() / m.duration).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(theme.track)) {
        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(theme.accent))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatTime(m.position), color = theme.subText, fontSize = 10.sp)
        Text(formatTime(m.duration), color = theme.subText, fontSize = 10.sp)
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun askNotificationAccess(ctx: Context) {
    ctx.startSafely(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
}

/* ================= WEATHER CARD ================= */

@Composable
private fun WeatherCard(theme: CarTheme, env: DashboardEnv, modifier: Modifier) {
    val state = env.weather
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

/* ================= DOCK ================= */

private class DockItem(
    val label: String,
    val icon: ImageVector,
    val badge: Boolean = false,
    val dim: Boolean = false
)

private val DOCK_ITEMS = listOf(
    DockItem("Navigation", Icons.Filled.Navigation, badge = true),
    DockItem("Music", Icons.Filled.MusicNote),
    DockItem("Video", Icons.Filled.OndemandVideo, badge = true, dim = true),
    DockItem("Phone", Icons.Filled.Phone),
    DockItem("Radio", Icons.Filled.Radio),
    DockItem("Connectivity", Icons.Filled.Public),
    DockItem("Settings", Icons.Filled.Settings),
    DockItem("Apps", Icons.Filled.Apps)
)

@Composable
private fun DefaultDock(theme: CarTheme, env: DashboardEnv) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf("Navigation") }
    val shape = RoundedCornerShape(18.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .background(theme.card)
            .border(1.dp, theme.track.copy(alpha = 0.6f), shape),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DOCK_ITEMS.forEach { item ->
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