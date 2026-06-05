package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.Units
import edu.osu.t22.planear.adsb.Aircraft
import edu.osu.t22.planear.scenes.SceneSwitcher

data class FlightSheetData(
    val callsign:        String,
    val callsignLabel:   String,   // "CALLSIGN", "REGISTRATION", or "ID"
    val registration:    String,
    val type:            String,
    val altitudeRawFt:   Double,   // raw feet; format via Units.formatAltitude
    val speedRawKts:     Int,      // raw knots; format via Units.formatSpeed
    val headingDeg:      String,
    val verticalRateFpm: Int,      // raw ft/min; format via Units.formatVerticalRate
    val date:            String,
    val origin:          String,
    val destination:     String,
    val isLive:          Boolean
) {
    val altitude:     String get() = if (altitudeRawFt  == NA_DOUBLE) NA else Units.formatAltitude(altitudeRawFt)
    val speed:        String get() = if (speedRawKts    == NA_INT)    NA else Units.formatSpeed(speedRawKts)
    val verticalRate: String get() = if (verticalRateFpm == NA_INT)   NA else Units.formatVerticalRate(verticalRateFpm)

    companion object {

        const val NA = "N/A"
        const val NA_DOUBLE = Double.MIN_VALUE
        const val NA_INT    = Int.MIN_VALUE

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
                callsign        = csValue,
                callsignLabel   = csLabel,
                registration    = aircraft.registration?.takeIf { it.isNotBlank() } ?: NA,
                type            = aircraft.longType?.takeIf { it.isNotBlank() } ?: aircraft.type?.takeIf { it.isNotBlank() } ?: NA,
                altitudeRawFt   = aircraft.altitudeSeaLevel,
                speedRawKts     = aircraft.groundSpeed?.toInt() ?: NA_INT,
                headingDeg      = formatHeading(aircraft.headingDegrees),
                verticalRateFpm = aircraft.verticalRate,
                date            = NA,
                origin          = aircraft.origin?.takeIf { it.isNotBlank() } ?: NA,
                destination     = aircraft.destination?.takeIf { it.isNotBlank() } ?: NA,
                isLive          = true
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
                callsign        = csValue,
                callsignLabel   = csLabel,
                registration    = live?.registration?.takeIf { it.isNotBlank() }
                    ?: entry.registration.takeIf { it.isNotBlank() }
                    ?: NA,
                origin          = live?.origin?.takeIf { it.isNotBlank() }
                    ?: entry.origin.takeIf {it.isNotBlank()}
                    ?: NA,
                destination     = live?.destination?.takeIf { it.isNotBlank() }
                    ?: entry.destination.takeIf {it.isNotBlank()}
                    ?: NA,
                type            = live?.type?.takeIf { it.isNotBlank() }
                    ?: entry.planeType.takeIf { it.isNotBlank() }
                    ?: NA,
                altitudeRawFt   = live?.altitudeSeaLevel ?: NA_DOUBLE,
                speedRawKts     = live?.groundSpeed?.toInt() ?: NA_INT,
                headingDeg      = formatHeading(live?.headingDegrees),
                verticalRateFpm = live?.verticalRate ?: NA_INT,
                date            = entry.date,
                isLive          = live != null
            )
        }
    }
}