package com.example.carlauncher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Light positions as a fraction of the car image (0.0 to 1.0). Tune to your PNG.
private const val HEAD_X = 0.94f
private const val HEAD_Y = 0.50f
private const val TAIL_X = 0.06f
private const val TAIL_Y = 0.48f

// Rear wheel position, where the dust comes from
private const val WHEEL_X = 0.22f
private const val GROUND_Y = 0.92f

private class Streak(
    val y: Float, val len: Float, val speed: Float,
    val phase: Float, val thick: Float, val alpha: Float
)

// Built once for the whole app
private val STREAKS = Random(7).let { r ->
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

/**
 * [gpsSpeed] is a lambda on purpose: it is read in a snapshotFlow, so a speed
 * change never recomposes this function or its parent.
 */
@Composable
fun CarImage(
    theme: CarTheme,
    modifier: Modifier,
    lightsOn: Boolean = true,
    animating: Boolean = true,
    gpsSpeed: () -> Int = { 0 }
) {
    // -1.05 = just off-screen left, 0 = middle, +1.05 = just off-screen right
    val offset = remember { Animatable(-1.05f) }
    var shownImage by remember { mutableIntStateOf(theme.carImage) }
    var wasPaused by remember { mutableStateOf(false) }

    LaunchedEffect(theme.carImage, animating) {
        when {
            !animating -> {
                wasPaused = true
                shownImage = theme.carImage
                offset.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
            }
            wasPaused -> {
                offset.snapTo(-1.05f)
                wasPaused = false
                shownImage = theme.carImage
                offset.animateTo(0f, tween(1900, easing = FastOutSlowInEasing))
            }
            else -> {
                if (shownImage != theme.carImage) {
                    offset.animateTo(1.05f, tween(900, easing = FastOutSlowInEasing))
                    offset.snapTo(-1.05f)
                }
                shownImage = theme.carImage
                offset.animateTo(0f, tween(1900, easing = FastOutSlowInEasing))
            }
        }
    }

    // Smoothed speed so the wind builds up and dies away gradually.
    val smoothSpeed = rememberSmoothValue(1200, LinearOutSlowInEasing) {
        if (animating) gpsSpeed().toFloat() else 0f
    }

    // 0..1 wind strength (140 km/h and above = maximum). Call only in draw/effects.
    val windRaw: () -> Float = {
        val s = smoothSpeed.value
        if (!animating || s < 0.5f) 0f else (s / 140f).coerceIn(0f, 1f)
    }
    val windVisual: () -> Float = { sqrt(windRaw()) }

    // Animation clock. Runs frames ONLY while the car is really moving.
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        snapshotFlow { windRaw() > 0f }.collectLatest { moving ->
            if (!moving) return@collectLatest
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last != 0L) phase += (now - last) / 1_000_000_000f * 2.6f * windRaw()
                    last = now
                }
            }
        }
    }

    val lightsLevel by animateFloatAsState(
        targetValue = if (lightsOn) 1f else 0f,
        animationSpec = tween(700),
        label = "lightsLevel"
    )
    // The infinite pulse only runs while the lights are on.
    val pulse: State<Float> = if (lightsOn) {
        rememberInfiniteTransition(label = "lights").animateFloat(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )
    } else remember { mutableFloatStateOf(1f) }

    val painter = painterResource(shownImage)
    val ratio = painter.intrinsicSize.let {
        if (it.width > 0f && it.height > 0f) it.width / it.height else 2.2f
    }
    val windColor = if (theme.name == "NIGHT") Color.White else theme.subText

    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.BottomCenter) {
        val parentPx = constraints.maxWidth.toFloat()

        /* ---------- Wind streaks (one shared brush, scaled per streak) ---------- */
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, windColor, Color.Transparent),
                    startX = 0f, endX = 1f
                )
                onDrawBehind {
                    val k = windVisual()
                    if (k <= 0.02f) return@onDrawBehind
                    val w = size.width
                    val h = size.height
                    for (s in STREAKS) {
                        val len = s.len * w * (0.6f + k)
                        val p = (((s.phase - phase * s.speed) % 1f) + 1f) % 1f
                        val x = p * (w + len) - len
                        val y = h * (0.08f + 0.78f * s.y)
                        withTransform({
                            translate(x, y)
                            scale(len, 1f, Offset.Zero)
                        }) {
                            drawLine(
                                brush, Offset.Zero, Offset(1f, 0f),
                                strokeWidth = s.thick, alpha = s.alpha * k * 0.8f
                            )
                        }
                    }
                }
            }
        )

        /* ---------- Floor glow ---------- */
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .height(3.dp)
                .graphicsLayer { alpha = (1f - abs(offset.value)).coerceIn(0f, 1f) }
                .background(Brush.horizontalGradient(listOf(Color.Transparent, theme.accent, Color.Transparent)))
        )

        /* ---------- The car (moves as one unit) ---------- */
        Box(
            Modifier
                .padding(bottom = 6.dp)
                .fillMaxHeight()
                .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                .graphicsLayer {
                    translationX = offset.value * (parentPx / 2f + size.width / 2f)
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
                            center = Offset(w * WHEEL_X - life * w * 0.22f, h * GROUND_Y - life * h * 0.10f)
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

            // Headlights and tail glow: geometry and brushes are cached,
            // the pulse is only a layer alpha (no redraw, no allocations).
            // Static beam layer — alpha never pulsed so the beam length stays constant.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = lightsLevel *
                                (1f - abs(offset.value)).coerceIn(0f, 1f)
                    }
                    .drawWithCache {
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
                        val beamBrush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFF3D0).copy(alpha = 0.50f),
                                Color(0xFFFFF3D0).copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            startX = hx, endX = hx + beamLen
                        )

                        val tx = w * TAIL_X
                        val ty = h * TAIL_Y
                        val tailLen = w * 0.10f
                        val tailBrush = Brush.horizontalGradient(
                            listOf(Color(0xFFFF1E1E).copy(alpha = 0.50f), Color.Transparent),
                            startX = tx, endX = tx - tailLen
                        )

                        onDrawBehind {
                            drawPath(beam, beamBrush)
                            drawRect(
                                tailBrush,
                                topLeft = Offset(tx - tailLen, ty - h * 0.05f),
                                size = Size(tailLen, h * 0.10f)
                            )
                        }
                    }
            )

            // Glow-only layer — pulse only affects the radial glow intensity, not beam shape.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = lightsLevel *
                                (1f - abs(offset.value)).coerceIn(0f, 1f) *
                                (0.82f + 0.18f * pulse.value)
                    }
                    .drawWithCache {
                        val w = size.width
                        val h = size.height

                        val hx = w * HEAD_X
                        val hy = h * HEAD_Y
                        val headR = h * 0.12f
                        val headBrush = Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color(0xFFFFE9A8).copy(alpha = 0.40f),
                                Color.Transparent
                            ),
                            center = Offset(hx, hy), radius = headR
                        )

                        val tx = w * TAIL_X
                        val ty = h * TAIL_Y
                        val tailR = h * 0.14f
                        val tailGlow = Brush.radialGradient(
                            listOf(
                                Color(0xFFFF3B30).copy(alpha = 0.9f),
                                Color(0xFFFF1E1E).copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            center = Offset(tx, ty), radius = tailR
                        )

                        onDrawBehind {
                            drawCircle(headBrush, headR, Offset(hx, hy))
                            drawCircle(tailGlow, tailR, Offset(tx, ty))
                        }
                    }
            )
        }
    }
}