package com.example.carlauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

private val KNOWN_RADIO_PACKAGES = listOf(
    "com.android.fmradio",
    "com.mediatek.fmradio",
    "com.caf.fmradio",
    "com.syu.radio",
    "com.microntek.radio",
    "com.nwd.radio"
)

/** Finds the radio app: your saved choice, then known packages, then any app named "radio" or "FM". */
fun findRadioPackage(ctx: Context, prefs: SharedPreferences): String? {
    val pm = ctx.packageManager

    prefs.getString("radio_pkg", null)?.let {
        if (pm.getLaunchIntentForPackage(it) != null) return it
    }

    KNOWN_RADIO_PACKAGES.firstOrNull { pm.getLaunchIntentForPackage(it) != null }
        ?.let { return it }

    val fmWord = Regex("\\bfm\\b")
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcherIntent, 0)
        .firstOrNull { r ->
            val label = r.loadLabel(pm).toString().lowercase()
            val pkg = r.activityInfo.packageName.lowercase()
            pkg != ctx.packageName &&
                    (label.contains("radio") || pkg.contains("radio") || fmWord.containsMatchIn(label))
        }
        ?.activityInfo?.packageName
}

/** Returns true if a radio app was opened. */
fun launchRadio(ctx: Context, prefs: SharedPreferences): Boolean {
    val pkg = findRadioPackage(ctx, prefs) ?: return false
    val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
    ctx.startSafely(intent)
    return true
}