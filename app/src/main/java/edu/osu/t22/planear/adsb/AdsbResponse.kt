package edu.osu.t22.planear.adsb

data class AdsbResponse(
    val ac: List<AdsbAircraft>,
    val now: Long, // when file was generated
    val total: Int, // amount of aircraft returned
    val cTime: Long, // when data was last updated
    val pTime: Long // time to process response
)

data class AdsbAircraft(
    // Identifiers
    val hex: String? = "", // ICAO address
    val flight: String? = null, // callsign
    val r: String? = null, // regristration
    val t: String? = null, // aircraft type

    // Altitude
    val alt_baro: String? = null, // barometric altitude
    val alt_geom: Int? = null, // geometric altitude

    // Speed
    val gs: Float? = null, // ground speed
    val ias: Int? = null, // indicated air speed
    val tas: Int? = null, // true air speed
    val mach: Double? = null, // mach number

    // Heading
    val track: Double? = null, // ground track angle
    val track_rate: Double? = null, // rate of change for track

    // Position
    val lat: Double? = null, // latitude in decimal degrees
    val lon: Double? = null, // longitude in decimal degrees

    // Visibility
    val seen: Float? = null, // seconds since this aircraft was last seen
    val seen_pos: Float? = null, // seconds since this aircraft's position was updated

    // Vertical rate
    val baro_rate: Int? = null, // vertical rate in ft/min (barometric)

    // Misc
    val squawk: String? = null // squawk code
)