package com.openclaude.weather.util

import com.openclaude.weather.domain.WeatherAlert
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses the MeteoAlarm legacy Atom feed (official national warnings, e.g. SHMU for
 * Slovakia) into [WeatherAlert]s. Only currently valid, at-least-Moderate warnings
 * are returned, de-duplicated by event+severity.
 */
object MeteoAlarmParser {

    /** Builds the country feed URL from a country name, e.g. "Slovakia" -> ...-slovakia. */
    fun feedUrlFor(country: String?): String? {
        if (country.isNullOrBlank()) return null
        val slug = country.trim().lowercase(Locale.ROOT).replace(' ', '-')
        return "https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-$slug"
    }

    fun parse(xml: String): List<WeatherAlert> {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
            .newPullParser()
        parser.setInput(xml.reader())

        val alerts = mutableListOf<WeatherAlert>()
        var inEntry = false
        var event = ""; var severity = ""; var area = ""; var expires: Long? = null
        var tag: String? = null

        var type = parser.eventType
        while (type != XmlPullParser.END_DOCUMENT) {
            when (type) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "entry") {
                        inEntry = true; event = ""; severity = ""; area = ""; expires = null
                    }
                    tag = parser.name
                }
                XmlPullParser.TEXT -> if (inEntry) {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) when (tag) {
                        "event" -> event = text
                        "severity" -> severity = text
                        "areaDesc" -> area = text
                        "expires" -> expires = parseTime(text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "entry") {
                        inEntry = false
                        val active = expires == null || expires > System.currentTimeMillis()
                        val severe = severity in listOf("Moderate", "Severe", "Extreme")
                        if (event.isNotBlank() && active && severe) {
                            alerts.add(WeatherAlert(event, severity, area, expires))
                        }
                    }
                    tag = null
                }
            }
            type = parser.next()
        }
        return alerts.distinctBy { it.event + it.severity }
    }

    private fun parseTime(iso: String): Long? {
        for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ssZ")) {
            val t = runCatching { SimpleDateFormat(pattern, Locale.US).parse(iso)?.time }.getOrNull()
            if (t != null) return t
        }
        return null
    }
}
