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

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

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

        FavoritesStore.loadImage(this, favorite.imageFileName)?.let { image.setImageBitmap(it) }

        commonName.text = favorite.commonName
        scientificName.text = favorite.scientificName
        confidence.text = getString(R.string.confidence_format, favorite.confidence)

        if (favorite.isWeed) {
            banner.setBackgroundColor(0xFFC62828.toInt())
            weedIcon.text = "☠"
            weedText.text = getString(R.string.weed_status_weed)
            weedReason.text = favorite.weedReason
            weedReason.visibility = View.VISIBLE
        } else {
            banner.setBackgroundColor(0xFF2E7D32.toInt())
            weedIcon.text = "✓"
            weedText.text = getString(R.string.weed_status_safe)
            weedReason.visibility = View.GONE
        }

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

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
