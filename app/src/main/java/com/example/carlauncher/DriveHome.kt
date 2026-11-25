package com.example.carlauncher

import android.content.Intent
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/* ================= DRIVE THEME: MAIN LAYOUT ================= */

@Composable
fun DriveHome(
    theme: CarTheme,
    telemetry: Telemetry,
    media: MediaState,
    weather: WeatherState,
    animating: Boolean,
    onAnimating: () -> Unit,
    onRadio: () -> Unit,
    onRadioLong: () -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit,
    onApps: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        DriveTopBar(theme, animating, onAnimating, onRadio, onTheme, onSettings)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.weight(1f).fillMaxWidth()) {
            DriveSpeedPanel(theme, telemetry, animating, Modifier.weight(1.1f).fillMaxHeight())
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                DriveMusicCard(theme, media, Modifier.weight(1.15f).fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                DriveWhereTo(theme, Modifier.weight(0.85f).fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(92.dp)) {
            DriveWeather(theme, weather, Modifier.weight(0.9f).fillMaxHeight())
            Spacer(Modifier.width(16.dp))
            DriveDock(theme, onRadio, onRadioLong, onApps, Modifier.weight(1.3f).fillMaxHeight())
        }
    }
}

/* ================= CARD BASE ================= */

@Composable
private fun DCard(
    theme: CarTheme,
    modifier: Modifier,
    glow: Boolean = false,
    map: Boolean = false,
    pad: Int = 16,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .clip(shape)
            .background(theme.card)
            .drawBehind {
                if (glow) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(theme.accent.copy(alpha = 0.32f), Color.Transparent),
                            center = Offset(0f, size.height),
                            radius = size.width * 0.75f
                        )
                    )
                }
                if (map) {
                    val c = theme.subText.copy(alpha = 0.08f)
                    for (i in 0..8) {
                        drawLine(
                            c,
                            Offset(size.width * (0.35f + i * 0.09f), 0f),
                            Offset(size.width * (0.15f + i * 0.12f), size.height),
                            strokeWidth = 2f
                        )
                    }
                    for (i in 0..3) {
                        drawLine(
                            c,
                            Offset(size.width * 0.3f, size.height * (0.15f + i * 0.28f)),
                            Offset(size.width, size.height * (0.05f + i * 0.28f)),
                            strokeWidth = 2f
                        )
                    }
                }
            }
            .border(1.dp, theme.track.copy(alpha = 0.7f), shape)
            .padding(pad.dp),
        content = content
    )
}

/* ================= TOP BAR ================= */

@Composable
private fun DriveTopBar(
    theme: CarTheme,
    animating: Boolean,
    onAnimating: () -> Unit,
    onRadio: () -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.ENGLISH) }
    val ampmFmt = remember { SimpleDateFormat("a", Locale.ENGLISH) }
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.ENGLISH) }

    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(timeFmt.format(Date(now)), color = theme.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(ampmFmt.format(Date(now)), color = theme.subText, fontSize = 12.sp)
        Spacer(Modifier.width(14.dp))
        Text(dateFmt.format(Date(now)), color = theme.text, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))

        TopIcon(if (animating) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (animating) theme.accent else theme.subText, onAnimating)
        Spacer(Modifier.width(18.dp))
        TopIcon(Icons.Filled.Radio, theme.text, onRadio)
        Spacer(Modifier.width(18.dp))
        TopIcon(Icons.Filled.Settings, theme.accent, onSettings)
        Spacer(Modifier.width(18.dp))
        TopIcon(if (theme.name == "NIGHT") Icons.Filled.NightsStay else Icons.Filled.WbSunny, theme.text, onTheme)
        Spacer(Modifier.width(18.dp))
        Icon(Icons.Filled.Wifi, null, tint = theme.text, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Filled.SignalCellularAlt, null, tint = theme.subText, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TopIcon(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Icon(
        icon, null, tint = tint,
        modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onClick).padding(2.dp)
    )
}

/* ================= SPEEDOMETER PANEL ================= */

