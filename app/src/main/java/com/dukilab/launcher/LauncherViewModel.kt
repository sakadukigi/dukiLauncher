package com.dukilab.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dukilab.launcher.core.Cell
import com.dukilab.launcher.core.HomeItem
import com.dukilab.launcher.core.LauncherSettings
import com.dukilab.launcher.core.SavedState
import com.dukilab.launcher.core.Shortcut
import com.dukilab.launcher.data.AppRepository
import com.dukilab.launcher.data.IconLoader
import com.dukilab.launcher.data.LauncherStore
import com.dukilab.launcher.system.ScreenOffService
import com.dukilab.launcher.system.SystemBridge
import com.dukilab.launcher.ui.LauncherActions
import com.dukilab.launcher.ui.LauncherUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application), LauncherActions {
    private val repo = AppRepository(application)
    private val store = LauncherStore(application)
    private val system = SystemBridge(application, repo, ::shortcut)
    val icons = IconLoader(application, repo, ::shortcut)

    private val installed = MutableStateFlow<Map<String, com.dukilab.launcher.core.AppEntry>?>(null)
    private val defaultHome = MutableStateFlow(true)

    private val _homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** ホームボタンが押された（開いている一覧や設定を閉じる合図）。 */
    val homePressed: SharedFlow<Unit> = _homePressed.asSharedFlow()

    val state: StateFlow<LauncherUiState> = combine(
        installed,
        store.saved,
        store.settings,
        defaultHome,
        ScreenOffService.ready,
    ) { apps, saved, settings, isDefault, screenOff ->
        LauncherUiState(
            apps = apps.orEmpty(),
            saved = saved,
            settings = settings,
            loaded = apps != null,
            isDefaultHome = isDefault,
            screenOffReady = screenOff,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState())

    init {
        viewModelScope.launch {
            repo.apps.collect { list ->
                icons.clear()
                val map = list.associateBy { it.key }
                installed.value = map
                val autoAdd = store.settings.first().autoAddNewApps
                store.updateSaved { it.reconcile(map.keys, autoAdd) }
            }
        }
        refreshDefaultHome()
    }

    fun refreshDefaultHome() {
        defaultHome.value = system.isDefaultHome()
    }

    fun onHomePressed() {
        _homePressed.tryEmit(Unit)
    }

    private fun shortcut(key: String): Shortcut? = state.value.saved.shortcuts[key]

    private fun edit(transform: (SavedState) -> SavedState) {
        viewModelScope.launch { store.updateSaved { transform(it).dropUnusedShortcuts() } }
    }

    override fun launch(appKey: String) = system.launch(appKey)
    override fun openAppInfo(appKey: String) = system.openAppInfo(appKey)
    override fun uninstall(appKey: String) = system.uninstall(appKey)

    override fun addToHome(appKey: String, page: Int) =
        edit { it.copy(layout = it.layout.addApp(appKey, page, allowDuplicate = true)) }

    override fun addWebShortcut(url: String, label: String, page: Int) = edit {
        it.addShortcut(Shortcut(Shortcut.webKey(System.currentTimeMillis().toString(36)), label, url = url), page)
    }
    override fun removeFromHome(appKey: String) = edit { it.copy(layout = it.layout.removeApp(appKey)) }
    override fun removeHomeCell(cell: Cell) = edit { it.copy(layout = it.layout.removeAt(cell)) }
    override fun moveHomeItem(from: Cell, to: Cell) = edit { it.copy(layout = it.layout.move(from, to)) }

    override fun moveToDock(from: Cell) = edit { s ->
        val app = s.layout.itemAt(from) as? HomeItem.App ?: return@edit s
        if (s.dock.isFull && !s.dock.contains(app.appKey)) return@edit s
        s.copy(layout = s.layout.removeAt(from), dock = s.dock.add(app.appKey))
    }

    override fun renameFolder(folderId: String, name: String) =
        edit { it.copy(layout = it.layout.renameFolder(folderId, name)) }

    override fun takeOutOfFolder(folderId: String, appKey: String) =
        edit { it.copy(layout = it.layout.takeOutOfFolder(folderId, appKey)) }

    override fun reorderInFolder(folderId: String, from: Int, to: Int) =
        edit { it.copy(layout = it.layout.reorderInFolder(folderId, from, to)) }

    override fun dissolveFolder(folderId: String) = edit { it.copy(layout = it.layout.dissolveFolder(folderId)) }

    override fun addToDock(appKey: String) = edit { it.copy(dock = it.dock.add(appKey)) }
    override fun removeFromDock(appKey: String) = edit { it.copy(dock = it.dock.remove(appKey)) }
    override fun moveInDock(from: Int, to: Int) = edit { it.copy(dock = it.dock.move(from, to)) }

    override fun hide(appKey: String) = edit {
        it.copy(hidden = it.hidden + appKey, layout = it.layout.removeApp(appKey), dock = it.dock.remove(appKey))
    }

    override fun unhide(appKey: String) = edit { it.copy(hidden = it.hidden - appKey) }

    override fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { store.updateSettings(transform) }
    }

    override fun setGrid(columns: Int, rows: Int) {
        viewModelScope.launch {
            store.updateBoth { settings, saved ->
                settings.copy(gridColumns = columns, gridRows = rows) to
                    saved.copy(layout = saved.layout.resized(columns, rows))
            }
        }
    }

    override fun expandNotifications() = system.expandNotifications()
    override fun lockScreen() = system.lockScreen()
    override fun openHomeSettings() = system.openHomeSettings()
    override fun openAccessibilitySettings() = system.openAccessibilitySettings()
}
