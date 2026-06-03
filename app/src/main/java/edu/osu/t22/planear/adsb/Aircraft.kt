package edu.osu.t22.planear.adsb

import edu.osu.t22.planear.geo.GeoPoint
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Aircraft (
    val id: String
) {
    var isActive: Boolean = false
    var enrichmentState: EnrichmentState = EnrichmentState.NONE

    var callsign: String? = null
    var type: String? = null
    var longType: String? = null
    var registration: String? = null

    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var altitudeSeaLevel: Double = 0.0
    var altitudeGround: Double = 0.0

    var headingDegrees: Double? = null
    var groundSpeed: Double? = null
    var verticalRate: Int = 0

    // temp values that can change/be removed once Owen wants
    var origin: String? = null
    var destination: String? = null

    var seenSeconds: Float? = null
    var seenPosSeconds: Float? = null
    var lastRecieveTime: Long = 0L

    var origin: String? = null
    var destination: String? = null

    val label: String
        get() = callsign ?: registration ?: id

    val altitudeMeters: Double
        get() = altitudeSeaLevel * 0.3048

    val positionTimeStamp: Long
        get() = lastRecieveTime - ((seenPosSeconds ?: 0f) * 1000f).toLong()

    constructor(id: String, callsign: String, type: String, speed: Double, altitude: Double) : this(id) {
        this.callsign = callsign
        this.type = type
        this.groundSpeed = speed
        this.altitudeSeaLevel = altitude
        this.altitudeGround = altitude
    }

    fun updateFrom(adsb: AdsbAircraft, recieveTime: Long = System.currentTimeMillis()) {
        isActive = adsb.lat != null && adsb.lon != null && adsb.alt_baro != null
        latitude = adsb.lat ?: latitude
        longitude = adsb.lon ?: longitude
        altitudeSeaLevel = if (adsb.alt_baro == "ground") 0.0 else adsb.alt_baro?.toDoubleOrNull() ?: altitudeSeaLevel
        altitudeGround = adsb.alt_geom?.toDouble() ?: altitudeGround
        headingDegrees = adsb.track ?: headingDegrees
        groundSpeed = adsb.gs?.toDouble() ?: groundSpeed
        callsign = adsb.flight?.trim()
        type = adsb.t?.trim()
        registration = adsb.r?.trim()

        seenSeconds = adsb.seen
        seenPosSeconds = adsb.seen_pos

        lastRecieveTime = recieveTime

        verticalRate = adsb.baro_rate ?: verticalRate
    }

    fun getPosition(now: Long = System.currentTimeMillis()): GeoPoint {
        val dt = (now - positionTimeStamp) / 1000.0
        val speedKnots = groundSpeed
        val headingDeg = headingDegrees
        if (speedKnots == null || headingDeg == null) {
            return GeoPoint(
                latDeg = latitude,
                lonDeg = longitude,
                altM = altitudeMeters
            )
        }

        val speedMps = speedKnots * 0.514444
        val distance = speedMps * dt // meters traveled in time

        val earthR = 6371000.0

        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val bearing = Math.toRadians(headingDeg)

        val angularDistance = distance / earthR

        val lat2 = asin(
        sin(lat1) * cos(angularDistance) +
            cos(lat1) * sin(angularDistance) * cos(bearing)
        )

        val lon2 = lon1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2)
        )

        val altitude = altitudeSeaLevel + (verticalRate * (dt / 60.0))

        return GeoPoint(
            latDeg = Math.toDegrees(lat2),
            lonDeg = ((Math.toDegrees(lon2) + 540) % 360) - 180,
            altM = altitude * 0.3048
        )
    }

    fun setRoute(routeValue: String?) {
        val route = routeValue?.trim()
        val parts = route?.split("-")
        if (parts != null && parts.size >= 2) {
            origin = parts.first().trim()
            destination = parts.last().trim()
        }
    }
}