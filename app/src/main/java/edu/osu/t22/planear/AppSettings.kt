package edu.osu.t22.planear

import android.hardware.HardwareBuffer

object AppSettings {
    /** Search radius in nautical miles. */
    var searchRadiusNm: Int = 50

    /** When true, AppColors.current returns the dark palette. */
    var darkMode: Boolean = false

    @Volatile var hb: HardwareBuffer? = null
    @Volatile var hbImage: android.media.Image? = null

    var cameraIsEnabled: Boolean = false

    var canEnableCamera: Boolean = true
}