package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.adsb.AircraftPosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.SharedPreferences
import edu.osu.t22.planear.adsb.Aircraft

// Shared flight data
data class FlightEntry(
    val callsign: String,
    val takeoffTime: String,
    val landingTime: String,
    val planeType: String,
    val airspeed: Int,
    val date: String
)

val flightData: MutableList<FlightEntry> = mutableListOf()

object FlightHistoryStore {
    private const val PREFS_NAME = "planear_flight_history"
    private const val KEY_ENCODED_FLIGHTS = "encoded_flights"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        load()
    }

    private fun load() {
        val encodedSet = prefs.getStringSet(KEY_ENCODED_FLIGHTS, emptySet()) ?: emptySet()

        flightData.clear()
        Page.flightFavorites.clear()

        // Decode each string: "index|callsign|takeoffTime|landingTime|planeType|airspeed|date|isFavorite"
        val parsedList = encodedSet.mapNotNull { str ->
            val parts = str.split("|", limit = 8)
            if (parts.size == 8) {
                val index = parts[0].toIntOrNull() ?: 0
                val entry = FlightEntry(
                    callsign = parts[1],
                    takeoffTime = parts[2],
                    landingTime = parts[3],
                    planeType = parts[4],
                    airspeed = parts[5].toIntOrNull() ?: 0,
                    date = parts[6]
                )
                val isFav = parts[7] == "true"
                Triple(index, entry, isFav)
            } else null
        }.sortedBy { it.first }

        for (item in parsedList) {
            flightData.add(item.second)
            Page.flightFavorites.add(item.third)
        }

        while (Page.flightFavorites.size < flightData.size) {
            Page.flightFavorites.add(false)
        }
    }

    fun save() {
        val encodedSet = mutableSetOf<String>()
        for (i in 0 until flightData.size) {
            val f = flightData[i]
            val isFav = if (i < Page.flightFavorites.size) Page.flightFavorites[i] else false
            val encoded = "$i|${f.callsign}|${f.takeoffTime}|${f.landingTime}|${f.planeType}|${f.airspeed}|${f.date}|$isFav"
            encodedSet.add(encoded)
        }

        prefs.edit()
            .putStringSet(KEY_ENCODED_FLIGHTS, encodedSet)
            .apply()
    }
}

fun logFlightHistory(plane: Aircraft) {
    if (flightData.none { it.callsign == plane.label }) {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val dateStr = dateFormat.format(Date())
        val timeStr = timeFormat.format(Date())

        val speed = plane.groundSpeed?.toInt() ?: 0
        val type = plane.type ?: "Unknown"

        flightData.add(0, FlightEntry(
            callsign = plane.label,
            takeoffTime = timeStr,
            landingTime = "Unknown",
            planeType = type,
            airspeed = speed,
            date = dateStr
        ))
        Page.flightFavorites.add(0, false)
        FlightHistoryStore.save()
    }
}
