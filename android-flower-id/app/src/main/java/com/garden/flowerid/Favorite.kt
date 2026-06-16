package com.garden.flowerid

data class Favorite(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val confidence: Double,
    val isWeed: Boolean,
    val weedReason: String?,
    val description: String?,
    val wikipediaUrl: String?,
    val imageFileName: String,
    val timestamp: Long
)
