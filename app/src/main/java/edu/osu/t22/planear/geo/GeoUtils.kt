package edu.osu.t22.planear.geo

import edu.osu.t22.planear.adsb.AdsbAircraft
import kotlin.math.*

//sample 3D vector to use in renderer later
data class Vec3(
    val x: Double, //East meters
    val y: Double, //North meters
    val z: Double  //height meters
)

data class GeoPoint(
    val latDeg: Double, //latitude in degrees
    val lonDeg: Double, //longitude in degrees
    val altM: Double    //altitude in meters (MSL or AGL) doesn't matter just need to be consistent
                        // https://pilotinstitute.com/agl-vs-msl/ explanation of what this is
)

data class RelativeDirection(
    val distanceMeters: Double,
    val bearingToAircraft: Double, //absolute bearing, 0..360 0 being north
    val relativeBearingDeg: Double,//-180..180 0 being straight ahead
    val elevationDeg: Double       //angle above or below horizon
)

object GeoUtils {
    private const val EARTH_RADIUS_M = 6_371_000.0 //approximately
    //if issues arise other numbers that can be tested could be
    //6_378_137.0 equatorial radius
    //6_356_752.0 polar radius

    private fun degToRad(deg: Double) = deg * PI / 180.0
    private fun radToDeg(rad: Double) = rad * 180.0 / PI

    //Great-circle distance between two points on Earth using haversine

    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = degToRad(a.latDeg)
        val lat2 = degToRad(b.latDeg)
        val dLat = lat2 - lat1
        val dLon = degToRad(b.lonDeg - a.lonDeg)

        val sinLat = sin(dLat / 2)
        val sinLon = sin(dLon / 2)

        val h = sinLat * sinLat + cos(lat1) * cos(lat2) * sinLon * sinLon

        val c = 2.0 * atan2(sqrt(h), sqrt(1.0 - h))
        return EARTH_RADIUS_M * c
    }

    //initial bearing (forward azimuth) form point a to point b
    //0 = North, 90 = East, 180 = South, 270 = West
    fun bearingDeg(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = degToRad(a.latDeg)
        val lat2 = degToRad(b.latDeg)
        val dLon = degToRad(b.lonDeg - a.lonDeg)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var brng = radToDeg(atan2(y, x)) //-180 to 180
        if (brng < 0.0) brng += 360 //normalize 0 to 360
        return brng
    }

    //compute local East/North/height (ENH) vector from user to aircraft
    //this is a local tangent plane approximation, seems good for ranges like 50 - 100 NM
    //x = East, y = North, z = Height (all in meters)
    fun enhVector(user: GeoPoint, aircraft: GeoPoint): Vec3 {
        val lat0 = degToRad(user.latDeg)
        val lon0 = degToRad(user.lonDeg)
        val lat1 = degToRad(aircraft.latDeg)
        val lon1 = degToRad(aircraft.lonDeg)

        val dLat = lat1 - lat0
        val dLon = lon1 - lon0
        val dAlt = aircraft.altM - user.altM

        // small angle local ENH approximation
        val east = dLon * cos(lat0) * EARTH_RADIUS_M
        val north = dLat * EARTH_RADIUS_M
        val height = dAlt

        return Vec3(east, north, height)
    }

    //compute distances, bearing, relative bearing (to user heading) and elevation angle
    //userHeadingDeg heading the users device is facing 0 = North, 180 = South
    fun relativeDirection(
        user: GeoPoint,
        userHeadingDeg: Double,
        aircraft: GeoPoint
    ): RelativeDirection {
        val distance = distanceMeters(user, aircraft)
        val bearingToAircraft = bearingDeg(user, aircraft)

        //relative bearing -180 to 180 0 being straight ahead
        var relBearing = bearingToAircraft - userHeadingDeg
        //normalize to -180 to 180
        relBearing = (relBearing + 540.0) % 360.0 - 180.0

        //horizontal ground distance
        val enh =  enhVector(user, aircraft)
        val horizontal = hypot(enh.x, enh.y)

        //elevation angle positive is above horizon negative is below
        val elevationRad = atan2(enh.z, horizontal)
        val elevationDeg = radToDeg(elevationRad)

        return RelativeDirection(
            distanceMeters = distance,
            bearingToAircraft = bearingToAircraft,
            relativeBearingDeg = relBearing,
            elevationDeg = elevationDeg
        )
    }
}