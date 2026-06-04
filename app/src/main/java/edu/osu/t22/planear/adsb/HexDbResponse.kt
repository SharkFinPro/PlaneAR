package edu.osu.t22.planear.adsb

data class HexDbRouteResponse(
    val flight: String? = null,
    val route: String? = null,
    val updatetime: Long? = null,
    val status: String? = null,
    val error: String? = null
)

data class HexDbAircraftResponse(
    val ICAOTypeCode: String? = null,
    val Manufacturer: String? = null,
    val ModeS: String? = null,
    val OperatorFlagCode: String? = null,
    val RegisteredOwners: String? = null,
    val Registration: String? = null,
    val Type: String? = null,
    val status: String? = null,
    val error: String? = null
)
