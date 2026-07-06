package com.openclaude.weather.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Builds the Retrofit services. Kept dependency-free (manual DI) to stay lightweight. */
object Network {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val weatherApi: OpenMeteoApi by lazy {
        retrofit("https://api.open-meteo.com/").create(OpenMeteoApi::class.java)
    }

    val geocodingApi: GeocodingApi by lazy {
        retrofit("https://geocoding-api.open-meteo.com/").create(GeocodingApi::class.java)
    }

    val rainViewerApi: RainViewerApi by lazy {
        retrofit("https://api.rainviewer.com/").create(RainViewerApi::class.java)
    }

    val airQualityApi: AirQualityApi by lazy {
        retrofit("https://air-quality-api.open-meteo.com/").create(AirQualityApi::class.java)
    }

    val feedApi: FeedApi by lazy {
        // Base URL is unused (absolute @Url), but Retrofit requires one.
        retrofit("https://feeds.meteoalarm.org/").create(FeedApi::class.java)
    }
}
