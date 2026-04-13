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

    // Misc
    val squawk: String? = null // squawk code
) {
    val altitudeMeters: Double?
        get() {
            val feet = alt_baro?.toDoubleOrNull() ?: return null
            return feet * 0.3048
        }

    val isProjectable: Boolean
        get() = lat != null && lon != null && altitudeMeters != null

    val label: String
        get() = flight?.trim()?.takeIf { it.isNotEmpty() }
            ?: hex?.trim()?.takeIf { it.isNotEmpty() }
            ?: "UNKNOWN"
}