@Composable
private fun DriveSpeedPanel(theme: CarTheme, t: Telemetry, animating: Boolean, modifier: Modifier) {
    Box(
        modifier.drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(theme.accent.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(0f, size.height * 0.55f),
                    radius = size.height * 0.95f
                )
            )
        }
    ) {
        DriveGauge(theme, t.speed, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.Center).offset(x = 26.dp, y = (-26).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${t.speed}", color = theme.text, fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text("km/h", color = theme.subText, fontSize = 16.sp)
        }
        CarImage(
            theme,
            Modifier.align(Alignment.BottomStart).padding(start = 8.dp)
                .fillMaxWidth(0.78f).fillMaxHeight(0.42f),
            lightsOn = theme.name == "NIGHT",
            animating = animating,
            gpsSpeed = t.speed
        )
    }
}

@Composable
private fun DriveGauge(theme: CarTheme, speed: Int, modifier: Modifier, max: Int = 240) {
    BoxWithConstraints(modifier) {
        val d = minOf(maxWidth, maxHeight)
        Box(Modifier.size(d).align(Alignment.Center)) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 7.dp.toPx()
                val inset = stroke * 1.5f + 4.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val frac = speed.coerceIn(0, max) / max.toFloat()

                // thin outer track
                drawArc(theme.subText.copy(alpha = 0.55f), 135f, 270f, false, Offset(inset, inset), arcSize,
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                // glowing active arc
                if (frac > 0f) {
                    drawArc(theme.accent.copy(alpha = 0.28f), 135f, 270f * frac, false, Offset(inset, inset), arcSize,
                        style = Stroke(stroke * 3f, cap = StrokeCap.Round))
                    drawArc(theme.accent, 135f, 270f * frac, false, Offset(inset, inset), arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round))
                }

                // tick marks every 10 km/h, long every 20
                val center = Offset(size.width / 2, size.height / 2)
                val outerR = size.minDimension / 2 - inset - stroke * 1.4f
                val ticks = 24
                for (i in 0..ticks) {
                    val a = Math.toRadians(135.0 + 270.0 * i / ticks)
                    val major = i % 2 == 0
                    val len = if (major) 12.dp.toPx() else 6.dp.toPx()
                    val c = cos(a).toFloat()
                    val s = sin(a).toFloat()
                    drawLine(
                        color = if (i / ticks.toFloat() <= frac) theme.accent else theme.subText.copy(alpha = 0.7f),
                        start = center + Offset(c * outerR, s * outerR),
                        end = center + Offset(c * (outerR - len), s * (outerR - len)),
                        strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
            }
            // numbers every 20 km/h
            val rad = d / 2 - 58.dp
            for (i in 0..12) {
                val a = Math.toRadians(135.0 + 270.0 * i / 12)
                Text(
                    "${i * 20}", color = theme.text.copy(alpha = 0.9f), fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                        .offset(x = rad * cos(a).toFloat(), y = rad * sin(a).toFloat())
                )
            }
        }
    }
}

/* ================= MUSIC CARD ================= */

@Composable
private fun DriveMusicCard(theme: CarTheme, m: MediaState, modifier: Modifier) {
    val ctx = LocalContext.current
    DCard(theme, modifier, glow = true, pad = 14) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(theme.accent, theme.accent.copy(alpha = 0.45f)))),
                    contentAlignment = Alignment.Center
                ) {
                    val art = m.art
                    if (art != null) Image(art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).clickable(enabled = !m.access) {
                    ctx.startSafely(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }) {
                    Text(m.title, color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(m.artist, color = theme.subText, fontSize = 13.sp, maxLines = 1)
                }
            }
            val frac = if (m.duration > 0) (m.position.toFloat() / m.duration).coerceIn(0f, 1f) else 0f
            Column {
                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(theme.track)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(theme.accent))
                }
                Spacer(Modifier.height(3.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(clock(m.position), color = theme.subText, fontSize = 10.sp)
                    Text(clock(m.duration), color = theme.subText, fontSize = 10.sp)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SkipPrevious, null, tint = theme.text,
                    modifier = Modifier.size(34.dp).clip(CircleShape).clickable { m.previous() })
                Spacer(Modifier.width(26.dp))
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(theme.track.copy(alpha = 0.7f))
                        .border(1.dp, theme.subText.copy(alpha = 0.4f), CircleShape)
                        .clickable { m.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (m.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null,
                        tint = theme.text, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(26.dp))
                Icon(Icons.Filled.SkipNext, null, tint = theme.text,
                    modifier = Modifier.size(34.dp).clip(CircleShape).clickable { m.next() })
            }
        }
    }
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/* ================= WHERE TO ================= */

