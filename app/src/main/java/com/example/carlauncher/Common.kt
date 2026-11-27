package com.example.carlauncher

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/* ---------------- INTENT HELPERS ---------------- */

fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { }
}

fun launchByCategory(ctx: Context, category: String) {
    ctx.startSafely(Intent(Intent.ACTION_MAIN).addCategory(category))
}

/* ---------------- PERFORMANCE HELPERS ---------------- */

/** Wall clock that ticks exactly on the second. Read it only where it is displayed. */
@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(1000L - value % 1000L)
    }
}

/**
 * Smoothly follows [target]. The returned State changes every frame, so read it
 * only inside draw / graphicsLayer lambdas. That way nothing recomposes.
 */
@Composable
fun rememberSmoothValue(
    durationMs: Int,
    easing: Easing = FastOutSlowInEasing,
    target: () -> Float
): State<Float> {
    val anim = remember { Animatable(0f) }
    val latest = rememberUpdatedState(target)
    LaunchedEffect(anim) {
        snapshotFlow { latest.value.invoke() }.collectLatest {
            anim.animateTo(it, tween(durationMs, easing = easing))
        }
    }
    return anim.asState()
}

/* ---------------- SHARED CARD ---------------- */

@Composable
fun CardBox(theme: CarTheme, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .clip(shape)
            .background(theme.card)
            .border(1.dp, theme.track.copy(alpha = 0.6f), shape)
            .padding(14.dp),
        content = content
    )
}