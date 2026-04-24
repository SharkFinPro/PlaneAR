package edu.osu.t22.planear.adsb

import edu.osu.t22.planear.geo.GeoPoint

data class AircraftScreenPoint(
    val x: Float,
    val y: Float,
    val label: String
)

// Raw aircraft data for re-projection
data class AircraftPosition(
    val position: GeoPoint,
    val label: String
)

object AircraftOverlayStore {
    @Volatile
    var points: List<AircraftScreenPoint> = emptyList()

    // Raw aircraft positions - updated by ADSB polling
    @Volatile
    var aircraftData: List<AircraftPosition> = emptyList()
}