package com.garden.flowerid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

object FavoritesStore {
    private const val PREFS = "favorites_prefs"
    private const val KEY_LIST = "favorites_list"
    private val gson = Gson()

    fun getAll(context: Context): List<Favorite> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LIST, null) ?: return emptyList()
        val type = object : TypeToken<List<Favorite>>() {}.type
        val list: List<Favorite>? = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
        return (list ?: emptyList()).sortedByDescending { it.timestamp }
    }

    fun isFavorite(context: Context, scientificName: String): Boolean =
        getAll(context).any { it.scientificName == scientificName }

    fun add(
        context: Context,
        commonName: String,
        scientificName: String,
        confidence: Double,
        isWeed: Boolean,
        weedReason: String?,
        weedRemoval: String?,
        weedSeverity: String?,
        weedHazard: String?,
        description: String?,
        wikipediaUrl: String?,
        bitmap: Bitmap?
    ) {
        val id = System.currentTimeMillis().toString()
        val imageFileName = if (bitmap != null) saveImage(context, bitmap, id) else ""
        val favorite = Favorite(
            id, commonName, scientificName, confidence, isWeed,
            weedReason, weedRemoval, weedSeverity, weedHazard,
            description, wikipediaUrl, imageFileName,
            System.currentTimeMillis()
        )
        val list = getAll(context).toMutableList()
        list.add(favorite)
        saveAll(context, list)
    }

    fun remove(context: Context, scientificName: String) {
        val list = getAll(context).toMutableList()
        val toRemove = list.filter { it.scientificName == scientificName }
        toRemove.forEach { fav ->
            if (fav.imageFileName.isNotBlank()) {
                File(context.filesDir, fav.imageFileName).delete()
            }
        }
        list.removeAll(toRemove)
        saveAll(context, list)
    }

    fun loadImage(context: Context, fileName: String): Bitmap? {
        if (fileName.isBlank()) return null
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun saveImage(context: Context, bitmap: Bitmap, id: String): String {
        val fileName = "fav_$id.jpg"
        FileOutputStream(File(context.filesDir, fileName)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return fileName
    }

    private fun saveAll(context: Context, list: List<Favorite>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIST, gson.toJson(list))
            .apply()
    }
}
