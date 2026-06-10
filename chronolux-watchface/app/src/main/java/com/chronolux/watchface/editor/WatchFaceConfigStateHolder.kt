package com.chronolux.watchface.editor

import androidx.activity.ComponentActivity
import androidx.wear.watchface.editor.EditorSession
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import com.chronolux.watchface.style.ColorTheme
import com.chronolux.watchface.style.LayoutMode
import com.chronolux.watchface.style.StyleIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfigUiState(
    val colorThemeName: String = "",
    val layoutModeName: String = "",
    val ticksEnabled: Boolean = true,
    val complicationSlotIds: List<Int> = emptyList()
)

/**
 * Owns the [EditorSession] for the config activity and exposes a simple
 * UI state flow. Style mutations are written straight back to the session
 * so the system persists them when the activity finishes.
 */
class WatchFaceConfigStateHolder(
    private val scope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private var editorSession: EditorSession? = null

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            val session = EditorSession.createOnWatchEditorSession(activity)
            editorSession = session
            session.userStyle.collect { style -> publishState(session, style) }
        }
    }

    private fun publishState(session: EditorSession, style: UserStyle) {
        var themeName = ""
        var layoutName = ""
        var ticks = true

        style.forEach { (setting, option) ->
            when (setting.id.value) {
                StyleIds.COLOR_THEME -> {
                    val theme = ColorTheme.fromId(option.id.toString())
                    themeName = activity.getString(theme.displayNameRes)
                }
                StyleIds.LAYOUT_MODE -> {
                    val mode = LayoutMode.fromId(option.id.toString())
                    layoutName = mode.name.lowercase().replaceFirstChar { it.uppercase() }
                }
                StyleIds.SHOW_TICKS ->
                    ticks = (option as BooleanUserStyleSetting.BooleanOption).value
            }
        }

        _uiState.value = ConfigUiState(
            colorThemeName = themeName,
            layoutModeName = layoutName,
            ticksEnabled = ticks,
            complicationSlotIds = session.complicationSlotsState.value.keys.sorted()
        )
    }

    fun nextColorTheme() = cycleListSetting(StyleIds.COLOR_THEME)

    fun nextLayoutMode() = cycleListSetting(StyleIds.LAYOUT_MODE)

    /** Advances a list setting to its next option, wrapping at the end. */
    private fun cycleListSetting(settingId: String) {
        val session = editorSession ?: return
        val style = session.userStyle.value
        val entry = style.firstNotNullOfOrNull { (setting, option) ->
            if (setting.id.value == settingId) setting to option else null
        } ?: return
        val (setting, current) = entry
        val options = (setting as ListUserStyleSetting).options
        val nextIndex = (options.indexOfFirst { it.id == current.id } + 1) % options.size
        applyOption(setting, options[nextIndex])
    }

    fun setTicksEnabled(enabled: Boolean) {
        val session = editorSession ?: return
        val setting = session.userStyleSchema.userStyleSettings
            .firstOrNull { it.id.value == StyleIds.SHOW_TICKS } as? BooleanUserStyleSetting
            ?: return
        applyOption(setting, BooleanUserStyleSetting.BooleanOption.from(enabled))
    }

    private fun applyOption(
        setting: UserStyleSetting,
        option: UserStyleSetting.Option
    ) {
        val session = editorSession ?: return
        val mutable = session.userStyle.value.toMutableUserStyle()
        mutable[setting] = option
        session.userStyle.value = mutable.toUserStyle()
    }

    suspend fun openComplicationPicker(slotId: Int) {
        editorSession?.openComplicationDataSourceChooser(slotId)
    }
}
