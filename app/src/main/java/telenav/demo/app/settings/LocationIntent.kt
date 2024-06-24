package telenav.demo.app.settings

import android.location.Location

/**
 * Intents to change location settings
 *
 * @since 2022-05-27
 */
sealed class LocationIntent

data class CvpFollowGPS(val status: Boolean): LocationIntent()

data class SalFollowGPS(val status: Boolean): LocationIntent()

data class CvpChange(val location: Location): LocationIntent()

data class SalChange(val location: Location): LocationIntent()

data class SalFollowCVP(val status: Boolean): LocationIntent()