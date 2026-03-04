package edu.osu.t22.planear.models

// Define this here so you can use it for both Mocking
// and the real Retrofit API later.
data class FlightData(
    val callsign: String,
    val takeoffTime: String,
    val landingTime: String,
    val planeType: String,
    val airspeed: Int,       // knots
    val altitude: Int,       // feet
    val date: String         // display date string
)