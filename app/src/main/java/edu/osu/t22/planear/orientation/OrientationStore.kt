package edu.osu.t22.planear.orientation

data class OrientationData(
    val azimuthDeg: Double,
    val pitchDeg: Double,
    val rollDeg: Double,
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun getCardinalDirection(): String {
        return when (azimuthDeg) {
            in 337.5..360.0, in 0.0..22.5 -> "N"
            in 22.5..67.5 -> "NE"
            in 67.5..112.5 -> "E"
            in 112.5..157.5 -> "SE"
            in 157.5..202.5 -> "S"
            in 202.5..247.5 -> "SW"
            in 247.5..292.5 -> "W"
            in 292.5..337.5 -> "NW"
            else -> "?"
        }
    }
}

object OrientationStore {
    @Volatile
    var data: OrientationData = OrientationData(0.0, 0.0, 0.0, 0.0f, 0.0f, 0.0f)
}
