package com.example.carlauncher

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
    weather: WeatherState,
    animating: Boolean,
    onAnimating: () -> Unit,
    onRadio: () -> Unit,
    onRadioLong: () -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit,
    onApps: () -> Unit
) {
    val night = theme.name == "NIGHT"
    val k = if (night) 1f else 0.4f
    val padPx = with(LocalDensity.current) { 18.dp.toPx() } // matches the padding in LauncherApp

    Column(
        Modifier
            .fillMaxSize()
            .drawBehind {
                // red ambient glow behind everything (extends under the outer padding)
                val tl = Offset(-padPx, -padPx)
                val sz = Size(size.width + padPx * 2, size.height + padPx * 2)
                drawRect(
                    Brush.radialGradient(
                        listOf(theme.accent.copy(alpha = 0.42f * k), Color.Transparent),
                        center = Offset(size.width * 0.16f, size.height * 0.62f),
                        radius = size.width * 0.55f
                    ), tl, sz
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(theme.accent.copy(alpha = 0.14f * k), Color.Transparent),
                        center = Offset(size.width * 0.96f, size.height * 0.02f),
                        radius = size.width * 0.4f
                    ), tl, sz
                )
            }
    ) {
        DriveTopBar(theme, animating, onAnimating, onTheme, onSettings)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.weight(1f).fillMaxWidth()) {
            DriveSpeedPanel(theme, telemetry, animating, Modifier.weight(1.25f).fillMaxHeight())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                DriveRadioCard(theme, onRadio, Modifier.height(84.dp).fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                DriveWhereTo(theme, telemetry, Modifier.weight(1f).fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(86.dp)) {
            DriveWeather(theme, weather, Modifier.weight(0.8f).fillMaxHeight())
            Spacer(Modifier.width(14.dp))
            DriveDock(theme, onRadio, onRadioLong, onApps, Modifier.weight(1.3f).fillMaxHeight())
        }
    }
}

/* ================= GLASS CONTAINER ================= */

@Composable
private fun Glass(
    theme: CarTheme,
    modifier: Modifier,
    radius: Dp = 20.dp,
    pad: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val tint = if (theme.name == "NIGHT") Color.White else Color.Black
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.13f), tint.copy(alpha = 0.04f))))
            .background(Brush.horizontalGradient(listOf(theme.accent.copy(alpha = 0.10f), Color.Transparent)))
            .border(
                1.dp,
                Brush.linearGradient(listOf(tint.copy(alpha = 0.32f), tint.copy(alpha = 0.05f), theme.accent.copy(alpha = 0.30f))),
                shape
            )
            .padding(pad),
        content = content
    )
}

/* ================= TOP BAR ================= */

@Composable
private fun DriveTopBar(
    theme: CarTheme,
    animating: Boolean,
    onAnimating: () -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.ENGLISH) }
    val ampmFmt = remember { SimpleDateFormat("a", Locale.ENGLISH) }
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.ENGLISH) }

    Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(timeFmt.format(Date(now)), color = theme.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(ampmFmt.format(Date(now)), color = theme.text, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.width(12.dp))
        Text(dateFmt.format(Date(now)), color = theme.text, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.weight(1f))

        TopIcon(if (animating) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (animating) theme.accent else theme.subText, onAnimating)
        Spacer(Modifier.width(18.dp))
        TopIcon(Icons.Filled.NightsStay, theme.text, onTheme)
        Spacer(Modifier.width(18.dp))
        TopIcon(Icons.Filled.Settings, theme.text, onSettings)
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
        modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onClick)
    )
}

/* ================= SPEEDOMETER ================= */

@Composable
private fun DriveSpeedPanel(theme: CarTheme, t: Telemetry, animating: Boolean, modifier: Modifier) {
    Box(modifier) {
        DriveGauge(theme, t.speed, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.Center).offset(y = (-14).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${t.speed}", color = theme.text, fontSize = 68.sp, fontWeight = FontWeight.Bold)
            Text("km/h", color = theme.subText, fontSize = 15.sp)
        }
        CarImage(
            theme,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.72f).fillMaxHeight(0.36f),
            lightsOn = theme.name == "NIGHT",
            animating = animating,
            gpsSpeed = t.speed
        )
    }
}

