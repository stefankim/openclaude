package com.openclaude.weather.data.local


/** A place the user has pinned. Identity is the rounded lat/lon to avoid duplicates. */
data class SavedLocation(
    val id: String,
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val timezone: String?
) {
    val displayName: String
        get() = buildString {
            append(name)
            if (!region.isNullOrBlank() && region != name) append(", ").append(region)
        }

    companion object {
        fun idFor(lat: Double, lon: Double): String =
            "%.3f,%.3f".format(lat, lon)
    }
}
