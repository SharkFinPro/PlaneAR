package edu.osu.t22.planear

object Units {

    fun formatAltitude(ft: Double): String {
        return if (AppSettings.useMetric) {
            "%.0f m".format(ft * 0.3048)
        } else {
            "%.0f ft".format(ft)
        }
    }

    fun formatSpeed(kts: Int): String {
        return if (AppSettings.useMetric) {
            "${(kts * 1.852).toInt()} km/h"
        } else {
            "$kts kts"
        }
    }

    fun formatVerticalRate(fpm: Int): String {
        return when {
            fpm >  100 -> if (AppSettings.useMetric) "+${"%.0f".format(fpm * 0.00508)} m/s ▲"
                          else "+$fpm ft/min ▲"
            fpm < -100 -> if (AppSettings.useMetric) "${"%.0f".format(fpm * 0.00508)} m/s ▼"
                          else "$fpm ft/min ▼"
            else       -> "Cruising ~"
        }
    }

    fun formatDistance(meters: Double): String {
        return if (AppSettings.useMetric) {
            if (meters < 1000.0) "${"%.0f".format(meters)} m"
            else "${"%.1f".format(meters / 1000.0)} km"
        } else {
            val nm = meters / 1852.0
            if (nm < 1.0) "${"%.0f".format(meters * 3.28084)} ft"
            else "${"%.1f".format(nm)} nm"
        }
    }
}
