package com.example.carlauncher

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/** One entry per car colour. Add a new colour by adding one line to CarColors. */
data class CarColor(
    val id: String,
    val label: String,
    @DrawableRes val image: Int,
    val accent: Color,   // gauge, buttons, floor glow, dock highlight
    val glow: Color      // outer screen frame glow
)

val CarColors = listOf(
    CarColor("orange", "Orange", R.drawable.car_orange, Color(0xFFFF6A1A), Color(0xFF1E90FF)),
    CarColor("blue",   "Blue",   R.drawable.car_blue,   Color(0xFF2D8CFF), Color(0xFF2D8CFF)),
    CarColor("red",    "Red",    R.drawable.car_red,    Color(0xFFFF3B3B), Color(0xFFFF5A5A)),
    CarColor("white",  "White",  R.drawable.car_white,  Color(0xFF9FB4C8), Color(0xFF6FA8FF)),
    CarColor("pink",  "pink",  R.drawable.car_pink,  Color(0xFFEC0D67), Color(0xFFEC0D67)),
    CarColor("black",  "black",  R.drawable.car_black,  Color(0xFFFFFFFF), Color(0xFFFFFFFF)),

)

data class CarTheme(
    val name: String,            // "NIGHT" or "DAY", shown on the top-bar button
    val background: Color,
    val card: Color,
    val accent: Color,
    val glow: Color,
    val text: Color,
    val subText: Color,
    val track: Color,
    @DrawableRes val carImage: Int
)

fun buildTheme(car: CarColor, night: Boolean) = if (night) {
    CarTheme(
        name = "NIGHT",
        background = Color(0xFF0A0C11),
        card = Color(0xFF12151C),
        accent = car.accent,
        glow = car.glow,
        text = Color(0xFFFFFFFF),
        subText = Color(0xFF9AA0AB),
        track = Color(0xFF2A2E38),
        carImage = car.image
    )
} else {
    CarTheme(
        name = "DAY",
        background = Color(0xFFE9ECF2),
        card = Color(0xFFFFFFFF),
        accent = car.accent,
        glow = car.glow.copy(alpha = 0.6f),
        text = Color(0xFF11141A),
        subText = Color(0xFF626976),
        track = Color(0xFFD5D9E2),
        carImage = car.image
    )
}