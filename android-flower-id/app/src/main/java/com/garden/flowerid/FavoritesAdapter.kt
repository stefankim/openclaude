package com.garden.flowerid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(
    private val items: MutableList<Favorite>,
    private val onRemove: (Favorite) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.favoriteImage)
        val commonName: TextView = view.findViewById(R.id.favoriteCommonName)
        val scientificName: TextView = view.findViewById(R.id.favoriteScientificName)
        val weedBadge: TextView = view.findViewById(R.id.favoriteWeedBadge)
        val removeButton: ImageButton = view.findViewById(R.id.favoriteRemoveButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.commonName.text = item.commonName
        holder.scientificName.text = item.scientificName

        val bitmap = FavoritesStore.loadImage(holder.itemView.context, item.imageFileName)
        if (bitmap != null) holder.image.setImageBitmap(bitmap)

        if (item.isWeed) {
            holder.weedBadge.text = "☠ Weed"
            holder.weedBadge.setBackgroundColor(0xFFC62828.toInt())
        } else {
            holder.weedBadge.text = "✓ Keep"
            holder.weedBadge.setBackgroundColor(0xFF2E7D32.toInt())
        }

        holder.removeButton.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                val removed = items.removeAt(pos)
                notifyItemRemoved(pos)
                onRemove(removed)
            }
        }
    }
}
