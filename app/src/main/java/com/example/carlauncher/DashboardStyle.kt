package com.example.carlauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One dashboard "theme" = its own layout + its own background.
 * To add a new one: create a file with an `object MyStyle : DashboardStyle`
 * and add it to [DashboardStyles.all]. Nothing else needs to change.
 */
interface DashboardStyle {
    /** Saved in SharedPreferences. Never change it after release. */
    val id: String
    val label: String
    val description: String
    val icon: ImageVector

    /**
     * Full-frame background. Called once per colour/theme change (not per frame).
     * Use Modifier.drawWithCache inside so paths and brushes are built once.
     */
    fun background(theme: CarTheme): Modifier

    /** The whole home screen of this style. */
    @Composable
    fun Content(theme: CarTheme, env: DashboardEnv)
}

/** Everything a style may need. Created once, so it never causes extra recomposition. */
@Stable
class DashboardEnv(
    val state: LauncherState,
    val telemetry: Telemetry,
    val media: MediaState,
    val onRadio: () -> Unit,
    val onRadioLong: () -> Unit,
    val onApps: () -> Unit,
    val onSettings: () -> Unit
) {
    var weather by mutableStateOf<WeatherState>(WeatherState.Loading)
}

object DashboardStyles {
    /** Add new styles here. The first one is the fallback. */
    val all: List<DashboardStyle> = listOf(DefaultStyle, CockpitStyle, AuroraStyle)

    fun byId(id: String): DashboardStyle = all.firstOrNull { it.id == id } ?: all.first()
}