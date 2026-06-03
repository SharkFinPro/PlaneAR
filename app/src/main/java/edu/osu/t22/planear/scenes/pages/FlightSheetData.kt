package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.adsb.Aircraft
import edu.osu.t22.planear.scenes.SceneSwitcher

data class FlightSheetData(
    val callsign:       String,
    val callsignLabel:  String,   // "CALLSIGN", "REGISTRATION", or "ID"
    val registration:   String,
    val type:           String,
    val altitudeFt:     String,
    val speedKts:       String,
    val headingDeg:     String,
    val verticalRate:   String,   // e.g. "+1200 ft/min" or "-800 ft/min"
    val date:           String,
    val origin:         String,
    val destination:    String,
    val isLive:         Boolean
) {
    companion object {

        private const val NA = "N/A"

        private fun resolveCallsign(
            callsign: String?,
            registration: String?,
            id: String
        ): Pair<String, String> {
            // Returns Pair(displayValue, fieldLabel)
            return when {
                !callsign?.trim().isNullOrBlank()     -> callsign!!.trim() to "CALLSIGN"
                !registration?.trim().isNullOrBlank() -> registration!!.trim() to "REGISTRATION"
                else                                  -> id to "ID"
            }
        }

        private fun formatVerticalRate(fpm: Int): String = when {
            fpm >  100 -> "+$fpm ft/min ▲"
            fpm < -100 -> "$fpm ft/min ▼"
            else       -> "Cruising ~"
        }

        private fun formatHeading(degrees: Double?): String {
            degrees ?: return NA
            val cardinals = listOf("N","NE","E","SE","S","SW","W","NW")
            val cardinal = cardinals[((degrees + 22.5) / 45.0).toInt() % 8]
            return "${degrees.toInt()}° $cardinal"
        }

        fun fromAircraft(aircraft: Aircraft): FlightSheetData {
            val (csValue, csLabel) = resolveCallsign(
                aircraft.callsign, aircraft.registration, aircraft.id
            )
            return FlightSheetData(
                callsign      = csValue,
                callsignLabel = csLabel,
                registration  = aircraft.registration?.takeIf { it.isNotBlank() } ?: NA,
                type          = aircraft.longType?.takeIf { it.isNotBlank() } ?: aircraft.type?.takeIf { it.isNotBlank() } ?: NA,
                altitudeFt    = "%.0f ft".format(aircraft.altitudeSeaLevel),
                speedKts      = "${aircraft.groundSpeed?.toInt() ?: NA} kts",
                headingDeg    = formatHeading(aircraft.headingDegrees),
                verticalRate  = formatVerticalRate(aircraft.verticalRate),
                date          = NA,
                origin        = aircraft.origin?.takeIf { it.isNotBlank() } ?: NA,
                destination   = aircraft.destination?.takeIf { it.isNotBlank() } ?: NA,
                isLive        = true
            )
        }

        fun fromEntry(entry: FlightEntry): FlightSheetData {
            val live = SceneSwitcher.adsbManager
                .getRepository()
                .getAircraft()
                .firstOrNull { it.label == entry.callsign }

            val (csValue, csLabel) = resolveCallsign(
                live?.callsign ?: entry.callsign,
                live?.registration ?: entry.registration,
                entry.callsign
            )

            return FlightSheetData(
                callsign      = csValue,
                callsignLabel = csLabel,
                registration  = live?.registration?.takeIf { it.isNotBlank() }
                    ?: entry.registration.takeIf { it.isNotBlank() }
                    ?: NA,
                origin      = live?.origin?.takeIf { it.isNotBlank() } ?: NA,
                destination = live?.destination?.takeIf { it.isNotBlank() } ?: NA,
                type          = live?.type?.takeIf { it.isNotBlank() }
                    ?: entry.planeType.takeIf { it.isNotBlank() }
                    ?: NA,
                altitudeFt    = live?.let { "%.0f ft".format(it.altitudeSeaLevel) } ?: NA,
                speedKts      = live?.groundSpeed?.let { "${it.toInt()} kts" }
                    ?: "${entry.airspeed} kts",
                headingDeg    = formatHeading(live?.headingDegrees),
                verticalRate  = live?.let { formatVerticalRate(it.verticalRate) } ?: NA,
                date          = entry.date,
                isLive        = live != null
            )
        }
    }
}