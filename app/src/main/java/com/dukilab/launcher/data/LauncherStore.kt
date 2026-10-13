package com.dukilab.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dukilab.launcher.core.DoubleTapAction
import com.dukilab.launcher.core.Dock
import com.dukilab.launcher.core.LauncherSettings
import com.dukilab.launcher.core.LayoutCodec
import com.dukilab.launcher.core.SavedState
import com.dukilab.launcher.core.SwipeDownAction
import com.dukilab.launcher.core.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher")

/** 設定と配置を端末に保存する。 */
class LauncherStore(context: Context) {
    private val store = context.applicationContext.launcherDataStore

    val settings: Flow<LauncherSettings> = store.data.map(::readSettings).distinctUntilChanged()
    val saved: Flow<SavedState> = store.data.map(::readSaved).distinctUntilChanged()

    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        store.edit { p -> writeSettings(p, transform(readSettings(p))) }
    }

    suspend fun updateSaved(transform: (SavedState) -> SavedState) {
        store.edit { p ->
            val before = readSaved(p)
            val after = transform(before)
            if (after != before) writeSaved(p, after)
        }
    }

    /** 設定と配置をまとめて変える（マス目の大きさを変えるときなど）。 */
    suspend fun updateBoth(transform: (LauncherSettings, SavedState) -> Pair<LauncherSettings, SavedState>) {
        store.edit { p ->
            val (s, saved) = transform(readSettings(p), readSaved(p))
            writeSettings(p, s)
            writeSaved(p, saved)
        }
    }

    private fun readSettings(p: Preferences): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            themeMode = enumOr(p[THEME], d.themeMode),
            monochromeIcons = p[MONO] ?: d.monochromeIcons,
            showLabels = p[LABELS] ?: d.showLabels,
            gridColumns = p[COLUMNS]?.takeIf { it in LauncherSettings.COLUMN_CHOICES } ?: d.gridColumns,
            gridRows = p[ROWS]?.takeIf { it in LauncherSettings.ROW_CHOICES } ?: d.gridRows,
            swipeDown = enumOr(p[SWIPE_DOWN], d.swipeDown),
            doubleTap = enumOr(p[DOUBLE_TAP], d.doubleTap),
            showSeconds = p[SECONDS] ?: d.showSeconds,
            autoAddNewApps = p[AUTO_ADD] ?: d.autoAddNewApps,
        )
    }

    private fun writeSettings(p: MutablePreferences, s: LauncherSettings) {
        p[THEME] = s.themeMode.name
        p[MONO] = s.monochromeIcons
        p[LABELS] = s.showLabels
        p[COLUMNS] = s.gridColumns
        p[ROWS] = s.gridRows
        p[SWIPE_DOWN] = s.swipeDown.name
        p[DOUBLE_TAP] = s.doubleTap.name
        p[SECONDS] = s.showSeconds
        p[AUTO_ADD] = s.autoAddNewApps
    }

    private fun readSaved(p: Preferences): SavedState = SavedState(
        layout = LayoutCodec.decode(p[LAYOUT]),
        dock = Dock(LayoutCodec.decodeList(p[DOCK]).filter { it.isNotEmpty() }.take(Dock.MAX)),
        hidden = LayoutCodec.decodeList(p[HIDDEN]).filter { it.isNotEmpty() }.toSet(),
        known = LayoutCodec.decodeList(p[KNOWN]).filter { it.isNotEmpty() }.toSet(),
        initialized = p[INITIALIZED] ?: false,
        shortcuts = LayoutCodec.decodeShortcuts(p[SHORTCUTS]),
    )

    private fun writeSaved(p: MutablePreferences, s: SavedState) {
        p[LAYOUT] = LayoutCodec.encode(s.layout)
        p[DOCK] = LayoutCodec.encodeList(s.dock.appKeys)
        p[HIDDEN] = LayoutCodec.encodeList(s.hidden.sorted())
        p[KNOWN] = LayoutCodec.encodeList(s.known.sorted())
        p[INITIALIZED] = s.initialized
        p[SHORTCUTS] = LayoutCodec.encodeShortcuts(s.shortcuts.values)
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val MONO = booleanPreferencesKey("monochrome_icons")
        val LABELS = booleanPreferencesKey("show_labels")
        val COLUMNS = intPreferencesKey("grid_columns")
        val ROWS = intPreferencesKey("grid_rows")
        val SWIPE_DOWN = stringPreferencesKey("swipe_down")
        val DOUBLE_TAP = stringPreferencesKey("double_tap")
        val AUTO_ADD = booleanPreferencesKey("auto_add")
        val SECONDS = booleanPreferencesKey("show_seconds")
        val LAYOUT = stringPreferencesKey("layout")
        val DOCK = stringPreferencesKey("dock")
        val HIDDEN = stringPreferencesKey("hidden")
        val KNOWN = stringPreferencesKey("known")
        val INITIALIZED = booleanPreferencesKey("initialized")
        val SHORTCUTS = stringPreferencesKey("shortcuts")
    }
}
