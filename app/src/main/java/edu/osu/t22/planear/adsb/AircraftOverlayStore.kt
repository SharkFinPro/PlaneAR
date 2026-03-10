package edu.osu.t22.planear.adsb

data class AircraftScreenPoint(
    val x: Float,
    val y: Float,
    val label: String
)

object AircraftOverlayStore {
    @Volatile
    var points: List<AircraftScreenPoint> = emptyList()
}