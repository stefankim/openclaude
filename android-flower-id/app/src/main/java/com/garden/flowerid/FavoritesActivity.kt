package com.garden.flowerid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import java.util.Locale
import java.util.concurrent.TimeUnit

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.favorites_title)
        }

        recyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyText = findViewById(R.id.emptyFavoritesText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val favorites = FavoritesStore.getAll(this).toMutableList()
        updateEmptyState(favorites.isEmpty())

        recyclerView.adapter = FavoritesAdapter(
            favorites,
            onRemove = { removed ->
                FavoritesStore.remove(this, removed.scientificName)
                updateEmptyState(favorites.isEmpty())
            },
            onClick = { favorite -> showDetail(favorite) }
        )
    }

    private fun appLanguage(): String =
        if (Locale.getDefault().language == "sk") "sk" else "en"

    private fun showDetail(favorite: Favorite) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_favorite_detail, null)

        val image = view.findViewById<ImageView>(R.id.detailImage)
        val banner = view.findViewById<LinearLayout>(R.id.detailWeedBanner)
        val weedIcon = view.findViewById<TextView>(R.id.detailWeedIcon)
        val weedText = view.findViewById<TextView>(R.id.detailWeedText)
        val commonName = view.findViewById<TextView>(R.id.detailCommonName)
        val scientificName = view.findViewById<TextView>(R.id.detailScientificName)
        val confidence = view.findViewById<TextView>(R.id.detailConfidence)
        val description = view.findViewById<TextView>(R.id.detailDescription)
        val learnMoreLink = view.findViewById<TextView>(R.id.detailLearnMoreLink)
        val weedReason = view.findViewById<TextView>(R.id.detailWeedReason)
        val removalCard = view.findViewById<LinearLayout>(R.id.detailRemovalCard)
        val removalText = view.findViewById<TextView>(R.id.detailRemovalText)
        val safetyWarning = view.findViewById<TextView>(R.id.detailSafetyWarning)

        FavoritesStore.loadImage(this, favorite.imageFileName)?.let { image.setImageBitmap(it) }

        // Re-derive weed info live so it follows the current app language,
        // even for favorites saved in another language. Fall back to the
        // stored (cached) values only if the plant is no longer in the database.
        val info = WeedDatabase.identify(favorite.scientificName)

        commonName.text = info?.commonName ?: favorite.commonName
        scientificName.text = favorite.scientificName
        confidence.text = getString(R.string.confidence_format, favorite.confidence)

        if (favorite.isWeed) {
            val severity = info?.severity?.name ?: favorite.weedSeverity
            when (severity) {
                "MILD" -> {
                    banner.setBackgroundColor(0xFFEF6C00.toInt())
                    weedIcon.text = "⚠"
                    weedText.text = getString(R.string.weed_status_weed_mild)
                }
                "NOTIFIABLE" -> {
                    banner.setBackgroundColor(0xFF4A148C.toInt())
                    weedIcon.text = "⛔"
                    weedText.text = getString(R.string.weed_status_weed_notifiable)
                }
                else -> {
                    banner.setBackgroundColor(0xFFC62828.toInt())
                    weedIcon.text = "☠"
                    weedText.text = getString(R.string.weed_status_weed_aggressive)
                }
            }
            val reason = info?.reason ?: favorite.weedReason
            weedReason.text = reason
            weedReason.visibility = View.VISIBLE
            val removal = info?.removal ?: favorite.weedRemoval
            if (!removal.isNullOrBlank()) {
                removalText.text = removal
                removalCard.visibility = View.VISIBLE
            }
            val hazard = info?.hazard ?: favorite.weedHazard
            if (!hazard.isNullOrBlank()) {
                safetyWarning.text = getString(R.string.safety_prefix, hazard)
                safetyWarning.visibility = View.VISIBLE
            }
        } else {
            banner.setBackgroundColor(0xFF2E7D32.toInt())
            weedIcon.text = "✓"
            weedText.text = getString(R.string.weed_status_safe)
            weedReason.visibility = View.GONE
            removalCard.visibility = View.GONE
        }

        // Show the stored description immediately as a fallback…
        if (!favorite.description.isNullOrBlank()) {
            description.text = favorite.description
            description.visibility = View.VISIBLE
        }
        if (!favorite.wikipediaUrl.isNullOrBlank()) {
            learnMoreLink.visibility = View.VISIBLE
            learnMoreLink.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(favorite.wikipediaUrl)))
            }
        }

        // …then refresh name and description from Wikipedia in the current app language.
        refreshFromWikipedia(favorite, info != null, commonName, description, learnMoreLink)

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun refreshFromWikipedia(
        favorite: Favorite,
        isKnownWeed: Boolean,
        nameView: TextView,
        description: TextView,
        learnMoreLink: TextView
    ) {
        val lang = appLanguage()
        WikipediaHelper.fetch(client, lang, favorite.scientificName, favorite.commonName) { result ->
            if (result == null) return@fetch
            runOnUiThread {
                description.text = result.extract
                description.visibility = View.VISIBLE
                if (result.pageUrl != null) {
                    learnMoreLink.visibility = View.VISIBLE
                    learnMoreLink.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.pageUrl)))
                    }
                }
                // For plants not in the weed database, use the localized Wikipedia
                // article title as the name (weeds already show their database name).
                if (!isKnownWeed && result.lang == lang && result.title.isNotBlank()) {
                    nameView.text = result.title
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        client.dispatcher.executorService.shutdown()
    }
}