@Composable
private fun DriveWhereTo(theme: CarTheme, modifier: Modifier) {
    val ctx = LocalContext.current
    DCard(theme, modifier, map = true, pad = 14) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Where to?", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().height(50.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DestButton(theme, "Home", Icons.Filled.Home, Modifier.weight(1f)) {
                    ctx.startSafely(Intent(Intent.ACTION_VIEW, "geo:0,0?q=Home".toUri()))
                }
                DestButton(theme, "Work", Icons.Filled.Work, Modifier.weight(1f)) {
                    ctx.startSafely(Intent(Intent.ACTION_VIEW, "geo:0,0?q=Work".toUri()))
                }
                DestButton(theme, "Search", Icons.Filled.Search, Modifier.weight(1f)) {
                    ctx.startSafely(Intent(Intent.ACTION_VIEW, "geo:0,0?q=".toUri()))
                }
            }
        }
    }
}

@Composable
private fun DestButton(theme: CarTheme, label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxHeight().clip(RoundedCornerShape(12.dp))
            .background(theme.accent.copy(alpha = 0.16f))
            .border(1.dp, theme.accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = theme.text, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/* ================= WEATHER ================= */

@Composable
private fun DriveWeather(theme: CarTheme, state: WeatherState, modifier: Modifier) {
    val ok = (state as? WeatherState.Ok)?.data
    val sunny = ok == null || ok.condition == "Clear"
    DCard(theme, modifier, pad = 14) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (sunny) Icons.Filled.WbSunny else Icons.Filled.Cloud,
                null,
                tint = if (sunny) Color(0xFFFFB300) else Color(0xFFD5D9E0),
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(ok?.let { "${it.now}°C" } ?: "--°C", color = theme.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    when (state) {
                        is WeatherState.Ok -> state.data.condition
                        is WeatherState.Error -> "Retrying…"
                        WeatherState.Loading -> "Loading…"
                    },
                    color = theme.subText, fontSize = 13.sp, maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = theme.subText, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(WEATHER_CITY, color = theme.subText, fontSize = 11.sp, maxLines = 1)
                }
            }
            if (ok != null) {
                Box(Modifier.width(1.dp).height(44.dp).background(theme.track))
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("H: ${ok.high}°C", color = theme.text, fontSize = 12.sp)
                    Text("L: ${ok.low}°C", color = theme.subText, fontSize = 12.sp)
                }
            }
        }
    }
}

/* ================= DOCK ================= */

private data class DriveApp(val label: String, val icon: ImageVector, val color: Color)

@Composable
private fun DriveDock(
    theme: CarTheme,
    onRadio: () -> Unit,
    onRadioLong: () -> Unit,
    onApps: () -> Unit,
    modifier: Modifier
) {
    val ctx = LocalContext.current
    val apps = listOf(
        DriveApp("Maps", Icons.Filled.Navigation, Color(0xFF1F5BFF)),
        DriveApp("Music", Icons.Filled.MusicNote, Color(0xFFF02B2B)),
        DriveApp("Phone", Icons.Filled.Phone, Color(0xFF1FD15C)),
        DriveApp("Radio", Icons.Filled.Radio, Color(0xFF8B3FD9)),
        DriveApp("Apps", Icons.Filled.GridView, Color(0xFF5A5F6B))
    )
    DCard(theme, modifier, pad = 6) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                Column(
                    Modifier.weight(1f).fillMaxHeight().pointerInput(app.label) {
                        detectTapGestures(
                            onLongPress = { if (app.label == "Radio") onRadioLong() },
                            onTap = {
                                when (app.label) {
                                    "Maps" -> ctx.startSafely(Intent(Intent.ACTION_VIEW, "geo:0,0?q=".toUri()))
                                    "Music" -> launchByCategory(ctx, Intent.CATEGORY_APP_MUSIC)
                                    "Phone" -> ctx.startSafely(Intent(Intent.ACTION_DIAL))
                                    "Radio" -> onRadio()
                                    "Apps" -> onApps()
                                }
                            }
                        )
                    },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(app.color, app.color.copy(alpha = 0.7f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(app.icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(app.label, color = theme.text, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}