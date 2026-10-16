package com.railsathi.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val STATUS_ARG = "train"
    const val STATUS_PATTERN = "status?train={train}"
    const val AVAILABILITY = "availability"
    const val PNR = "pnr"
    const val FAVOURITES = "favourites"
    const val HISTORY = "history"
    const val PRIVACY = "privacy"

    /** Route to the running-status screen, optionally pre-filled with a train number. */
    fun status(trainNumber: String? = null): String =
        if (trainNumber.isNullOrBlank()) "status" else "status?train=$trainNumber"
}
