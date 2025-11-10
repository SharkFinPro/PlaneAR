package edu.osu.t22.planear.adsb

data class AdsbResponse(
    val ac: List<AdsbAircraft>,
    val now: Long, // when file was generated
    val total: Int, // amount of aircraft returned
    val cTime: Long,
    val pTime: Long
)

data class AdsbAircraft(
    val hex: String, // ICAO address
    val flight: String, // callsign
    val r: String, // regristration
    val t: String, // type
    val alt_baro: String,
    val alt_geom: Int,
    val gs: Float,
    val ias: Int,
    val tas: Int,
    val mach: Double,
    val track: Double,
    val track_rate: Double,
    val lat: Double,
    val lon: Double,
    val seen: Float,
    val seen_pos: Float,
    val squawk: String
)