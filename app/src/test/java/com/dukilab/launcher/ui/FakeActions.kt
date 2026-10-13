package com.dukilab.launcher.ui

import com.dukilab.launcher.core.Cell
import com.dukilab.launcher.core.LauncherSettings

/** 呼ばれた操作を記録するだけの偽物。 */
class FakeActions : LauncherActions {
    val calls = mutableListOf<String>()
    var settings = LauncherSettings()

    override fun launch(appKey: String) { calls += "launch:$appKey" }
    override fun openAppInfo(appKey: String) { calls += "info:$appKey" }
    override fun uninstall(appKey: String) { calls += "uninstall:$appKey" }
    override fun addToHome(appKey: String, page: Int) { calls += "addToHome:$appKey:$page" }
    override fun addWebShortcut(url: String, label: String, page: Int) { calls += "addWeb:$url:$label:$page" }
    override fun removeFromHome(appKey: String) { calls += "removeFromHome:$appKey" }
    override fun removeHomeCell(cell: Cell) { calls += "removeHomeCell:${cell.page}:${cell.index}" }
    override fun moveHomeItem(from: Cell, to: Cell) { calls += "move:${from.index}->${to.index}" }
    override fun moveToDock(from: Cell) { calls += "moveToDock:${from.index}" }
    override fun renameFolder(folderId: String, name: String) { calls += "rename:$folderId:$name" }
    override fun takeOutOfFolder(folderId: String, appKey: String) { calls += "takeOut:$folderId:$appKey" }
    override fun reorderInFolder(folderId: String, from: Int, to: Int) { calls += "reorder:$folderId:$from->$to" }
    override fun dissolveFolder(folderId: String) { calls += "dissolve:$folderId" }
    override fun addToDock(appKey: String) { calls += "addToDock:$appKey" }
    override fun removeFromDock(appKey: String) { calls += "removeFromDock:$appKey" }
    override fun moveInDock(from: Int, to: Int) { calls += "moveInDock:$from->$to" }
    override fun hide(appKey: String) { calls += "hide:$appKey" }
    override fun unhide(appKey: String) { calls += "unhide:$appKey" }
    override fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        settings = transform(settings)
        calls += "settings"
    }
    override fun setGrid(columns: Int, rows: Int) { calls += "grid:${columns}x$rows" }
    override fun expandNotifications() { calls += "notifications" }
    override fun lockScreen() { calls += "lock" }
    override fun openHomeSettings() { calls += "homeSettings" }
    override fun openAccessibilitySettings() { calls += "a11ySettings" }
}
