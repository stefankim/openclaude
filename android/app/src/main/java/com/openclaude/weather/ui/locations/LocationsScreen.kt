package com.openclaude.weather.ui.locations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.weather.data.local.LocationStore
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.data.remote.GeoResult
import com.openclaude.weather.ui.SearchState
import com.openclaude.weather.ui.components.SectionTitle

@Composable
fun LocationsScreen(
    locations: List<SavedLocation>,
    selectedId: String?,
    search: SearchState,
    onQueryChange: (String) -> Unit,
    onAdd: (GeoResult) -> Unit,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = search.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search a city…", color = Color.White.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )

        Text(
            "${locations.size}/${LocationStore.MAX_LOCATIONS} saved",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        )

        // Search results take over the list when the user is typing.
        if (search.query.length >= 2) {
            Spacer(Modifier.height(8.dp))
            SectionTitle("Results")
            if (search.loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(16.dp))
            } else if (search.message != null) {
                Text(search.message, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(search.results) { r ->
                    ResultRow(r) { onAdd(r) }
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            SectionTitle("Your locations")
            if (locations.isEmpty()) {
                Text(
                    "No locations yet. Search above to add up to ${LocationStore.MAX_LOCATIONS}.",
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(8.dp)
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(locations, key = { it.id }) { loc ->
                    SavedRow(
                        location = loc,
                        selected = loc.id == selectedId,
                        onSelect = { onSelect(loc.id) },
                        onRemove = { onRemove(loc.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: GeoResult, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable { onAdd() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.LocationCity, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(result.admin1, result.country).joinToString(", "),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        Icon(Icons.Filled.Add, "Add", tint = Color(0xFF9FD0FF))
    }
}

@Composable
private fun SavedRow(
    location: SavedLocation,
    selected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.14f))
            .clickable { onSelect() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(Icons.Filled.CheckCircle, "Selected", tint = Color(0xFF9FD0FF), modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.Filled.LocationCity, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(location.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
            location.country?.let {
                Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, "Remove", tint = Color.White.copy(alpha = 0.85f))
        }
    }
}
