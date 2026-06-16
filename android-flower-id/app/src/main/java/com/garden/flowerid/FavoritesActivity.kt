package com.garden.flowerid

import android.os.Bundle
import android.view.View
import android.widget.TextView
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

        recyclerView.adapter = FavoritesAdapter(favorites) { removed ->
            FavoritesStore.remove(this, removed.scientificName)
            updateEmptyState(favorites.isEmpty())
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
}
