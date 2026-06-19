package com.example.weatherly.data.model

/** A place the user has searched for and saved. */
data class SavedPlace(
    val name: String,
    val admin: String?,
    val country: String?,
    val lat: Double,
    val lon: Double
) {
    /** "New Jersey, United States" style subtitle. */
    val subtitle: String
        get() = listOfNotNull(admin, country).distinct().joinToString(", ")
}
