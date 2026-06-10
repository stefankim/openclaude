package com.chronolux.watchface.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.chronolux.watchface.R
import kotlinx.coroutines.launch

/**
 * On-watch editor launched from the watch face picker ("Customize") or the
 * companion app. Backed by [WatchFaceConfigStateHolder], which owns the
 * androidx EditorSession.
 */
class WatchFaceConfigActivity : ComponentActivity() {

    private lateinit var stateHolder: WatchFaceConfigStateHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateHolder = WatchFaceConfigStateHolder(lifecycleScope, this)

        setContent {
            MaterialTheme {
                val uiState by stateHolder.uiState.collectAsState()

                ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = getString(R.string.config_title),
                            style = MaterialTheme.typography.title3,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    item {
                        Chip(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            label = { Text(getString(R.string.setting_color_theme)) },
                            secondaryLabel = { Text(uiState.colorThemeName) },
                            colors = ChipDefaults.primaryChipColors(),
                            onClick = { stateHolder.nextColorTheme() }
                        )
                    }

                    item {
                        Chip(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            label = { Text(getString(R.string.setting_layout_mode)) },
                            secondaryLabel = { Text(uiState.layoutModeName) },
                            colors = ChipDefaults.primaryChipColors(),
                            onClick = { stateHolder.nextLayoutMode() }
                        )
                    }

                    item {
                        ToggleChip(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            checked = uiState.ticksEnabled,
                            onCheckedChange = { stateHolder.setTicksEnabled(it) },
                            label = { Text(getString(R.string.setting_show_ticks)) },
                            toggleControl = {
                                androidx.wear.compose.material.Switch(checked = uiState.ticksEnabled)
                            },
                            colors = ToggleChipDefaults.toggleChipColors()
                        )
                    }

                    item {
                        Text(
                            text = getString(R.string.config_complications_header),
                            style = MaterialTheme.typography.caption1,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    items(uiState.complicationSlotIds) { slotId ->
                        Chip(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            label = { Text(getString(R.string.config_edit_complication, slotId)) },
                            colors = ChipDefaults.secondaryChipColors(),
                            onClick = {
                                lifecycleScope.launch { stateHolder.openComplicationPicker(slotId) }
                            }
                        )
                    }
                }
            }
        }
    }
}
