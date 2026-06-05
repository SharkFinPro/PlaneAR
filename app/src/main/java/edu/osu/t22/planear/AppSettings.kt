package edu.osu.t22.planear

import android.hardware.HardwareBuffer

object AppSettings {
    /** Search radius in nautical miles. */
    var searchRadiusNm: Int = 50

    /** When true, AppColors.current returns the dark palette. */
    var darkMode: Boolean = false

    var cameraIsEnabled: Boolean = false

    var canEnableCamera: Boolean = true

    var hasCameraPermissions: Boolean = false

    var useMetric: Boolean = false

    var useAltApi: Boolean = false
}