package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.adsb.AircraftPosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

fun logFlightHistory(plane: AircraftPosition) {
    if (flightData.none { it.callsign == plane.label }) {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val dateStr = dateFormat.format(Date())
        val timeStr = timeFormat.format(Date())

        val speed = plane.rawData?.gs?.toInt() ?: 0
        val type = plane.rawData?.t ?: "Unknown"

        flightData.add(0, FlightEntry(
            callsign = plane.label,
            takeoffTime = timeStr,
            landingTime = "Unknown",
            planeType = type,
            airspeed = speed,
            date = dateStr
        ))
        Page.flightFavorites.add(0, false)
    }
}
