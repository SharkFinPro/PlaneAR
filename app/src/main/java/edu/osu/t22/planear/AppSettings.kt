package edu.osu.t22.planear

import android.hardware.HardwareBuffer

object AppSettings {
    /** Search radius in nautical miles. */
    var searchRadiusNm: Int = 10

    /** When true, AppColors.current returns the dark palette. */
    var darkMode: Boolean = false

    var hb: HardwareBuffer? = null
}