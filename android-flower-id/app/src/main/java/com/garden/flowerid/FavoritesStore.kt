package com.garden.flowerid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object FavoritesStore {
    private const val PREFS = "favorites_prefs"
    private const val KEY_LIST = "favorites_list"
    private val gson = Gson()

    // Self-contained export record: a favorite plus its photo inline as Base64,
    // so a single exported file can fully restore everything with no other state.
    private data class ExportItem(
        val commonName: String,
        val scientificName: String,
        val confidence: Double,
        val isWeed: Boolean,
        val weedReason: String?,
        val weedRemoval: String?,
        val weedSeverity: String?,
        val weedHazard: String?,
        val description: String?,
        val wikipediaUrl: String?,
        val timestamp: Long,
        val imageBase64: String?
    )

    private data class ExportData(
        val version: Int,
        val favorites: List<ExportItem>
    )

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

    /** Serializes every favorite (with its photo inline) to a single JSON string. */
    fun exportToJson(context: Context): String {
        val items = getAll(context).map { fav ->
            val bmp = loadImage(context, fav.imageFileName)
            ExportItem(
                fav.commonName, fav.scientificName, fav.confidence, fav.isWeed,
                fav.weedReason, fav.weedRemoval, fav.weedSeverity, fav.weedHazard,
                fav.description, fav.wikipediaUrl, fav.timestamp,
                bmp?.let { bitmapToBase64(it) }
            )
        }
        return gson.toJson(ExportData(1, items))
    }

    /**
     * Restores favorites from an exported JSON string, skipping any whose
     * scientific name is already saved. Returns the number actually added.
     */
    fun importFromJson(context: Context, json: String): Int {
        val data = gson.fromJson(json, ExportData::class.java)
            ?: return 0
        val list = getAll(context).toMutableList()
        val existing = list.map { it.scientificName }.toMutableSet()
        var added = 0
        data.favorites.forEach { item ->
            if (item.scientificName.isBlank() || existing.contains(item.scientificName)) return@forEach
            val id = "${item.timestamp}_${added}"
            val imageFileName = item.imageBase64?.let { b64 ->
                base64ToBitmap(b64)?.let { saveImage(context, it, id) }
            } ?: ""
            list.add(
                Favorite(
                    id, item.commonName, item.scientificName, item.confidence, item.isWeed,
                    item.weedReason, item.weedRemoval, item.weedSeverity, item.weedHazard,
                    item.description, item.wikipediaUrl, imageFileName, item.timestamp
                )
            )
            existing.add(item.scientificName)
            added++
        }
        if (added > 0) saveAll(context, list)
        return added
    }

    fun count(context: Context): Int = getAll(context).size

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun base64ToBitmap(data: String): Bitmap? = try {
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
