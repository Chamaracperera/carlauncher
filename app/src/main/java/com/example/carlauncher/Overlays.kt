package com.example.carlauncher

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

/* ---------------- SETTINGS POPUP ---------------- */

@Composable
fun SettingsPopup(
    theme: CarTheme,
    state: LauncherState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .shadow(16.dp, shape)
            .clip(shape)
            .background(theme.card)
            .border(1.dp, theme.track, shape)
            .width(400.dp)
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
                    modifier = Modifier.size(24.dp).clip(CircleShape).clickable(onClick = onClose)
                )
            }

            // 1. Theme / style picker: built from the registry, so new styles appear automatically
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("1. Pick a Theme", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStyles.all.forEach { s ->
                        val sel = state.styleId == s.id
                        val cardShape = RoundedCornerShape(14.dp)
                        Column(
                            Modifier
                                .width(112.dp)
                                .clip(cardShape)
                                .background(if (sel) theme.accent.copy(alpha = 0.12f) else theme.background)
                                .border(
                                    if (sel) 2.dp else 1.dp,
                                    if (sel) theme.accent else theme.track,
                                    cardShape
                                )
                                .clickable { state.setStyle(s.id) }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(s.icon, null, tint = if (sel) theme.accent else theme.text, modifier = Modifier.size(22.dp))
                                if (sel) Icon(Icons.Filled.CheckCircle, null, tint = theme.accent, modifier = Modifier.size(16.dp))
                            }
                            Text(s.label, color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            // minLines keeps every card the same height, even for short descriptions
                            Text(s.description, color = theme.subText, fontSize = 11.sp, maxLines = 2, minLines = 2)
                        }
                    }
                }
            }

            // 2. Vehicle colour
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("2. Vehicle Colour", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CarColors.forEach { c ->
                        val selected = c.id == state.carId
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { state.setCar(c.id) }
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

            // 3. Owner name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("3. Owner Name", color = theme.subText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = state.ownerName,
                    onValueChange = state::updateOwnerName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Enter owner name", color = theme.subText) },
                    textStyle = TextStyle(color = theme.text, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
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

/* ---------------- APP DRAWER ---------------- */

@Composable
fun AppDrawer(
    theme: CarTheme,
    title: String = "All apps",
    onPick: ((String) -> Unit)? = null,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    // Loaded once. Icons are rasterised at 96px to keep the grid light.
    val apps = remember {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { Triple(it.loadLabel(pm).toString(), it.loadIcon(pm).toBitmap(96, 96).asImageBitmap(), it.activityInfo.packageName) }
            .sortedBy { it.first.lowercase() }
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
            items(apps, key = { it.third }) { (label, icon, pkg) ->
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