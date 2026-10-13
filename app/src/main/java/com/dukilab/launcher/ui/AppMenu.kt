package com.dukilab.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dukilab.launcher.core.Cell
import com.dukilab.launcher.core.Dock
import com.dukilab.launcher.core.Tags
import com.dukilab.launcher.ui.components.AppIcon
import com.dukilab.launcher.ui.components.DukiTag
import com.dukilab.launcher.ui.components.DukiText
import com.dukilab.launcher.ui.components.FolderIcon
import com.dukilab.launcher.ui.components.bottomLine
import com.dukilab.launcher.ui.components.cutCorner
import com.dukilab.launcher.ui.components.topLine
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme

/** 長押しメニューを出す対象。 */
sealed interface MenuTarget {
    /** アプリ一覧のアプリ。 */
    data class DrawerApp(val appKey: String) : MenuTarget

    /** ホーム画面のアプリ。 */
    data class HomeApp(val appKey: String, val cell: Cell) : MenuTarget

    /** ホーム画面のフォルダ。 */
    data class HomeFolder(val folderId: String, val cell: Cell) : MenuTarget

    /** ドックのアプリ。 */
    data class DockApp(val appKey: String, val index: Int) : MenuTarget

    /** フォルダの中のアプリ。 */
    data class FolderApp(val folderId: String, val appKey: String) : MenuTarget

    /** ホーム画面の何もないところ。 */
    data object Background : MenuTarget
}

/** メニューの1項目。 */
data class MenuAction(val label: String, val enabled: Boolean = true, val run: () -> Unit)

/** 対象ごとのメニュー項目を組み立てる。 */
fun menuActions(
    target: MenuTarget,
    state: LauncherUiState,
    actions: LauncherActions,
    currentPage: Int,
    openFolderForRename: (String) -> Unit,
    openSettings: () -> Unit,
    openDrawer: () -> Unit,
    openAddWeb: () -> Unit,
): List<MenuAction> {
    val saved = state.saved
    fun common(key: String): List<MenuAction> = buildList {
        // ショートカットにはアプリ情報もアンインストールもない。
        if (state.isShortcut(key)) return@buildList
        add(MenuAction("アプリ情報") { actions.openAppInfo(key) })
        if (state.app(key)?.isSystemApp != true) add(MenuAction("アンインストール") { actions.uninstall(key) })
    }
    return when (target) {
        is MenuTarget.DrawerApp -> buildList {
            val key = target.appKey
            add(MenuAction("ホーム画面に置く") { actions.addToHome(key, currentPage) })
            if (saved.layout.contains(key)) add(MenuAction("ホーム画面からすべて外す") { actions.removeFromHome(key) })
            when {
                saved.dock.contains(key) -> add(MenuAction("ドックから外す") { actions.removeFromDock(key) })
                saved.dock.isFull -> add(MenuAction("ドックがいっぱいです（最大${Dock.MAX}個）", enabled = false) {})
                else -> add(MenuAction("ドックに追加") { actions.addToDock(key) })
            }
            add(MenuAction("アプリ一覧で非表示にする") { actions.hide(key) })
            addAll(common(key))
        }

        is MenuTarget.HomeApp -> buildList {
            val key = target.appKey
            add(MenuAction("ホーム画面から外す") { actions.removeHomeCell(target.cell) })
            add(MenuAction("ホーム画面にもう1つ置く") { actions.addToHome(key, target.cell.page) })
            if (!saved.dock.contains(key) && !saved.dock.isFull) {
                add(MenuAction("ドックへ移す") { actions.moveToDock(target.cell) })
            }
            addAll(common(key))
        }

        is MenuTarget.HomeFolder -> listOf(
            MenuAction("名前を変える") { openFolderForRename(target.folderId) },
            MenuAction("フォルダを解く") { actions.dissolveFolder(target.folderId) },
            MenuAction("ホーム画面から外す") { actions.removeHomeCell(target.cell) },
        )

        is MenuTarget.DockApp -> buildList {
            val last = saved.dock.appKeys.lastIndex
            add(MenuAction("左へ移動", enabled = target.index > 0) { actions.moveInDock(target.index, target.index - 1) })
            add(MenuAction("右へ移動", enabled = target.index < last) { actions.moveInDock(target.index, target.index + 1) })
            add(MenuAction("ドックから外す") { actions.removeFromDock(target.appKey) })
            addAll(common(target.appKey))
        }

        is MenuTarget.FolderApp -> buildList {
            add(MenuAction("フォルダから出す") { actions.takeOutOfFolder(target.folderId, target.appKey) })
            addAll(common(target.appKey))
        }

        MenuTarget.Background -> listOf(
            MenuAction("アプリ一覧を開く") { openDrawer() },
            MenuAction("Webページを追加") { openAddWeb() },
            MenuAction("設定を開く") { openSettings() },
        )
    }
}

/**
 * 長押しメニュー。画面の下に、直角のパネル（左上に cut-md の三角）で出す。
 */
@Composable
fun AppMenuSheet(
    target: MenuTarget,
    state: LauncherUiState,
    items: List<MenuAction>,
    onDismiss: () -> Unit,
) {
    val c = DukiTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "閉じる",
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surfaceRaised)
                .topLine(c.line)
                .cutCorner(DukiDimens.cutMd, c.cornerMark)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = DukiDimens.space2),
        ) {
            MenuHeader(target, state)
            items.forEach { item ->
                MenuRow(item) {
                    onDismiss()
                    item.run()
                }
            }
        }
    }
}

@Composable
private fun MenuHeader(target: MenuTarget, state: LauncherUiState) {
    val c = DukiTheme.colors
    val appKey = when (target) {
        is MenuTarget.DrawerApp -> target.appKey
        is MenuTarget.HomeApp -> target.appKey
        is MenuTarget.DockApp -> target.appKey
        is MenuTarget.FolderApp -> target.appKey
        else -> null
    }
    val folder = (target as? MenuTarget.HomeFolder)?.let { state.saved.layout.folder(it.folderId) }
    val app = appKey?.let { state.app(it) }
    val title = app?.label ?: folder?.name ?: "ホーム画面"
    val tag = when {
        app != null && state.isShortcut(app.key) -> "SHORTCUT · ${app.packageName}"
        app != null -> app.packageName
        folder != null -> Tags.of("folder", folder.appKeys.size)
        else -> Tags.of("home")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bottomLine(c.line)
            .padding(start = DukiDimens.space5, end = DukiDimens.space5, top = DukiDimens.space5, bottom = DukiDimens.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DukiDimens.space4),
    ) {
        when {
            app != null -> AppIcon(app, 40.dp)
            folder != null -> FolderIcon(folder.appKeys.map { state.app(it) }, 40.dp)
        }
        Column(Modifier.weight(1f)) {
            DukiTag(tag)
            DukiText(title, DukiTheme.type.h3, maxLines = 1)
        }
    }
}

@Composable
private fun MenuRow(item: MenuAction, onClick: () -> Unit) {
    val c = DukiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(enabled = item.enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = DukiDimens.space5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(DukiDimens.space1))
        DukiText(
            text = item.label,
            style = DukiTheme.type.body,
            color = if (item.enabled) c.ink else c.inkMuted,
            maxLines = 1,
        )
    }
}
