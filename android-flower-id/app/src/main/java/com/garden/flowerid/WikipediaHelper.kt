package com.garden.flowerid

import android.net.Uri
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Resolves a plant summary from Wikipedia, preferring the app's language.
 *
 * For a non-English language it first tries the localized Wikipedia directly,
 * then falls back to a search on that Wikipedia (so a scientific name like
 * "Rubus saxatilis" resolves to the localized article "Ostružina skalná"),
 * and finally falls back to English. The returned [Summary.title] is the
 * localized article title, usable as a localized common name.
 */
object WikipediaHelper {

    data class Summary(
        val lang: String,
        val title: String,
        val extract: String,
        val pageUrl: String?
    )

    fun fetch(
        client: OkHttpClient,
        appLang: String,
        scientificName: String?,
        commonName: String?,
        onResult: (Summary?) -> Unit
    ) {
        val titles = listOfNotNull(scientificName, commonName)
            .filter { it.isNotBlank() }
            .distinct()
        val attempts = mutableListOf<((Summary?) -> Unit) -> Unit>()
        if (appLang != "en") {
            titles.forEach { t -> attempts.add { cb -> summary(client, appLang, t, cb) } }
            titles.forEach { t -> attempts.add { cb -> searchThenSummary(client, appLang, t, cb) } }
        }
        titles.forEach { t -> attempts.add { cb -> summary(client, "en", t, cb) } }
        runChain(attempts, 0, onResult)
    }

    private fun runChain(
        attempts: List<((Summary?) -> Unit) -> Unit>,
        index: Int,
        onResult: (Summary?) -> Unit
    ) {
        if (index >= attempts.size) {
            onResult(null)
            return
        }
        attempts[index] { result ->
            if (result != null) onResult(result)
            else runChain(attempts, index + 1, onResult)
        }
    }

    private fun summary(client: OkHttpClient, lang: String, title: String, cb: (Summary?) -> Unit) {
        val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/" +
            Uri.encode(title.replace(" ", "_"))
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GardenWeedID-Android/1.0")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = cb(null)

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    cb(null)
                    return
                }
                try {
                    val obj = JsonParser.parseString(response.body?.string()).asJsonObject
                    val type = obj.get("type")?.asString
                    val extract = obj.get("extract")?.asString
                    val pageTitle = obj.get("title")?.asString
                    val pageUrl = obj.getAsJsonObject("content_urls")
                        ?.getAsJsonObject("desktop")?.get("page")?.asString
                    // Skip disambiguation pages and empty summaries
                    if (extract.isNullOrBlank() || pageTitle.isNullOrBlank() ||
                        type == "disambiguation"
                    ) {
                        cb(null)
                    } else {
                        cb(Summary(lang, pageTitle, extract, pageUrl))
                    }
                } catch (e: Exception) {
                    cb(null)
                }
            }
        })
    }

    private fun searchThenSummary(
        client: OkHttpClient,
        lang: String,
        query: String,
        cb: (Summary?) -> Unit
    ) {
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
            Uri.encode(query) + "&srlimit=1&format=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GardenWeedID-Android/1.0")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = cb(null)

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    cb(null)
                    return
                }
                try {
                    val results = JsonParser.parseString(response.body?.string()).asJsonObject
                        .getAsJsonObject("query")?.getAsJsonArray("search")
                    val firstTitle = if (results != null && results.size() > 0)
                        results[0].asJsonObject.get("title")?.asString else null
                    if (firstTitle.isNullOrBlank()) cb(null)
                    else summary(client, lang, firstTitle, cb)
                } catch (e: Exception) {
                    cb(null)
                }
            }
        })
    }
}