@Composable
private fun DriveGauge(theme: CarTheme, speed: Int, modifier: Modifier, max: Int = 240) {
    val startAngle = 140f
    val sweepAngle = 260f
    val shown by animateFloatAsState(
        targetValue = speed.coerceIn(0, max).toFloat(),
        animationSpec = tween(600),
        label = "driveGaugeSpeed"
    )
    val night = theme.name == "NIGHT"
    val tint = if (night) Color.White else Color.Black

    BoxWithConstraints(modifier) {
        val d = minOf(maxWidth, maxHeight)
        Box(Modifier.size(d).align(Alignment.Center)) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = 16.dp.toPx()
                val r = size.minDimension / 2 - inset
                val c = Offset(size.width / 2, size.height / 2)
                val topLeft = Offset(inset, inset)
                val arcSize = Size(r * 2, r * 2)
                val frac = shown / max

                // dark inner disc
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.Black.copy(alpha = if (night) 0.55f else 0.06f), Color.Transparent),
                        center = c, radius = r
                    ),
                    radius = r * 0.95f, center = c
                )

                // thin full track (the grey arc on the right side in the image)
                drawArc(
                    tint.copy(alpha = 0.45f), startAngle, sweepAngle, false, topLeft, arcSize,
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )

                // red tapered arc: thick at 0, thin at the tip, with glow
                val n = 100
                val activeN = (n * frac).toInt()
                if (activeN > 0) {
                    val per = sweepAngle / n
                    for (i in 0 until activeN) {
                        val w = 18.dp.toPx() * (1f - 0.72f * (i / n.toFloat()))
                        drawArc(
                            theme.accent.copy(alpha = 0.16f), startAngle + i * per, per + 0.8f, false,
                            topLeft, arcSize, style = Stroke(w * 2.8f)
                        )
                    }
                    for (i in 0 until activeN) {
                        val w = 18.dp.toPx() * (1f - 0.72f * (i / n.toFloat()))
                        drawArc(
                            theme.accent, startAngle + i * per, per + 0.8f, false,
                            topLeft, arcSize, style = Stroke(w)
                        )
                    }
                }

                // tick marks
                val ticks = 60
                for (i in 0..ticks) {
                    val a = Math.toRadians((startAngle + sweepAngle * i / ticks).toDouble())
                    val major = i % 5 == 0
                    val outer = r - 12.dp.toPx()
                    val inner = outer - if (major) 10.dp.toPx() else 5.dp.toPx()
                    val cs = cos(a).toFloat()
                    val sn = sin(a).toFloat()
                    drawLine(
                        color = if (i / ticks.toFloat() <= frac) theme.accent else tint.copy(alpha = 0.55f),
                        start = c + Offset(cs * outer, sn * outer),
                        end = c + Offset(cs * inner, sn * inner),
                        strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // numbers 0..240 every 20
            val rad = d / 2 - 16.dp - 12.dp - 28.dp
            for (i in 0..12) {
                val a = Math.toRadians(startAngle + sweepAngle * i / 12.0)
                Text(
                    "${i * 20}", color = theme.text.copy(alpha = 0.9f), fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = rad * cos(a).toFloat(), y = rad * sin(a).toFloat())
                )
            }
        }
    }
}

/* ================= RADIO ================= */

private const val FM_MIN = 87.5f
private const val FM_MAX = 108.0f

// Stations used by the scan buttons. Replace with your real saved or scanned list.
private val RADIO_PRESETS = listOf(
    87.5f, 90.3f, 92.3f, 94.7f, 97.1f, 99.9f, 101.1f, 103.5f, 105.7f, 107.9f
)

@Composable
private fun DriveRadioCard(
    theme: CarTheme,
    onRadio: () -> Unit,
    modifier: Modifier
) {
    var freq by remember { mutableFloatStateOf(87.5f) }

    // Fine tuning: 0.1 MHz per step, wraps around at the band ends
    fun step(delta: Float) {
        var f = Math.round((freq + delta) * 10f) / 10f
        if (f > FM_MAX) f = FM_MIN
        if (f < FM_MIN) f = FM_MAX
        freq = f
    }

    // Scan: jump to the next or previous station in the list, wraps around
    fun seek(forward: Boolean) {
        val eps = 0.05f
        freq = if (forward) {
            RADIO_PRESETS.firstOrNull { it > freq + eps } ?: RADIO_PRESETS.first()
        } else {
            RADIO_PRESETS.lastOrNull { it < freq - eps } ?: RADIO_PRESETS.last()
        }
    }

    Glass(theme, modifier, pad = 8.dp) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val swoosh = Path().apply {
                            moveTo(w * 0.05f, h * 0.05f)
                            cubicTo(w * 0.35f, h * 0.10f, w * 0.55f, h * 0.95f, w * 0.98f, h * 0.90f)
                        }
                        val brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, theme.accent.copy(alpha = 0.9f), Color.Transparent)
                        )
                        drawPath(swoosh, brush, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round), alpha = 0.18f)
                        drawPath(swoosh, brush, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scan back (round)
                RadioRoundButton(theme, Icons.Filled.SkipPrevious) { seek(false) }
                Spacer(Modifier.width(6.dp))
                // -0.1 (square)
                RadioSquareButton(theme, Icons.Filled.ChevronLeft,
                    onStep = { step(-0.1f) }, onBigStep = { step(-1f) })

                // Frequency display - tap to open the radio app
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onRadio),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("FM", color = theme.subText, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("%.2f".format(freq), color = theme.text, fontSize = 26.sp,
                        fontWeight = FontWeight.Light, maxLines = 1)
                    Spacer(Modifier.width(5.dp))
                    Text("MHz", color = theme.subText, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp))
                }

                // +0.1 (square)
                RadioSquareButton(theme, Icons.Filled.ChevronRight,
                    onStep = { step(0.1f) }, onBigStep = { step(1f) })
                Spacer(Modifier.width(6.dp))
                // Scan next (round)
                RadioRoundButton(theme, Icons.Filled.SkipNext) { seek(true) }
            }

            Spacer(Modifier.height(4.dp))

            // Tuner position bar
            val frac = ((freq - FM_MIN) / (FM_MAX - FM_MIN)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(theme.track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(theme.accent.copy(alpha = 0.3f), theme.accent))
                        )
                )
            }
        }
    }
}

