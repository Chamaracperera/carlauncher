package com.example.carlauncher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.math.roundToInt

// Change these to your city
const val WEATHER_CITY = "Colombo"
const val WEATHER_LAT = 6.9271
const val WEATHER_LON = 79.8612

data class Weather(
    val now: Int,
    val high: Int,
    val low: Int,
    val condition: String,
    val sunset: String
)

sealed class WeatherState {
    object Loading : WeatherState()
    data class Ok(val data: Weather) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

suspend fun fetchWeather(lat: Double, lon: Double): WeatherState = withContext(Dispatchers.IO) {
    var conn: HttpURLConnection? = null
    try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,sunset" +
                "&timezone=auto&forecast_days=1"

        conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "CarLauncher/1.0")
        }

        if (conn.responseCode != 200) {
            return@withContext WeatherState.Error("Server error ${conn.responseCode}")
        }

        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val j = JSONObject(text)
        val cur = j.getJSONObject("current")
        val daily = j.getJSONObject("daily")

        WeatherState.Ok(
            Weather(
                now = cur.getDouble("temperature_2m").roundToInt(),
                high = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                low = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                condition = codeToText(cur.getInt("weather_code")),
                sunset = daily.getJSONArray("sunset").getString(0).substringAfter("T")
            )
        )
    } catch (e: UnknownHostException) {
        WeatherState.Error("No internet")
    } catch (e: SocketTimeoutException) {
        WeatherState.Error("Timed out")
    } catch (e: SSLException) {
        WeatherState.Error("Secure connection failed")
    } catch (e: Exception) {
        WeatherState.Error(e.javaClass.simpleName)
    } finally {
        conn?.disconnect()
    }
}

private fun codeToText(c: Int) = when (c) {
    0, 1 -> "Clear"
    2 -> "Partly cloudy"
    3 -> "Cloudy"
    in 45..48 -> "Fog"
    in 51..67 -> "Rain"
    in 71..77 -> "Snow"
    in 80..82 -> "Showers"
    in 95..99 -> "Storm"
    else -> "Clear"
}