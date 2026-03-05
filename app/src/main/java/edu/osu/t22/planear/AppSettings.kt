package edu.osu.t22.planear

object AppSettings {
    /** Search radius in nautical miles. */
    var searchRadiusNm: Int = 10

    /** Convert nm to km for use with the ADSB API (which expects km). */
    val searchRadiusKm: Int get() = (searchRadiusNm * 1.852).toInt()
}