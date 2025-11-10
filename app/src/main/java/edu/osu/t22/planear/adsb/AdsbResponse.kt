package edu.osu.t22.planear.adsb

data class AdsbResponse(
    val ac: List<AdsbAircraft>,
    val now: Long, // when file was generated
    val total: Int, // amount of aircraft returned
    val cTime: Long, // unknown
    val pTime: Long // unknown
)

data class AdsbAircraft(
    // Identifiers
    val hex: String, // ICAO address
    val flight: String, // callsign
    val r: String, // regristration
    val t: String, // aircraft type

    // Altitude
    val alt_baro: String, // barometric altitude
    val alt_geom: Int, // geometric altitude

    // Speed
    val gs: Float, // ground speed
    val ias: Int, // indicated air speed
    val tas: Int, // true air speed
    val mach: Double, // mach number

    // Heading
    val track: Double, // ground track angle
    val track_rate: Double, // rate of change for track

    // Position
    val lat: Double, // latitude in decimal degrees
    val lon: Double, // longitude in decimal degrees

    // Visibility
    val seen: Float, // seconds since this aircraft was last seen
    val seen_pos: Float, // seconds since this aircraft's position was updated

    // Misc
    val squawk: String // squawk code
)