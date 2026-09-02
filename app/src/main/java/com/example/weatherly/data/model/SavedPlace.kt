package com.example.weatherly.data.model

/** A place the user has searched for and saved. */
data class SavedPlace(
    val name: String,
    val admin: String?,
    val country: String?,
    val lat: Double,
    val lon: Double,
    // IANA zone id (e.g. "America/New_York") from the geocoding result — lets the locations sheet
    // show each saved/searched place's local time with no separate fetch. Default required: this
    // app persists SavedPlace as JSON via Moshi's reflective adapter (PreferencesStore), which
    // only fills a missing key from a constructor default — a place saved before this field
    // existed simply has no local-time display until it's re-searched and re-added.
    val timezone: String? = null
) {
    /** "New Jersey, United States" style subtitle. */
    val subtitle: String
        get() = listOfNotNull(admin, country).distinct().joinToString(", ")
}
