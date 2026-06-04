package edu.osu.t22.planear.orientation

data class OrientationData(
    val azimuthDeg: Double,
    val pitchDeg: Double,
    val rollDeg: Double,
    val x: Float,
    val y: Float,
    val z: Float,
    // Raw 3×3 rotation matrix from SensorManager.getRotationMatrixFromVector,
    // stored row-major as 9 floats.  Passed directly to set3DViewMatrix() so
    // the GPU view matrices are built without going through Euler angles.
    val rotationMatrix: FloatArray = FloatArray(9) { if (it % 4 == 0) 1f else 0f }
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
