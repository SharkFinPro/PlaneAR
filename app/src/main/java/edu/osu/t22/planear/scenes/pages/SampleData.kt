package edu.osu.t22.planear.scenes.pages

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.SharedPreferences
import edu.osu.t22.planear.adsb.Aircraft

// Shared flight data
data class FlightEntry(
    val callsign:     String,
    val origin:  String,
    val destination:  String,
    val planeType:    String,
    val airspeed:     Int,
    val verticalRate: Int,       // ft/min
    val date:         String,
    val registration: String = ""
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

        // Format: "index|callsign|origin|destination|planeType|airspeed|verticalRate|registration|date|isFavorite"
        val parsedList = encodedSet.mapNotNull { str ->
            val parts = str.split("|", limit = 10)
            if (parts.size == 10) {
                val index = parts[0].toIntOrNull() ?: 0
                val entry = FlightEntry(
                    callsign = parts[1],
                    origin = parts[2],
                    destination = parts[3],
                    planeType = parts[4],
                    airspeed = parts[5].toIntOrNull() ?: 0,
                    verticalRate = parts[6].toIntOrNull() ?: 0,
                    registration = parts[7],
                    date = parts[8]
                )
                val isFav = parts[9] == "true"
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
            val encoded = "$i|${f.callsign}|${f.origin}|${f.destination}|" +
                    "${f.planeType}|${f.airspeed}|${f.verticalRate}|" +
                    "${f.registration}|${f.date}|$isFav"
            encodedSet.add(encoded)
        }

        prefs.edit().putStringSet(KEY_ENCODED_FLIGHTS, encodedSet).apply()
    }

}

fun logFlightHistory(plane: Aircraft) {
    if (flightData.none { it.callsign == plane.label }) {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        flightData.add(
            0, FlightEntry(
            callsign = plane.label,
            origin = plane.origin?.takeIf { it.isNotBlank() } ?: "",
            destination = plane.destination?.takeIf { it.isNotBlank() } ?: "",
            planeType = plane.longType?.takeIf { it.isNotBlank() } ?: plane.type?.takeIf { it.isNotBlank() } ?: "Unknown",
            airspeed = plane.groundSpeed?.toInt() ?: 0,
            verticalRate = plane.verticalRate,
            registration = plane.registration?.takeIf { it.isNotBlank() } ?: "",
            date = dateFormat.format(Date())
        ))
        Page.flightFavorites.add(0, false)
        FlightHistoryStore.save()
    }
}