/* Round button: scan to previous / next station */
@Composable
private fun RadioRoundButton(theme: CarTheme, icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(theme.text.copy(alpha = 0.14f), theme.text.copy(alpha = 0.04f))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(theme.text.copy(alpha = 0.4f), theme.accent.copy(alpha = 0.5f))),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = theme.text, modifier = Modifier.size(24.dp))
    }
}

/* Square button: fine step. Tap = 0.1 MHz, long press = 1 MHz */
@Composable
private fun RadioSquareButton(
    theme: CarTheme,
    icon: ImageVector,
    onStep: () -> Unit,
    onBigStep: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .size(width = 34.dp, height = 44.dp)
            .clip(shape)
            .background(theme.accent.copy(alpha = 0.22f))
            .border(1.dp, theme.accent.copy(alpha = 0.45f), shape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onStep() }, onLongPress = { onBigStep() })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = theme.text, modifier = Modifier.size(24.dp))
    }
}

/* ================= WHERE TO & LIVE NAVIGATION ================= */

@Composable
private fun DriveWhereTo(theme: CarTheme, telemetry: Telemetry, modifier: Modifier) {
    val ctx = LocalContext.current
    val currentLocation = LatLng(telemetry.latitude, telemetry.longitude)

    val mapView = remember {
        MapView(ctx).apply {
            try {
                onCreate(Bundle())
                onResume()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mapView.onPause()
                mapView.onDestroy()
            } catch (_: Exception) {}
        }
    }

    var googleMapInstance by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }

    LaunchedEffect(mapView) {
        try {
            mapView.getMapAsync { map ->
                googleMapInstance = map
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                map.uiSettings.isMapToolbarEnabled = false
                try {
                    map.isMyLocationEnabled = true
                } catch (_: SecurityException) {}
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(currentLocation, googleMapInstance) {
        googleMapInstance?.let { map ->
            try {
                val update = CameraUpdateFactory.newLatLngZoom(currentLocation, 15f)
                map.animateCamera(update)
                map.clear()
                map.addMarker(
                    MarkerOptions()
                        .position(currentLocation)
                        .title("Current Position")
                        .snippet("${telemetry.speed} km/h")
                )
            } catch (_: Exception) {}
        }
    }

    Glass(theme, modifier, pad = 12.dp) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Where to? • Live Navigation", color = theme.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.Navigation, null, tint = theme.accent, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(6.dp))

            // Live Google Map View container
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, theme.track, RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.accent.copy(alpha = 0.22f))
            .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = theme.text, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = theme.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/* ================= WEATHER ================= */

@Composable
private fun DriveWeather(theme: CarTheme, state: WeatherState, modifier: Modifier) {
    val ok = (state as? WeatherState.Ok)?.data
    val sunny = ok == null || ok.condition == "Clear"
    Glass(theme, modifier, radius = 20.dp, pad = 12.dp) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (sunny) Icons.Filled.WbSunny else Icons.Filled.Cloud, null,
                tint = if (sunny) Color(0xFFFFB300) else theme.text.copy(alpha = 0.85f),
                modifier = Modifier.size(42.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ok?.let { "${it.now}°C" } ?: "--°C", color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    when (state) {
                        is WeatherState.Ok -> state.data.condition
                        is WeatherState.Error -> "Retrying…"
                        WeatherState.Loading -> "Loading…"
                    },
                    color = theme.text, fontSize = 12.sp, maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = theme.subText, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(WEATHER_CITY, color = theme.subText, fontSize = 11.sp, maxLines = 1)
                }
            }
            if (ok != null) {
                Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(theme.track))
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text("H: ${ok.high}°C", color = theme.text, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("L: ${ok.low}°C", color = theme.subText, fontSize = 11.sp)
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
        DriveApp("Maps", Icons.Filled.Navigation, Color(0xFF2D6BFF)),
        DriveApp("Music", Icons.Filled.MusicNote, Color(0xFFF02B2B)),
        DriveApp("Phone", Icons.Filled.Phone, Color(0xFF22C55E)),
        DriveApp("Radio", Icons.Filled.Radio, Color(0xFF8B3FD9)),
        DriveApp("Apps", Icons.Filled.Apps, Color(0xFF5A5F6B))
    )
    Glass(theme, modifier, radius = 20.dp, pad = 6.dp) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(app.label) {
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
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Brush.verticalGradient(listOf(lerp(app.color, Color.White, 0.25f), app.color)))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(app.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(app.label, color = theme.text, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}