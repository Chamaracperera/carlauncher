package com.example.carlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

class Telemetry(private val ctx: Context) : LocationListener {
    var speed by mutableIntStateOf(0)
    var heading by mutableStateOf("N")
    var tripKm by mutableFloatStateOf(0f)
    var driveMin by mutableIntStateOf(0)
    var latitude by mutableDoubleStateOf(6.9271)
    var longitude by mutableDoubleStateOf(79.8612)

    private var last: Location? = null
    private var lastFix = 0L
    private var meters = 0f
    private val startMs = System.currentTimeMillis()

    @SuppressLint("MissingPermission")
    fun start() {
        val lm = ctx.getSystemService(LocationManager::class.java)
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
        } catch (_: Exception) { }
    }

    fun stop() {
        try {
            ctx.getSystemService(LocationManager::class.java).removeUpdates(this)
        } catch (_: Exception) { }
    }

    override fun onLocationChanged(l: Location) {
        lastFix = System.currentTimeMillis()
        latitude = l.latitude
        longitude = l.longitude

        // Ignore GPS noise when parked (readings under 3 km/h count as stopped)
        val kmh = (l.speed * 3.6f).roundToInt()
        speed = if (kmh < 3) 0 else kmh

        if (l.hasBearing() && speed > 3) heading = compass(l.bearing)
        last?.let { if (speed > 2) meters += it.distanceTo(l) }
        last = l
        tripKm = meters / 1000f
        driveMin = ((System.currentTimeMillis() - startMs) / 60000).toInt()
    }

    /** If no GPS update arrived for 4 seconds, treat the car as stopped. */
    fun checkStale() {
        if (speed > 0 && System.currentTimeMillis() - lastFix > 4000) speed = 0
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}

    private fun compass(b: Float): String {
        val d = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return d[(((b + 22.5f) / 45f).toInt()) % 8]
    }
}