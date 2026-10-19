package com.saeed.mirror

/** App-wide settings you are expected to edit. Mirrors App/Config.swift on iOS. */
object Config {
    /** Your try-on backend. While null, the app uses MockTryOnService. */
    val tryOnEndpoint: String? = null

    /** ML Kit's head yaw sign. If the app asks you to turn the wrong way, change to -1f. */
    const val YAW_SIGN = 1f

    /** Degrees the head must turn before a side photo counts. */
    const val SIDE_YAW_THRESHOLD = 35f

    /** How close to straight-on the front photo must be, in degrees. */
    const val FRONT_YAW_TOLERANCE = 12f

    /** How long the head must stay lined up before the photo is taken automatically. */
    const val HOLD_STILL_MS = 800L
}
